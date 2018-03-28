/*******************************************************************************
 * Copyright (c) 2018 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class X509Util {

    /**
     * Extract "common name" from "distinguished name".
     * 
     * @param dn distinguished name
     * @return common name
     * @throws IllegalStateException if no CN is contained in DN.
     */
    public static String extractCN(String dn) {
        // Extract common name
        Matcher endpointMatcher = Pattern.compile("CN=(.*?)(,|$)").matcher(dn);
        if (endpointMatcher.find()) {
            return endpointMatcher.group(1);
        } else {
            throw new IllegalStateException(
                    "Unable to extract sender identity : can not get common name in certificate");
        }
    }

    /**
     * Create a simplified "distinguished name" from a given "common name".
     *
     * @return a DN with format "CN=xxx"
     */
    public static String createDN(String commonName) {
        return "CN=" + commonName;
    }

}
