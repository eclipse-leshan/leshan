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
    public class Version implements Comparable<Version> {

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

        public static String validate(String version) {
            if (version == null || version.isEmpty())
                return "version  MUST NOT be null or empty";
            String[] versionPart = version.split("\\.");
            if (versionPart.length != 2) {
                return String.format("version (%s) MUST be composed of 2 parts", version);
            }
            for (int i = 0; i < 2; i++) {
                try {
                    int parsedInt = Integer.parseInt(versionPart[i]);
                    if (parsedInt < 0) {
                        return String.format("version (%s) part %d (%s) is not a valid integer", version, i + 1,
                                versionPart[i]);
                    }
                } catch (Exception e) {
                    return String.format("version (%s) part %d (%s) is not a valid integer", version, i + 1,
                            versionPart);
                }
            }
            return null;
        }

        public static Version getDefault() {
            return V1_0;
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

        @Override
        public int compareTo(Version other) {
            String[] versionPart = this.value.split("\\.");
            String[] otherVersionPart = other.value.split("\\.");

            short major = Short.parseShort(versionPart[0]);
            short oMajor = Short.parseShort(otherVersionPart[0]);
            if (major != oMajor)
                return major - oMajor;

            short minor = Short.parseShort(versionPart[1]);
            short oMinor = Short.parseShort(otherVersionPart[1]);

            return minor - oMinor;
        }

        public boolean newerThan(Version version) {
            return this.compareTo(version) > 0;
        }

        public boolean olderThan(Version version) {
            return this.compareTo(version) < 0;
        }

        public boolean newerThan(String version) {
            return newerThan(Version.get(version));
        }
    }

    /** The default CoAP port for unsecured CoAP communication */
    static final int DEFAULT_COAP_PORT = 5683;

    /** The default CoAP port for secure CoAP communication */
    static final int DEFAULT_COAP_SECURE_PORT = 5684;
}
