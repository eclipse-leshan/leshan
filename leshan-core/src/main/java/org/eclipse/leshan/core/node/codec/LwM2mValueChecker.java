/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
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

import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mPath;

/**
 * A {@link LwM2mValueConverter} which do no conversion but raise a {@link CodecException} if type is not the expected
 * one.
 *
 */
public class LwM2mValueChecker implements LwM2mValueConverter {

    @Override
    public Object convertValue(Object value, Type currentType, Type expectedType, LwM2mPath resourcePath) {
        if (expectedType == null) {
            // unknown resource, trusted value
            return value;
        }

        if (currentType == expectedType) {
            // expected type
            return value;
        }

        throw new CodecException("Invalid value type for resource %s, expected %s, got %s", resourcePath, expectedType,
                currentType);
    }
}
