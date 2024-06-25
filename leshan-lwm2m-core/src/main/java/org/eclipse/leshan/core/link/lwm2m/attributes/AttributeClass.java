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
package org.eclipse.leshan.core.link.lwm2m.attributes;

/**
 * Attributes are organized according to their purpose;
 * <p>
 * 2 Class of Attributes are supported:
 * <ul>
 * <li>{@link AttributeClass#NOTIFICATION} gather Attributes regarding Notify operations parameters
 * <li>{@link AttributeClass#PROPERTIES} gather Attributes regarding general information
 * </ul>
 */
public enum AttributeClass {
    PROPERTIES, NOTIFICATION
}
