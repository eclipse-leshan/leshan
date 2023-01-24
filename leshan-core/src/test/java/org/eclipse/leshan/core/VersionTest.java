/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.LwM2m.Version;
import org.junit.jupiter.api.Test;

public class VersionTest {
    @Test
    public void is_supported_tests() {
        assertTrue(LwM2mVersion.isSupported("1.0"));
        assertTrue(LwM2mVersion.isSupported("1.1"));
        assertFalse(LwM2mVersion.isSupported("1.2"));
        assertFalse(LwM2mVersion.isSupported(""));
        assertFalse(LwM2mVersion.isSupported(null));
    }

    @Test
    public void compare_tests() {
        assertTrue(new Version("1.0").compareTo(new Version("1.2")) < 0);
        assertTrue(new Version("0.9").compareTo(new Version("1.2")) < 0);
        assertTrue(new Version("1.2").compareTo(new Version("1.2")) == 0);
        assertTrue(new Version("1.3").compareTo(new Version("1.2")) > 0);
        assertTrue(new Version("2.0").compareTo(new Version("1.2")) > 0);
    }
}
