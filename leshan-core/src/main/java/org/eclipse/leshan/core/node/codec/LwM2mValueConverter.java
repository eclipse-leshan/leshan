/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
package org.eclipse.leshan.core.node.codec;

import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;

/**
 * Helper to convert value "magically" from one type to another.
 * <p>
 * This is used by {@link LwM2mEncoder} to fix {@link LwM2mResource} which would used a different {@link Type} than the
 * one defined in the {@link ResourceModel}.
 */
public interface LwM2mValueConverter {

    /**
     * Convert the given value to the expected type given in parameter.
     *
     * @param value the value to convert
     * @param currentType the current type of the value
     * @param expectedType the type expected
     * @param resourcePath the path of the concerned resource
     *
     * @exception CodecException the value is not convertible.
     */
    Object convertValue(Object value, Type currentType, Type expectedType, LwM2mPath resourcePath);

}
