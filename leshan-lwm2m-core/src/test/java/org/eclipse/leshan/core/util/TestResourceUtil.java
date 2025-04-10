/*******************************************************************************
 * Copyright (c) 2025 Sierra Wireless and others.
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
package org.eclipse.leshan.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TestResourceUtil {

    private TestResourceUtil() {
    }

    public static String getResourceName(String filename) {
        return String.format("credentials/%s", filename);
    }

    public static String getFileName(String filename, String extension) {
        return String.format("%s.%s", filename, extension);
    }

    public static byte[] loadResourceToByteArray(String resourcePath) {
        try (InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException(String.format("Resource %s not found: ", resourcePath));
            }

            // read files
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            for (int n; (n = inputStream.read(buffer)) != -1;)
                out.write(buffer, 0, n);

            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Unable to read Resource %s", e));
        }
    }
}
