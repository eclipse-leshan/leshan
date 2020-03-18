/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
package org.eclipse.leshan.core.node.codec.opaque;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.CodecException;

public class LwM2mNodeOpaqueDecoder {

    public static LwM2mNode decode(byte[] content, LwM2mPath path, LwM2mModel model) throws CodecException {
        if (!path.isResource())
            throw new CodecException("Invalid path %s : OpaqueDecoder decodes resource only", path);

        ResourceModel desc = model.getResourceModel(path.getObjectId(), path.getResourceId());
        if (desc != null && desc.type != Type.OPAQUE) {
            throw new CodecException(
                    "Invalid content format [%s] for path [%s], OPAQUE can only be used for single OPAQUE resource",
                    desc.type, path);
        }
        return LwM2mSingleResource.newBinaryResource(path.getResourceId(), content != null ? content : new byte[0]);
    }

}
