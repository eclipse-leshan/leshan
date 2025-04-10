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

import java.util.Objects;

public class TestCredentialsUtil {

    public static final String DER = "der";
    public static final String PEM = "pem";

    public static final Credential CLIENT_CERT_DER = new Credential("client_cert", DER);
    public static final Credential CLIENT_CERT_PEM = new Credential("client_cert", PEM);
    public static final Credential CLIENT_PRIVATE_KEY_DER = new Credential("client_prik", DER);

    public static final Credential CLIENT_CERT_WITH_GARBAGE_DATA_BEFORE_DER = new Credential(
            "client_cert_with_garbage_before", DER);
    public static final Credential CLIENT_CERT_WITH_GARBAGE_DATA_BEFORE_PEM = new Credential(
            "client_cert_with_garbage_before", PEM);

    public static final Credential CLIENT_CERT_WITH_GARBAGE_DATA_AFTER_DER = new Credential(
            "client_cert_with_garbage_after", DER);
    public static final Credential CLIENT_CERT_WITH_GARBAGE_DATA_AFTER_PEM = new Credential(
            "client_cert_with_garbage_after", PEM);

    public static final Credential CLIENT_CHAIN_DER = new Credential("client_chain", DER);
    public static final Credential CLIENT_CHAIN_PEM = new Credential("client_chain", PEM);

    public static final Credential CLIENT_CHAIN_WITH_GARBAGE_DATA_BEFORE_DER = new Credential(
            "client_chain_with_garbage_before", DER);
    public static final Credential CLIENT_CHAIN_WITH_GARBAGE_DATA_BEFORE_PEM = new Credential(
            "client_chain_with_garbage_before", PEM);

    public static final Credential CLIENT_CHAIN_WITH_GARBAGE_DATA_AFTER_DER = new Credential(
            "client_chain_with_garbage_after", DER);
    public static final Credential CLIENT_CHAIN_WITH_GARBAGE_DATA_AFTER_PEM = new Credential(
            "client_chain_with_garbage_after", PEM);

    public static final Credential CLIENT_CHAIN_WITH_GARBAGE_DATA_BETWEEN_DER = new Credential(
            "client_chain_with_garbage_between", DER);
    public static final Credential CLIENT_CHAIN_WITH_GARBAGE_DATA_BETWEEN_PEM = new Credential(
            "client_chain_with_garbage_between", PEM);

    public static class Credential {
        private final String filename;
        private final String extention;

        public Credential(String filename, String extention) {
            super();
            this.filename = filename;
            this.extention = extention;
        }

        public byte[] asByteArray() {
            return TestResourceUtil.loadResourceToByteArray(getPath());
        }

        public String getPath() {
            return String.format("credentials/%s", getFileNameWithExtention());
        }

        public String getFileNameWithExtention() {
            return String.format("%s.%s", filename, extention);
        }

        public String getFilename() {
            return filename;
        }

        public String getExtention() {
            return extention;
        }

        @Override
        public final int hashCode() {
            return Objects.hash(extention, filename);
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Credential other = (Credential) obj;
            return Objects.equals(extention, other.extention) && Objects.equals(filename, other.filename);
        }
    }
}
