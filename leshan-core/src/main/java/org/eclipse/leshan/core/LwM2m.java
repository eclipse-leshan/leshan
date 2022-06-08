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


// Some code modification to test github action
public interface LwM2m {

    /**
     * Version of LWM2M specification.
     */
    public class LwM2mVersion extends Version {

        public static LwM2mVersion V1_0 = new LwM2mVersion("1.0", true);
        public static LwM2mVersion V1_1 = new LwM2mVersion("1.1", true);
        private static LwM2mVersion[] supportedVersions = new LwM2mVersion[] { V1_0, V1_1 };

        private final boolean supported;

        protected LwM2mVersion(String version, boolean supported) {
            super(version);
            this.supported = supported;
        }

        public boolean isSupported() {
            return supported;
        }

        public static LwM2mVersion get(String version) {
            for (LwM2mVersion constantVersion : supportedVersions) {
                if (constantVersion.toString().equals(version)) {
                    return constantVersion;
                }
            }
            return new LwM2mVersion(version, false);
        }

        public static boolean isSupported(String version) {
            for (LwM2mVersion constantVersion : supportedVersions) {
                if (constantVersion.toString().equals(version)) {
                    return true;
                }
            }
            return false;
        }

        public static LwM2mVersion getDefault() {
            return V1_0;
        }

        public static LwM2mVersion lastSupported() {
            return V1_1;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + (supported ? 1231 : 1237);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj))
                return false;
            if (getClass() != obj.getClass())
                return false;
            LwM2mVersion other = (LwM2mVersion) obj;
            if (supported != other.supported)
                return false;
            return true;
        }
    }

    /**
     * Generic class to handle Version (e.g. Object versioning)
     */
    public class Version implements Comparable<Version> {

        public static Version V1_0 = new Version("1.0");
        public static final Version MAX = new Version(Short.MAX_VALUE, Short.MIN_VALUE);

        protected final Short major;
        protected final Short minor;

        public Version(Short major, Short minor) {
            this.major = major;
            this.minor = minor;
        }

        public Version(String version) {
            try {
                String[] versionPart = version.split("\\.");
                this.major = Short.parseShort(versionPart[0]);
                this.minor = Short.parseShort(versionPart[1]);
            } catch (RuntimeException e) {
                String err = Version.validate(version);
                if (err != null) {
                    throw new IllegalArgumentException(err);
                } else {
                    throw e;
                }
            }
        }

        @Override
        public String toString() {
            return String.format("%d.%d", major, minor);
        }

        public static Version getDefault() {
            return V1_0;
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
                    Short parsedShort = Short.parseShort(versionPart[i]);
                    if (parsedShort < 0) {
                        return String.format("version (%s) part %d (%s) is not a valid short", version, i + 1,
                                versionPart[i]);
                    }
                } catch (Exception e) {
                    return String.format("version (%s) part %d (%s) is not a valid short", version, i + 1,
                            versionPart[i]);
                }
            }
            return null;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((major == null) ? 0 : major.hashCode());
            result = prime * result + ((minor == null) ? 0 : minor.hashCode());
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
            if (major == null) {
                if (other.major != null)
                    return false;
            } else if (!major.equals(other.major))
                return false;
            if (minor == null) {
                if (other.minor != null)
                    return false;
            } else if (!minor.equals(other.minor))
                return false;
            return true;
        }

        @Override
        public int compareTo(Version other) {
            if (major != other.major)
                return major - other.major;

            return minor - other.minor;
        }

        public boolean newerThan(Version version) {
            return this.compareTo(version) > 0;
        }

        public boolean olderThan(Version version) {
            return this.compareTo(version) < 0;
        }

        public boolean newerThan(String version) {
            return newerThan(new Version(version));
        }
    }

    /** The default CoAP port for unsecured CoAP communication */
    public static final int DEFAULT_COAP_PORT = 5683;

    /** The default CoAP port for secure CoAP communication */
    public static final int DEFAULT_COAP_SECURE_PORT = 5684;
}
