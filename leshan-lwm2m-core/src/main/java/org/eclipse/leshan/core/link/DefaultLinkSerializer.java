/*******************************************************************************
 * Copyright (c) 2021 Orange.
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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690.
 *******************************************************************************/
package org.eclipse.leshan.core.link;

/**
 * A Link as defined in http://tools.ietf.org/html/rfc6690.
 */
public class DefaultLinkSerializer implements LinkSerializer {

    private static final String TRAILER = ",";

    /***
     * Serialize severals {@code Link} to {@code String} as defined in http://tools.ietf.org/html/rfc6690.
     *
     * @param linkObjects links to serialize.
     *
     * @return a {@code String} representation like defined in http://tools.ietf.org/html/rfc6690. If LinkObjects is
     *         empty return an empty {@code String};
     */
    @Override
    public String serializeCoreLinkFormat(Link... linkObjects) {
        StringBuilder builder = new StringBuilder();
        if (linkObjects.length != 0) {
            builder.append(linkObjects[0].toCoreLinkFormat());
            for (int i = 1; i < linkObjects.length; i++) {
                builder.append(TRAILER).append(linkObjects[i].toCoreLinkFormat());
            }
        }
        return builder.toString();
    }

}
