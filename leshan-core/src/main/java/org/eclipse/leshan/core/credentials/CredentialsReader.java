/*******************************************************************************
 * Copyright (c) 2018 Sierra Wireless and others.
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
package org.eclipse.leshan.core.credentials;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.*;
import java.security.GeneralSecurityException;

/**
 * An helper class to read credentials from various input. <br>
 * To be used you MUST implement at least one method between decode(byte[]) and decode(InputStream).
 */
public abstract class CredentialsReader<T> {

    /**
     * Read credential from file
     */
    public T readFromFile(String fileName) throws IOException, GeneralSecurityException {
        try (RandomAccessFile f = new RandomAccessFile(fileName, "r")) {
            byte[] bytes = new byte[(int) f.length()];
            f.readFully(bytes);
            return decode(bytes);
        }
    }

    /**
     * Read credential from resource (in a jar, war, ...)
     *
     * @see java.lang.ClassLoader#getResourceAsStream(String)
     */
    public T readFromResource(String resourcePath) throws IOException, GeneralSecurityException {
        try (InputStream in = ClassLoader.getSystemResourceAsStream(resourcePath)) {
            return decode(in);
        }
    }

    /**
     * Decode credential from byte array.
     */
    public T decode(byte[] bytes) throws IOException, GeneralSecurityException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            return decode(in);
        }
    }

    /**
     * Decode credential from an InputStream.
     */
    public T decode(InputStream in) throws IOException, GeneralSecurityException {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();

            return decode(buffer.toByteArray());
        }
    }
}
