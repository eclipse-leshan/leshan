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
package org.eclipse.leshan.core.link.attributes;

/**
 * A key/value pair that describes the link or its target.
 * <p>
 * See https://datatracker.ietf.org/doc/html/rfc6690#section-1.3
 */
public interface Attribute {

    /**
     * @return the name of the attribute
     */
    String getName();

    /**
     * @return <code>true</code> if this Attribute has a value.
     */
    boolean hasValue();

    /**
     * @return the typed value of the attribute or <code>null</code> if this value hasn't any attribute.
     */
    Object getValue();

    /**
     * @return the value as it should be serialized for the CoreLink Format.
     */
    String getCoreLinkValue();

    /**
     * @return a attribute String as it should be serialized for the coreLink Format.
     *         <p>
     *         It should looks like : <code>"getName()=getCorelinkValue()"</code>
     */
    String toCoreLinkFormat();
}
