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
package org.eclipse.leshan.client.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;

/**
 * An Utility class which help to generate Link Objects from {@link LwM2mObjectEnabler} and {@link LwM2mModell}.<br>
 * Used for register and discover payload.
 */
public final class LinkFormatHelper {

    private LinkFormatHelper() {
    }

    public static LinkObject[] getClientDescription(List<LwM2mObjectEnabler> objectEnablers, String rootPath) {
        List<LinkObject> linkObjects = new ArrayList<>();

        // clean root path
        rootPath = rootPath == null ? "" : rootPath;

        // create link object for "object"
        Path rootURL = Paths.get("/", rootPath);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("rt", "oma.lwm2m");
        linkObjects.add(new LinkObject(rootURL.toString(), attributes));

        // sort resources
        for (LwM2mObjectEnabler objectEnabler : objectEnablers) {
            List<Integer> availableInstance = objectEnabler.getAvailableInstance();
            if (availableInstance.isEmpty()) {
                Path objectInstanceUrl = Paths.get("/", rootPath, Integer.toString(objectEnabler.getId()));
                linkObjects.add(new LinkObject(objectInstanceUrl.toString()));
            } else {
                for (Integer instanceId : objectEnabler.getAvailableInstance()) {
                    Path objectInstanceUrl = Paths.get("/", rootPath, Integer.toString(objectEnabler.getId()),
                            instanceId.toString());
                    linkObjects.add(new LinkObject(objectInstanceUrl.toString()));
                }
            }
        }

        return linkObjects.toArray(new LinkObject[] {});
    }

    public static LinkObject[] getObjectDescription(ObjectModel objectModel, String rootPath) {
        List<LinkObject> linkObjects = new ArrayList<>();

        // clean root path
        rootPath = rootPath == null ? "" : rootPath;

        // create link object for "object"
        Path objectURL = Paths.get("/", rootPath, Integer.toString(objectModel.id));
        linkObjects.add(new LinkObject(objectURL.toString()));

        // sort resources
        List<ResourceModel> resources = new ArrayList<>(objectModel.resources.values());
        Collections.sort(resources, new Comparator<ResourceModel>() {
            @Override
            public int compare(ResourceModel o1, ResourceModel o2) {
                return o1.id - o2.id;
            }
        });

        // create link object for resource
        for (ResourceModel resourceModel : resources) {
            Path resourceURL = Paths.get("/", rootPath, Integer.toString(objectModel.id), "0",
                    Integer.toString(resourceModel.id));
            linkObjects.add(new LinkObject(resourceURL.toString()));
        }

        return linkObjects.toArray(new LinkObject[] {});
    }

    public static LinkObject getInstanceDescription(ObjectModel objectModel, int instanceId, String rootPath) {
        // clean root path
        rootPath = rootPath == null ? "" : rootPath;

        // create link object for "object"
        Path objectURL = Paths.get("/", rootPath, Integer.toString(objectModel.id), Integer.toString(instanceId));
        return new LinkObject(objectURL.toString());
    }

    public static LinkObject getResourceDescription(int objectId, int instanceId, ResourceModel resourceModel,
            String rootPath) {
        // clean root path
        rootPath = rootPath == null ? "" : rootPath;

        // create link object for "object"
        Path objectURL = Paths.get("/", rootPath, Integer.toString(objectId), Integer.toString(instanceId),
                Integer.toString(resourceModel.id));
        return new LinkObject(objectURL.toString());
    }
}
