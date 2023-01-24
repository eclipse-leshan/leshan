/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.core.node;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class LwM2mParhTest {

    @Test
    public void test_compare_path() {
        assertEquals("/", "/");
        assertEquals("/1", "/1");
        assertEquals("/1/2", "/1/2");
        assertEquals("/1/2/3", "/1/2/3");
        assertEquals("/1/2/3/4", "/1/2/3/4");

        assertFirstSmaller("/", "/1");
        assertFirstSmaller("/", "/1/1");
        assertFirstSmaller("/", "/1/1/1");
        assertFirstSmaller("/", "/1/1/1/1");

        assertFirstSmaller("/1", "/1/1");
        assertFirstSmaller("/1", "/1/1/1");
        assertFirstSmaller("/1", "/1/1/1/1");

        assertFirstSmaller("/1/1", "/1/1/1");
        assertFirstSmaller("/1/1", "/1/1/1/1");

        assertFirstSmaller("/1/1/1", "/1/1/1/1");

        assertFirstSmaller("/1", "/2");
        assertFirstSmaller("/1/1", "/2");
        assertFirstSmaller("/1/1/1", "/2");
        assertFirstSmaller("/1/1/1/1", "/2");

        assertFirstSmaller("/1", "/2/1");
        assertFirstSmaller("/1/1", "/2/1");
        assertFirstSmaller("/1/1/1", "/2/1");
        assertFirstSmaller("/1/1/1/1", "/2/1");

        assertFirstSmaller("/1", "/2/1/1");
        assertFirstSmaller("/1/1", "/2/1/1");
        assertFirstSmaller("/1/1/1", "/2/1/1");
        assertFirstSmaller("/1/1/1/1", "/2/1/1");

        assertFirstSmaller("/1", "/2/1/1/1");
        assertFirstSmaller("/1/1", "/2/1/1/1");
        assertFirstSmaller("/1/1/1", "/2/1/1/1");
        assertFirstSmaller("/1/1/1/1", "/2/1/1/1");
    }

    private void assertEquals(String path1, String path2) {
        assertTrue(new LwM2mPath(path1).compareTo(new LwM2mPath(path2)) == 0);
    }

    private void assertFirstSmaller(String path1, String path2) {
        assertTrue(new LwM2mPath(path1).compareTo(new LwM2mPath(path2)) == -1);
        assertTrue(new LwM2mPath(path2).compareTo(new LwM2mPath(path1)) == 1);

    }
}
