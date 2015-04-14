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
package org.eclipse.leshan.server.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.ResourceModel.Operations;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An Utility class used to convert Link Object to ObjectModel or ResourceModel
 */
public class LinkFormatHelper {

    private static final Logger LOG = LoggerFactory.getLogger(LinkFormatHelper.class);

    public static LwM2mPath extractLwM2mPath(String url, String rootPath) {
        return new LwM2mPath(url);
    }

    public static ObjectModel getObjectModel(LinkObject[] linkObjects, String rootPath) {
        if (linkObjects == null || linkObjects.length == 0)
            return null;

        // validate first element which should be the object Id
        LinkObject object = linkObjects[0];
        LwM2mPath objectPath = extractLwM2mPath(object.getUrl(), rootPath);
        if (!objectPath.isObject()) {
            LOG.warn("First link object should be the path of an object {}", object);
            return null;
        }

        Map<Integer, ResourceModel> resources = new HashMap<>();
        for (int i = 1; i < linkObjects.length; i++) {
            LinkObject resource = linkObjects[i];
            LwM2mPath resourcePath = extractLwM2mPath(resource.getUrl(), rootPath);
            if (!resourcePath.isResource()) {
                LOG.warn("A link object with a resource path is expected but we get {}", resource);
                continue;
            }

            if (resourcePath.getObjectId() != objectPath.getObjectId()) {
                LOG.warn("We expect resource description for the object of id {} but we get {}", objectPath.isObject(),
                        resourcePath);
                continue;
            }

            if (resourcePath.getObjectInstanceId() != 0) {
                LOG.warn("The Object instance id of this link Object should be equals to 0", resource);
                continue;
            }

            String resourceName;
            Object titleAttribute = resource.getAttributes().get("title");
            if (titleAttribute != null) {
                resourceName = titleAttribute.toString();
            } else {
                resourceName = resourcePath.toString();
            }

            Type resourceType;
            try {
                Object typeAttribute = resource.getAttributes().get("type");
                if (typeAttribute != null) {
                    resourceType = Type.valueOf(typeAttribute.toString());
                } else {
                    resourceType = Type.OPAQUE;
                }
            } catch (IllegalArgumentException e) {
                resourceType = Type.OPAQUE;
            }

            ResourceModel resourceModel = new ResourceModel(resourcePath.getResourceId(), resourceName, Operations.RWE,
                    false, false, resourceType, "", "", "");
            resources.put(resourcePath.getResourceId(), resourceModel);
        }

        String objectName;
        Object titleAttribute = object.getAttributes().get("title");
        if (titleAttribute != null) {
            objectName = titleAttribute.toString();
        } else {
            objectName = objectPath.toString();
        }

        return new ObjectModel(objectPath.getObjectId(), objectName, "", true, false, resources);
    }

    public static ResourceModel getResourceModel(LinkObject[] linkObjects, String rootPath) {
        return null;
    }
}
