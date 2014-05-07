/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.example.notificationlistener;

import android.service.notification.StatusBarNotification;

import java.util.Comparator;
import java.util.HashMap;

/**
 * A {@link java.util.Comparator} that will sort notifications into the proper display order.
 */
public class OrderedNotificationsHelper implements Comparator<StatusBarNotification> {
    private HashMap<String, Integer> rankCache;
    private String[] mOrderedKeys;

    public OrderedNotificationsHelper(String[] notificationOrder) {
        this.rankCache = new HashMap<String, Integer>();
        mOrderedKeys = notificationOrder;
    }

    public String[] getOrderedKeys() {
        return mOrderedKeys;
    }

    public int getIndex(String key) {
        if (rankCache.isEmpty()) {
            fillCache();
        }
        Integer rank = rankCache.get(key);
        return rank == null ? -1 : rank;
    }

    /**
     * Update the order impleneted by {@link #compare(Object, Object)}.
     * @param orderedKeys notification keys in order from
     * {@link android.service.notification.NotificationListenerService#getOrderedNotificationKeys()}
     */
    public void updateKeyOrder(String[] orderedKeys) {
        // TODO implement incremental updates
        // Defer as much work as possible to fillCache()
        mOrderedKeys = orderedKeys;
        rankCache.clear();
    }

    private void fillCache() {
        final int N = mOrderedKeys.length;
        for (int i = 0; i < N; i++) {
            rankCache.put(mOrderedKeys[i], i);
        }
    }

    /**
     * More important notifications will sort to the front
     *
     * @param lhs an {@code Object}.
     * @param rhs a second {@code Object} to compare with {@code lhs}.
     * @return 0 if lhs and rhs are of equal importance, less than 0 if lhs more
     *      important that rhs, and greater than 0 if lhs less important than rhs.
     */
    public int compare(StatusBarNotification lhs, StatusBarNotification rhs) {
        if (rankCache.isEmpty()) {
            fillCache();
        }
        if (lhs == null || rhs == null) {
            throw new IllegalArgumentException("passed null to the Comparator");
        }
        final Integer lIndex = rankCache.get(lhs.getKey());
        final Integer rIndex = rankCache.get(rhs.getKey());
        if (rIndex == null && rIndex == null) {
            return 0;
        }
        if (lIndex == null) {
            return 1; // down sort unknown notification
        }
        if (rIndex == -1) {
            return -1; // down sort unknown notification
        }
        return Integer.compare(lIndex, rIndex);
    }
}