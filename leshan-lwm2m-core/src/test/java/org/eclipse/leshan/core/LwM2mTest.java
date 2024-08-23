/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Natalia Krzykała Orange Polska S.A. - initial implementation
 *******************************************************************************/
package org.eclipse.leshan.core;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class LwM2mTest {

    private class ExtendedLwM2mVersion extends LwM2m.LwM2mVersion {
        ExtendedLwM2mVersion(String version, boolean supported) {
            super(version, supported);
        }

        @Override
        public boolean canEqual(Object obj) {
            return (obj instanceof ExtendedLwM2mVersion);
        }
    }

    @Test
    public void assertEqualsHashcodeLwM2mVersion() {
        EqualsVerifier.forClass(LwM2m.LwM2mVersion.class).withRedefinedSuperclass()
                .withRedefinedSubclass(ExtendedLwM2mVersion.class).verify();
    }

    /*
     * private class ExtendedVersion extends LwM2m.Version { ExtendedVersion(int major, int minor) {
     * super(toShortExact(major, "version (%d.%d) major part (%d) is not a valid short", major, minor, major),
     * toShortExact(minor, "version (%d.%d) minor part (%d) is not a valid short", major, minor, minor)); }
     *
     * @Override public boolean canEqual(Object obj) { return (obj instanceof ExtendedVersion); } }
     */

    @Test
    public void assertEqualsHashcodeVersion() {
        // EqualsVerifier.forClass(LwM2m.Version.class).verify();
        // don't know how to create a constructor for the ExtendedVersion, problem with toShortExact being private, not
        // fe package-private
    }
}
