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

import java.util.Objects;

public interface LwM2m {

    /**
     * Version of LWM2M specification.
     */

    public class LwM2mVersion extends Version {

        public static final LwM2mVersion V1_0 = new LwM2mVersion("1.0", true);
        public static final LwM2mVersion V1_1 = new LwM2mVersion("1.1", true);
        private static final LwM2mVersion[] supportedVersions = new LwM2mVersion[] { V1_0, V1_1 };

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
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof LwM2mVersion))
                return false;
            if (!super.equals(o))
                return false;
            LwM2mVersion that = (LwM2mVersion) o;
            return that.canEqual(this) && supported == that.supported;
        }

        public boolean canEqual(Object o) {
            return (o instanceof LwM2mVersion);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), supported);
        }
    }

    /**
     * Generic class to handle Version (e.g. Object versioning)
     */
    public class Version implements Comparable<Version> {

        public static final Version V1_0 = new Version("1.0");
        public static final Version MAX = new Version(Short.MAX_VALUE, Short.MAX_VALUE);

        private static short toShortExact(int value, String format, Object... args) {
            if ((short) value != value) {
                throw new IllegalArgumentException(String.format(format, args));
            }
            return (short) value;
        }

        protected final short major;
        protected final short minor;

        public Version(int major, int minor) {
            this(toShortExact(major, "version (%d.%d) major part (%d) is not a valid short", major, minor, major),
                    toShortExact(minor, "version (%d.%d) minor part (%d) is not a valid short", major, minor, minor));
        }

        public Version(short major, short minor) {
            this.major = major;
            if (this.major < 0) {
                throw new IllegalArgumentException(
                        String.format("version (%d.%d) major part (%d) must not be negative", major, minor, major));
            }
            this.minor = minor;
            if (this.minor < 0) {
                throw new IllegalArgumentException(
                        String.format("version (%d.%d) minor part (%d) must not be negative", major, minor, minor));
            }
        }

        public Version(String version) {
            String err = Version.validate(version);
            if (err != null) {
                throw new IllegalArgumentException(err);
            }
            String[] versionPart = version.split("\\.");
            this.major = Short.parseShort(versionPart[0]);
            this.minor = Short.parseShort(versionPart[1]);
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
                return "version MUST NOT be null or empty";
            // We use split with limit = -1 to be sure "split" will discard trailing empty strings
            String[] versionParts = version.split("\\.", -1);
            if (versionParts.length != 2) {
                return String.format("version (%s) MUST be composed of 2 parts", version);
            }
            for (int i = 0; i < 2; i++) {
                String versionPart = versionParts[i];
                try {
                    if (versionPart.length() > 1 && versionPart.startsWith("0")) {
                        return String.format("version (%s) part %d (%s) must not be prefixed by 0", version, i + 1,
                                versionPart);
                    }
                    short parsedShort = Short.parseShort(versionPart);
                    if (parsedShort < 0) {
                        return String.format("version (%s) part %d (%s) must not be negative", version, i + 1,
                                versionPart);
                    }
                } catch (Exception e) {
                    return String.format("version (%s) part %d (%s) is not a valid short", version, i + 1, versionPart);
                }
            }
            return null;
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

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Version))
                return false;
            Version that = (Version) o;
            return that.canEqual(this) && major == that.major && minor == that.minor;
        }

        public boolean canEqual(Object o) {
            return (o instanceof Version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(major, minor);
        }
    }

    /** The default CoAP port for unsecured CoAP communication */
    public static final int DEFAULT_COAP_PORT = 5683;

    /** The default CoAP port for secure CoAP communication */
    public static final int DEFAULT_COAP_SECURE_PORT = 5684;
}
