/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.request;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;

public class ContentFormatHelper {

    /**
     * Guess the content format for a given payload.
     * 
     * By default, {@link ContentFormat#TLV} is used for multiple values payload.
     */
    public static ContentFormat compute(LwM2mPath path, LwM2mNode node, LwM2mModel model) {
        ContentFormat format;

        // Use text for single resource ...
        if (path.isResource()) {
            // Use resource description to guess
            final ResourceModel description = model.getResourceModel(path.getObjectId(), path.getResourceId());
            if (description != null) {
                if (description.multiple) {
                    format = ContentFormat.TLV;
                } else {
                    format = description.type == Type.OPAQUE ? ContentFormat.OPAQUE : ContentFormat.TEXT;
                }
            }
            // If no object description available, use 'node' to guess
            else {
                LwM2mResource resourceNode = ((LwM2mResource) node);
                if (resourceNode.isMultiInstances()) {
                    format = ContentFormat.TLV;
                } else {
                    format = resourceNode.getType() == Type.OPAQUE ? ContentFormat.OPAQUE : ContentFormat.TEXT;
                }
            }
        }
        // ... and TLV for other ones.
        else {
            format = ContentFormat.TLV;
        }

        return format;
    }

}
