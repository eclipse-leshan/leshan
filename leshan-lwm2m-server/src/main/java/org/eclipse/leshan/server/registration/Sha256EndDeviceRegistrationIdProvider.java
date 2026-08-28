/*******************************************************************************
 * Copyright (c) 2026 Sierra Wireless and others.
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
package org.eclipse.leshan.server.registration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * A {@link EndDeviceRegistrationIdProducer} implementation deriving the child registration ID from a SHA-256 hash of
 * the parent gateway registration ID, the end device's endpoint name (Device ID), and the end device's prefix.
 * <p>
 * The resulting ID is deterministic: the same {@code (gatewayRegistrationId, endpoint, prefix)} triple always produces
 * the same child registration ID. This is intentional for two reasons:
 * <ul>
 * <li>If the LwM2M Gateway changes the prefix associated with an end device, the child registration ID changes too.
 * This is desirable because CoAP observations are bound to a URI containing the prefix, so a prefix change should be
 * treated as a fresh registration (old observations dead, new ones must be re-established).</li>
 * <li>If the gateway itself re-registers (new gateway registration ID), all children get new IDs, which correctly
 * reflects the fact that the underlying transport session has changed.</li>
 * </ul>
 * <p>
 * The hash input is prefixed with a versioned domain separator so that any future change to the derivation scheme
 * (added field, normalization, algorithm switch) can be introduced without ambiguity against previously computed IDs.
 * <p>
 * The output is a URL-safe Base64 encoded truncation of the SHA-256 digest. 16 bytes (128 bits, encoded as 22 Base64
 * characters) is used by default, which gives collision resistance far beyond any realistic device population.
 */
public class Sha256EndDeviceRegistrationIdProvider implements EndDeviceRegistrationIdProvider {

    /**
     * Domain separator identifying (purpose, version) of the hash.
     * <p>
     * Bump the version suffix if the derivation scheme ever changes (e.g. new field added, input normalization
     * introduced). This guarantees old and new schemes cannot produce colliding IDs for related inputs.
     */
    private static final String DOMAIN_SEPARATOR = "lwm2m-gw-child-v1";

    /** Field separator used inside the hash input. */
    private static final String FIELD_SEPARATOR = "|";

    /** Default number of digest bytes kept. 16 bytes = 128 bits, encoded as 22 Base64 URL-safe characters. */
    private static final int DEFAULT_BYTE_LENGTH = 16;

    private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final int byteLength;

    public Sha256EndDeviceRegistrationIdProvider() {
        this(DEFAULT_BYTE_LENGTH);
    }

    /**
     * @param byteLength number of digest bytes to keep (must be between 4 and 32, inclusive). 32 keeps the full SHA-256
     *        output (43 Base64 characters).
     */
    public Sha256EndDeviceRegistrationIdProvider(int byteLength) {
        if (byteLength < 4 || byteLength > 32) {
            throw new IllegalArgumentException("byteLength must be between 4 and 32");
        }
        this.byteLength = byteLength;
    }

    @Override
    public String getRegistrationId(DeviceRegistration parentGateway, EndDeviceData endDeviceData) {
        if (parentGateway == null) {
            throw new IllegalArgumentException("parentGateway must not be null");
        }
        if (endDeviceData == null) {
            throw new IllegalArgumentException("endDeviceData must not be null");
        }

        // endpoint and prefix are guaranteed non-null by EndDeviceData's constructor.
        String input = DOMAIN_SEPARATOR + FIELD_SEPARATOR + parentGateway.getId() + FIELD_SEPARATOR
                + endDeviceData.getEndpoint() + FIELD_SEPARATOR + endDeviceData.getPrefix();

        byte[] digest = sha256(input.getBytes(StandardCharsets.UTF_8));
        byte[] truncated = new byte[byteLength];
        System.arraycopy(digest, 0, truncated, 0, byteLength);
        return BASE64_ENCODER.encodeToString(truncated);
    }

    private static byte[] sha256(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(input);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required to be available in every JRE, so this should never happen.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
