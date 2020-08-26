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

import static org.junit.Assert.*;

import org.eclipse.leshan.core.LwM2m.Version;
import org.junit.Test;

public class VersionTest {
    @Test
    public void is_supported_tests() {
        assertTrue(Version.isSupported("1.0"));
        assertTrue(Version.isSupported("1.1"));
        assertFalse(Version.isSupported("1.2"));
        assertFalse(Version.isSupported(""));
        assertFalse(Version.isSupported(null));
    }

    @Test
    public void compare_tests() {
        assertTrue(Version.get("1.0").compareTo(Version.get("1.2")) < 0);
        assertTrue(Version.get("0.9").compareTo(Version.get("1.2")) < 0);
        assertTrue(Version.get("1.2").compareTo(Version.get("1.2")) == 0);
        assertTrue(Version.get("1.3").compareTo(Version.get("1.2")) > 0);
        assertTrue(Version.get("2.0").compareTo(Version.get("1.2")) > 0);
    }
}
