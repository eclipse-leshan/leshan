/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
 *     Julien Vermillard - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.util.datatype;

import java.util.Arrays;

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.model.ResourceModel;

public class LwM2mValueUtil {

    static public String toPrettyString(ResourceModel.Type type, Object value) {
        switch (type) {
        case OPAQUE:
            // We don't print OPAQUE value as this could be credentials one.
            // Not ideal but didn't find better way for now.
            return ((byte[]) value).length + "Bytes";
        case CORELINK:
            return Arrays.toString((Link[]) value);
        default:
            return value.toString();
        }
    }
}
