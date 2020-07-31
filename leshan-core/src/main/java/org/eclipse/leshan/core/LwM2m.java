/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
package org.eclipse.leshan.core;

public interface LwM2m {

    /**
     * Version of LWM2M specification.
     */
    public class Version {
        public static Version V1_0 = new Version("1.0", true);
        public static Version V1_1 = new Version("1.1", true);
        private static Version[] supportedVersions = new Version[] { V1_0, V1_1 };

        private String value;
        private boolean supported;

        protected Version(String version, boolean supported) {
            this.value = version;
            this.supported = supported;
        }

        @Override
        public String toString() {
            return value;
        }

        public boolean isSupported() {
            return supported;
        }

        public static Version get(String version) {
            for (Version constantVersion : supportedVersions) {
                if (constantVersion.value.equals(version)) {
                    return constantVersion;
                }
            }
            return new Version(version, false);
        }

        public static boolean isSupported(String version) {
            return get(version).isSupported();
        }

        public static Version lastSupported() {
            return V1_1;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Version other = (Version) obj;
            if (value == null) {
                if (other.value != null)
                    return false;
            } else if (!value.equals(other.value))
                return false;
            return true;
        }
    }

    // TODO we should probably remove this (replaced by Version enum)
    /** The supported version of the specification */
    static final String VERSION = "1.0";

    /** The default CoAP port for unsecured CoAP communication */
    static final int DEFAULT_COAP_PORT = 5683;

    /** The default CoAP port for secure CoAP communication */
    static final int DEFAULT_COAP_SECURE_PORT = 5684;
}
