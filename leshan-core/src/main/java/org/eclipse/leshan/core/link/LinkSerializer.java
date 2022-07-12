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
 * A CoRE Link serializer interface.
 * <p>
 * Serializer will serialize links {@link Link} in format defined in
 * https://datatracker.ietf.org/doc/html/RFC6690#section-2
 */
public interface LinkSerializer {

    /**
     * Serialize links {@link Link} into String in format defined in RFC RFC6690.
     *
     * @param linkObjects array of {@link Link}
     */
    String serializeCoreLinkFormat(Link... linkObjects);
}
