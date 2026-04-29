/*******************************************************************************
 * Copyright (c) 2026 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.link;

import java.util.Arrays;

import org.eclipse.leshan.core.util.StringUtils;

public final class LinkUtil {

    @SuppressWarnings({ "java:S3776", "java:S1168" })
    public static Link[] sort(Link[] linksToSort) {
        // sort the list of objects
        if (linksToSort == null) {
            return null;
        }

        Link[] res = Arrays.copyOf(linksToSort, linksToSort.length);

        Arrays.sort(res, (o1, o2) -> {
            if (o1 == null && o2 == null)
                return 0;
            if (o1 == null)
                return -1;
            if (o2 == null)
                return 1;
            // by URL
            String[] url1 = o1.getUriReference().split("/");
            String[] url2 = o2.getUriReference().split("/");

            for (int i = 0; i < url1.length && i < url2.length; i++) {
                // is it two numbers?
                if (isNumber(url1[i]) && isNumber(url2[i])) {
                    int cmp = Integer.parseInt(url1[i]) - Integer.parseInt(url2[i]);
                    if (cmp != 0) {
                        return cmp;
                    }
                } else {

                    int v = url1[i].compareTo(url2[i]);

                    if (v != 0) {
                        return v;
                    }
                }
            }

            return url1.length - url2.length;
        });

        return res;
    }

    private static boolean isNumber(String s) {
        return !StringUtils.isEmpty(s) && StringUtils.isNumeric(s);
    }
}
