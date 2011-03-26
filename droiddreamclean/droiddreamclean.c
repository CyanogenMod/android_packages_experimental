#include <stdio.h>
#include <stdlib.h>
#include <sys/mount.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <ctype.h>

/* from mwomack's product_data.csv */
const char *packages[] = { "Super.mobi.eraser",
  "advanced.piano",
  "com.Funny.Face",
  "com.advanced.SoundManager",
  "com.advanced.scientific.calculator",
  "com.app.aun",
  "com.apps.tosd",
  "com.beauty.leg",
  "com.bubble",
  "com.dice.power",
  "com.dice.power.advanced",
  "com.dodge.game.fallingball",
  "com.droiddream.advancedtaskkiller1",
  "com.droiddream.android.afdvancedfm",
  "com.droiddream.barcodescanner",
  "com.droiddream.basketball",
  "com.droiddream.blueftp",
  "com.droiddream.bowlingtime",
  "com.droiddream.comparator",
  "com.droiddream.compasslevel",
  "com.droiddream.daltonismo",
  "com.droiddream.fallingball",
  "com.droiddream.game.omok",
  "com.droiddream.glowhockey",
  "com.droiddream.howtotie",
  "com.droiddream.lovePositions",
  "com.droiddream.musicbox",
  "com.droiddream.passwordsafe",
  "com.droiddream.pewpew",
  "com.droiddream.sexringtones",
  "com.droiddream.stopwatch",
  "com.droiddream.system.app.remover",
  "com.editor.photoenhance",
  "com.fall.down",
  "com.fall.soft.down",
  "com.free.chess",
  "com.free.game.finger",
  "com.hg.panzerpanic1",
  "com.hz.game.mrrunner1",
  "com.magic.spiral",
  "com.power.SuperSolo",
  "com.power.basketball",
  "com.power.demo.note",
  "com.power.magic.StrobeLight",
  "com.quick.Delete",
  "com.sex.japaneese.girls",
  "com.sexsound.hilton",
  "com.sexy.hotgirls",
  "com.sexy.legs",
  "com.spider.man",
  "com.super.mp3ringtone",
  "hot.goddchen.sexyvideos",
  "org.droiddream.yellow4",
  "power.nick.ypaint",
  "power.power.rate",
  "powerstudio.spiderman",
  "proscio.app.nick.ypaint",
  "super.sancron.ringtones.sexysb",
  "org.super.yellow4",
  "com.droid.publick.hotgirls",
  "com.super.free.sexringtones",
  "hot.goddchen.power.sexyvideos",
  "\0"};

#define MAX_PACKAGES 512
#define MAX_PACKAGE_NAME_LENGTH 512

char installed_packages[MAX_PACKAGES][MAX_PACKAGE_NAME_LENGTH];
int num_packages;

void llog(char * msg, int result) {
  printf("%s:%s", msg, (result==0)?"Success":"Failure"); // Success is 0.
  if (result != 0 && errno != 0) {
    printf(" errorno=%s", strerror(errno));
  }
  printf("\n");
}

char *strstrip(char *s) {
  size_t size;
  char *end;

  size = strlen(s);

  if (!size)
    return s;

  end = s + size - 1;
  while (end >= s && isspace(*end))
    end--;
  *(end + 1) = '\0';

  while (*s && isspace(*s))
    s++;

  return s;
}

void populate_installed_packages() {
  FILE *fp;
  char pkg[MAX_PACKAGE_NAME_LENGTH];
  int len;
  num_packages = 0;

  fp = popen("/system/bin/pm list packages", "r");

  if (fp == NULL) {
    printf("failed to run /system/bin/pm list packages. not removing apps.\n");
    return;
  }

  while ((fgets(pkg, sizeof(pkg)-1,fp) != NULL) 
         && num_packages < MAX_PACKAGES) {

    //printf("package before = %s\n", pkg);
    len = (strlen(pkg)-8 < MAX_PACKAGE_NAME_LENGTH)?(strlen(pkg)-8):MAX_PACKAGE_NAME_LENGTH;
    strncpy(installed_packages[num_packages], (pkg+8), len);
    // pkg+8 removes the initial "package:""
    strstrip(installed_packages[num_packages]);
    //printf("package after = %s\n", installed_packages[num_packages]);
    num_packages++;
  }

  pclose(fp);
}

int package_installed(const char *package_name) {

  int i;

  for (i=0; i<num_packages; i++) {
    if (strcmp(package_name, installed_packages[i]) == 0) {
      return i;
    }
  }

  return -1;
}

void remove_package(const char *package_name, int idx) {
  char command[1024];
  int retval;

  printf("%d:",idx);
  fflush(stdout);

  snprintf(command, 1024, "pm uninstall %s", package_name);
  retval = system(command);
}


void getSystemMountPoint(char * dev) {
  FILE *f = fopen("/proc/mounts", "r");
  if (f == NULL) {
    printf("unable to read /proc/mounts: \n");
    exit(1);
  }

  char mountPoint[1024];
  char type[1024];
  char opts[1024];
  int freq;
  int passno;

  while(1) {
    int retval = fscanf(f, "%s %s %s %s %d %d", dev,
                        mountPoint, type, opts, &freq, &passno);
    if (retval != 6) {
      llog("getsysmntpnt wrong num args", retval);
      exit(1);
    }
    if (strcmp(mountPoint, "/system")) {
      return;
    }
  }
}

int file_exists(const char *filename) {
  FILE *f;

  if (f = fopen(filename, "r")) {
    fclose(f);
    return 1;
  }
  return 0;
}

int main() {
  int retval;
  char dev[1024];
  int i=0;

  printf("elh\n");

  if (getuid() != 0) {
    printf("not running as root\n");
    exit(1);
  }

  populate_installed_packages();

  while (packages[i][0] != '\0') {
    if (package_installed(packages[i]) != -1) {
      remove_package(packages[i], i);
    }
    i++;
  }

  getSystemMountPoint(dev);

  errno = 0;
  retval = mount(dev, "/system", "ignored", MS_REMOUNT, NULL);
  llog("mnt rw", retval);

  if (retval != 0) {
    // no use continuing if we can't remount read-write
    exit(1);
  }

  if (file_exists("/system/app/DownloadProvidersManager.apk")) {
    errno = 0;
    retval = unlink("/system/app/DownloadProvidersManager.apk");
    llog("rm DownloadProvidersManager", retval);

    errno = 0;
    printf("pm uninst downloadsmanager:");
    fflush(stdout);
    system("/system/bin/pm uninstall com.android.providers.downloadsmanager");
  }

  if (file_exists("/system/app/com.android.providers.ammanage.apk")) {
    errno = 0;
    retval = unlink("/system/app/com.android.providers.ammanage.apk");
    llog("rm ammanager", retval);

    errno = 0;
    printf("pm uninst ammanager:");
    fflush(stdout);
    system("/system/bin/pm uninstall com.android.providers.ammanage");
  }

  if (file_exists("/system/bin/profile")) {
    errno = 0;
    retval = unlink("/system/bin/profile");
    llog("rm profile", retval);
  }

  if (file_exists("/system/bin/share")) {
    errno = 0;
    retval = unlink("/system/bin/share");
    llog("rm share", retval);
  }

  /*
   * technically it's ok if the next line fails, as the
   * filesystem will be mounted read-only on the next boot
   */
  errno = 0;
  retval = mount(dev, "/system", "ignored", MS_REMOUNT | MS_RDONLY, NULL);
  llog("mnt ro", retval);

  return 0;
}
