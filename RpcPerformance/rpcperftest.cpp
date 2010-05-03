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

#include <binder/IServiceManager.h>

#include <stdio.h>
#include <time.h>

using namespace android;

static const int COUNT = 10000;

int main(int argc, char *argv[]) {
    if (argc != 2) {
        fprintf(stderr, "usage: rpcperftest servicename\n");
        return 2;
    }

    sp<IServiceManager> sm = defaultServiceManager();
    if (sm == NULL) {
        fprintf(stderr, "error: can't get default service manager\n");
        return 1;
    }

    sp<IBinder> service = sm->checkService(String16(argv[1]));
    if (service == NULL) {
        fprintf(stderr, "error: can't find service: %s\n", argv[1]);
        return 1;
    }

    struct timespec before, after;
    clock_gettime(CLOCK_MONOTONIC, &before);
    for (int i = 0; i < COUNT; i++) {
        status_t status = service->pingBinder();
        if (status != OK) {
            fprintf(stderr, "error: can't ping service manager [%d]\n", status);
            return 1;
        }
    }
    clock_gettime(CLOCK_MONOTONIC, &after);

    double seconds = (after.tv_sec - before.tv_sec);
    seconds += (after.tv_nsec - before.tv_nsec) / 1000000000.0;
    printf("%d calls in %.3f sec => %.3f ms/call\n",
        COUNT, seconds, 1000.0 * seconds / COUNT);

    return 0;
}
