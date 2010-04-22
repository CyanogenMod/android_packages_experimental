/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <assert.h>
#include <ctype.h>
#include <dirent.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/types.h>
#include <time.h>
#include <unistd.h>

// This program is as dumb as possible -- it reads a whole bunch of data
// from /proc and reports when it changes.  It's up to analysis tools to
// actually parse the data.  This program only does enough parsing to split
// large files (/proc/stat, /proc/yaffs) into individual values.
//
// The output format is a repeating series of observed differences:
//
//   T + <beforetime.stamp>
//   /proc/<new_filename> + <contents of newly discovered file>
//   /proc/<changed_filename> = <contents of changed file>
//   /proc/<deleted_filename> -
//   /proc/<filename>:<label> = <part of a multiline file>
//   T - <aftertime.stamp>
//
//
// Files read:
//
// /proc/*/stat  - for all running processes
// /proc/*/wchan - for all running processes
// /proc/stat    - one per line, e.g. "/proc/stat:intr"
// /proc/yaffs   - one per line, e.g. "/proc/yaffs:userdata:nBlockErasures"

struct data {
    char *name;            // filename, plus ":var" for many-valued files
    char *value;           // text to be reported when it changes
};

// Set data->name and data->value to malloc'd strings with the
// filename and contents of the file.  Trims trailing whitespace.
static void read_data(struct data *data, const char *filename) {
    char buf[4096];
    data->name = strdup(filename);
    int fd = open(filename, O_RDONLY);
    if (fd < 0) {
        data->value = NULL;
        return;
    }

    int len = read(fd, buf, sizeof(buf));
    if (len < 0) {
        perror(filename);
        close(fd);
        data->value = NULL;
        return;
    }

    close(fd);
    while (len > 0 && isspace(buf[len - 1])) --len;
    data->value = malloc(len + 1);
    memcpy(data->value, buf, len);
    data->value[len] = '\0';
}

// Read /proc/stat and write data entries for each line.
// Returns the number of entries written (always <= stats_count).
static int read_proc_stat(struct data *stats, int stats_count) {
    char buf[4096];
    int fd = open("/proc/stat", O_RDONLY);
    if (fd < 0) return 0;

    int len = read(fd, buf, sizeof(buf) - 1);
    if (len < 0) {
        perror("/proc/stat");
        close(fd);
        return 0;
    }
    buf[len] = '\0';
    close(fd);

    int num = 0;
    char *line;
    for (line = strtok(buf, "\n");
         line != NULL && num < stats_count;
         line = strtok(NULL, "\n")) {
        char *name_end = strchr(line, ' ');
        if (name_end == NULL) continue;

        struct data *data = &stats[num++];
        data->name = malloc(11 + (name_end - line) + 1);
        strcpy(data->name, "/proc/stat:");
        memcpy(data->name + 11, line, name_end - line);
        data->name[11 + (name_end - line)] = '\0';

        char *value = name_end;
        while (isspace(*value)) ++value;
        data->value = strdup(value);
    }

    return num;
}

// Read /proc/yaffs and write data entries for each line.
// Returns the number of entries written (always <= stats_count).
static int read_proc_yaffs(struct data *stats, int stats_count) {
    char buf[8192];
    int fd = open("/proc/yaffs", O_RDONLY);
    if (fd < 0) return 0;

    int len = read(fd, buf, sizeof(buf) - 1);
    if (len < 0) {
        perror("/proc/yaffs");
        close(fd);
        return 0;
    }
    buf[len] = '\0';
    close(fd);

    int num = 0, device_len = 0;
    char *line, *device = NULL;
    for (line = strtok(buf, "\n");
         line != NULL && num < stats_count;
         line = strtok(NULL, "\n")) {
        if (strncmp(line, "Device ", 7) == 0) {
            device = strchr(line, '"');
            if (device != NULL) {
                char *end = strchr(++device, '"');
                if (end != NULL) *end = '\0';
                device_len = strlen(device);
            }
            continue;
        }
        if (device == NULL) continue;

        char *name_end = line + strcspn(line, " .");
        if (name_end == line || *name_end == '\0') continue;

        struct data *data = &stats[num++];
        data->name = malloc(12 + device_len + 1 + (name_end - line) + 1);
        strcpy(data->name, "/proc/yaffs:");
        memcpy(data->name + 12, device, device_len);
        strcpy(data->name + 12 + device_len, ":");
        memcpy(data->name + 12 + device_len + 1, line, name_end - line);
        data->name[12 + device_len + 1 + (name_end - line)] = '\0';

        char *value = name_end;
        while (*value == '.' || isspace(*value)) ++value;
        data->value = strdup(value);
    }

    return num;
}

// Compare two "struct data" records by their name.
static int compare_data(const void *a, const void *b) {
    const struct data *data_a = (const struct data *) a;
    const struct data *data_b = (const struct data *) b;
    return strcmp(data_a->name, data_b->name);
}

// Return a malloc'd array of "struct data" read from all over /proc.
// The array is sorted by name and terminated by a record with name == NULL.
static struct data *read_stats() {
    int pids[4096];
    size_t pid_count = 0;

    DIR *proc_dir = opendir("/proc");
    if (proc_dir == NULL) {
        perror("Can't scan /proc");
        exit(1);
    }

    char filename[1024];
    struct dirent *proc_entry;
    while ((proc_entry = readdir(proc_dir))) {
        int pid = atoi(proc_entry->d_name);
        if (pid > 0) {
            if (pid_count >= sizeof(pids) / sizeof(pids[0])) {
                fprintf(stderr, "warning: >%d processes\n", pid_count);
                break;
            }
            pids[pid_count++] = pid;
        }
    }
    closedir(proc_dir);

    size_t i, stats_count = pid_count * 2 + 200;  // Enough for stat + yaffs
    struct data *stats = malloc((stats_count + 1) * sizeof(struct data));
    struct data *next = stats;
    for (i = 0; i < pid_count; i++) {
        sprintf(filename, "/proc/%d/stat", pids[i]);
        read_data(next++, filename);

        sprintf(filename, "/proc/%d/wchan", pids[i]);
        read_data(next++, filename);
    }

    next += read_proc_stat(next, stats + stats_count - next);
    next += read_proc_yaffs(next, stats + stats_count - next);

    assert(next < stats + stats_count);
    next->name = NULL;
    next->value = NULL;
    qsort(stats, next - stats, sizeof(struct data), compare_data);
    return stats;
}

// Print stats which have changed from one sorted array to the next.
static void diff_stats(struct data *old_stats, struct data *new_stats) {
    while (old_stats->name != NULL || new_stats->name != NULL) {
        int compare;
        if (old_stats->name == NULL) {
            compare = 1;
        } else if (new_stats->name == NULL) {
            compare = -1;
        } else {
            compare = compare_data(old_stats, new_stats);
        }

        if (compare < 0) {
            // old_stats no longer present
            if (old_stats->value != NULL) {
                printf("%s -\n", old_stats->name);
            }
            ++old_stats;
        } else if (compare > 0) {
            // new_stats is new
            if (new_stats->value != NULL) {
                printf("%s + %s\n", new_stats->name, new_stats->value);
            }
            ++new_stats;
        } else {
            // changed
            if (new_stats->value == NULL) {
                if (old_stats->value != NULL) {
                    printf("%s -\n", old_stats->name);
                }
            } else if (old_stats->value == NULL) {
                printf("%s + %s\n", new_stats->name, new_stats->value);
            } else if (strcmp(old_stats->value, new_stats->value)) {
                printf("%s = %s\n", new_stats->name, new_stats->value);
            }
            ++old_stats;
            ++new_stats;
        }
    }
}

// Free a "struct data" array and all the strings within it.
static void free_stats(struct data *stats) {
    int i;
    for (i = 0; stats[i].name != NULL; ++i) {
        free(stats[i].name);
        free(stats[i].value);
    }
    free(stats);
}

int main(int argc, char *argv[]) {
    if (argc != 2) {
        fprintf(stderr,
                "usage: procstatlog poll_interval > procstat.log\n\n"
                "Scans process status every poll_interval seconds (e.g. 0.1)\n"
                "and writes data from /proc/stat, /proc/*/stat files, and\n"
                "other /proc status files every time something changes.\n"
                "Data is logged continuously until the program is killed.\n");
        return 2;
    }

    long poll_usec = (long) (atof(argv[1]) * 1000000l);
    if (poll_usec <= 0) {
        fprintf(stderr, "illegal poll interval: %s\n", argv[1]);
        return 2;
    }

    struct data *old_stats = malloc(sizeof(struct data));
    old_stats->name = NULL;
    old_stats->value = NULL;
    while (1) {
        struct timeval before, after;
        gettimeofday(&before, NULL);
        printf("T + %ld.%06ld\n", before.tv_sec, before.tv_usec);

        struct data *new_stats = read_stats();
        diff_stats(old_stats, new_stats);
        free_stats(old_stats);
        old_stats = new_stats;
        gettimeofday(&after, NULL);
        printf("T - %ld.%06ld\n", after.tv_sec, after.tv_usec);

        long elapsed_usec = (long) after.tv_usec - before.tv_usec;
        elapsed_usec += 1000000l * (after.tv_sec - before.tv_sec);
        if (poll_usec > elapsed_usec) usleep(poll_usec - elapsed_usec);
    }

    return 0;
}
