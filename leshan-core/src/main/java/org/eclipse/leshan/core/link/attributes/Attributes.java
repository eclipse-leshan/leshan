/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.leshan.core.link.attributes.ContentFormatAttribute.ContentFormatAttributeModel;
import org.eclipse.leshan.core.link.attributes.ResourceTypeAttribute.ResourceTypeAttributeModel;

/**
 * Some constant about {@link Attribute}
 */
public interface Attributes {

    /**
     * Content Format code attribute 'ct='.
     */
    ContentFormatAttributeModel CT = ContentFormatAttribute.MODEL;

    /**
     * Resource Type attribute 'rt='.
     */
    ResourceTypeAttributeModel RT = ResourceTypeAttribute.MODEL;

    /**
     * All known attributes by Leshan.
     */
    Collection<AttributeModel<?>> ALL = Arrays.asList(CT, RT);
}
