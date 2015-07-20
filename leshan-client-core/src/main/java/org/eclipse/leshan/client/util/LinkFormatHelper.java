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

import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;

/**
 * An Utility class which help to generate Link Objects from {@link LwM2mObjectEnabler} and {@link LwM2mModel}.<br>
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
        String rootURL = getPath("/", rootPath);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("rt", "oma.lwm2m");
        linkObjects.add(new LinkObject(rootURL, attributes));

        // sort resources
        for (LwM2mObjectEnabler objectEnabler : objectEnablers) {
            List<Integer> availableInstance = objectEnabler.getAvailableInstanceIds();
            if (availableInstance.isEmpty()) {
                String objectInstanceUrl = getPath("/", rootPath, Integer.toString(objectEnabler.getId()));
                linkObjects.add(new LinkObject(objectInstanceUrl));
            } else {
                for (Integer instanceId : objectEnabler.getAvailableInstanceIds()) {
                    String objectInstanceUrl = getPath("/", rootPath, Integer.toString(objectEnabler.getId()),
                            instanceId.toString());
                    linkObjects.add(new LinkObject(objectInstanceUrl));
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
        String objectURL = getPath("/", rootPath, Integer.toString(objectModel.id));
        linkObjects.add(new LinkObject(objectURL));

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
            String resourceURL = getPath("/", rootPath, Integer.toString(objectModel.id), "0",
                    Integer.toString(resourceModel.id));
            linkObjects.add(new LinkObject(resourceURL));
        }

        return linkObjects.toArray(new LinkObject[] {});
    }

    public static LinkObject getInstanceDescription(ObjectModel objectModel, int instanceId, String rootPath) {
        // clean root path
        rootPath = rootPath == null ? "" : rootPath;

        // create link object for "object"
        String objectURL = getPath("/", rootPath, Integer.toString(objectModel.id), Integer.toString(instanceId));
        return new LinkObject(objectURL);
    }

    public static LinkObject getResourceDescription(int objectId, int instanceId, ResourceModel resourceModel,
            String rootPath) {
        // clean root path
        rootPath = rootPath == null ? "" : rootPath;

        // create link object for "object"
        String objectURL = getPath("/", rootPath, Integer.toString(objectId), Integer.toString(instanceId),
                Integer.toString(resourceModel.id));
        return new LinkObject(objectURL);
    }

    private static final String getPath(String first, String... more) {
        String path;
        if (more.length == 0) {
            path = first;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(first);
            for (String segment : more) {
                if (segment.length() > 0) {
                    if (sb.length() > 0)
                        sb.append('/');
                    sb.append(segment);
                }
            }
            path = sb.toString();
        }
        return normalizeAndCheck(path);
    }

    private static String normalizeAndCheck(String input) {
        int n = input.length();
        char prevChar = 0;
        for (int i = 0; i < n; i++) {
            char c = input.charAt(i);
            if ((c == '/') && (prevChar == '/'))
                return normalize(input, n, i - 1);
            checkNotNul(input, c);
            prevChar = c;
        }
        if (prevChar == '/')
            return normalize(input, n, n - 1);
        return input;
    }

    private static void checkNotNul(String input, char c) {
        if (c == '\u0000')
            throw new InvalidPathException(input, "Nul character not allowed");
    }

    private static String normalize(String input, int len, int off) {
        if (len == 0)
            return input;
        int n = len;
        while ((n > 0) && (input.charAt(n - 1) == '/'))
            n--;
        if (n == 0)
            return "/";
        StringBuilder sb = new StringBuilder(input.length());
        if (off > 0)
            sb.append(input.substring(0, off));
        char prevChar = 0;
        for (int i = off; i < n; i++) {
            char c = input.charAt(i);
            if ((c == '/') && (prevChar == '/'))
                continue;
            checkNotNul(input, c);
            sb.append(c);
            prevChar = c;
        }
        return sb.toString();
    }
}
