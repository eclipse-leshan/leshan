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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.Link;
import org.eclipse.leshan.LwM2mId;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.util.StringUtils;

/**
 * An Utility class which help to generate @{link Link} from {@link LwM2mObjectEnabler} and {@link LwM2mModel}.<br>
 * Used for register and discover payload.
 */
public final class LinkFormatHelper {

    private LinkFormatHelper() {
    }

    public static Link[] getClientDescription(Collection<LwM2mObjectEnabler> objectEnablers, String rootPath) {
        List<Link> links = new ArrayList<>();

        // clean root path
        String root = rootPath == null ? "" : rootPath;

        // create links for "object"
        String rootURL = getPath("/", root);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("rt", "oma.lwm2m");
        links.add(new Link(rootURL, attributes));

        // sort resources
        List<LwM2mObjectEnabler> objEnablerList = new ArrayList<>(objectEnablers);
        Collections.sort(objEnablerList, new Comparator<LwM2mObjectEnabler>() {
            @Override
            public int compare(LwM2mObjectEnabler o1, LwM2mObjectEnabler o2) {
                return o1.getId() - o2.getId();
            }
        });
        for (LwM2mObjectEnabler objectEnabler : objEnablerList) {
            // skip the security Object
            if (objectEnabler.getId() == LwM2mId.SECURITY)
                continue;

            List<Integer> availableInstance = objectEnabler.getAvailableInstanceIds();
            // Include an object link if there are no instances or there are object attributes (e.g. "ver")
            Map<String, ?> objectAttributes = getObjectAttributes(objectEnabler.getObjectModel());
            if (availableInstance.isEmpty() || (objectAttributes != null)) {
                String objectInstanceUrl = getPath("/", root, Integer.toString(objectEnabler.getId()));
                links.add(new Link(objectInstanceUrl, objectAttributes));
            }
            for (Integer instanceId : objectEnabler.getAvailableInstanceIds()) {
                String objectInstanceUrl = getPath("/", root, Integer.toString(objectEnabler.getId()),
                        instanceId.toString());
                links.add(new Link(objectInstanceUrl));
            }
        }

        return links.toArray(new Link[] {});
    }

    public static Link[] getObjectDescription(LwM2mObjectEnabler objectEnabler, String root) {
        List<Link> links = new ArrayList<>();

        // clean root path
        String rootPath = root == null ? "" : root;

        // create link for "object"
        Map<String, ?> objectAttributes = getObjectAttributes(objectEnabler.getObjectModel());
        String objectURL = getPath("/", rootPath, Integer.toString(objectEnabler.getId()));
        links.add(new Link(objectURL, objectAttributes));

        // create links for each available instance
        for (Integer instanceId : objectEnabler.getAvailableInstanceIds()) {
            links.addAll(Arrays.asList(getInstanceDescription(objectEnabler, instanceId, rootPath)));
        }

        return links.toArray(new Link[] {});
    }

    public static Link[] getInstanceDescription(LwM2mObjectEnabler objectEnabler, int instanceId, String root) {
        List<Link> links = new ArrayList<>();

        // clean root path
        String rootPath = root == null ? "" : root;

        // create link for "instance"
        String objectURL = getPath("/", rootPath, Integer.toString(objectEnabler.getId()),
                Integer.toString(instanceId));
        links.add(new Link(objectURL));

        // create links for each available resource
        for (Integer resourceId : objectEnabler.getAvailableResourceIds(instanceId)) {
            links.add(getResourceDescription(objectEnabler, instanceId, resourceId, rootPath));
        }
        return links.toArray(new Link[] {});
    }

    public static Link getResourceDescription(LwM2mObjectEnabler objectEnabler, int instanceId, int resourceId,
            String root) {
        // clean root path
        String rootPath = root == null ? "" : root;

        // create link for "resource"
        String objectURL = getPath("/", rootPath, Integer.toString(objectEnabler.getId()), Integer.toString(instanceId),
                Integer.toString(resourceId));
        return new Link(objectURL);
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
            checkNotNul(c);
            prevChar = c;
        }
        if (prevChar == '/')
            return normalize(input, n, n - 1);
        return input;
    }

    private static void checkNotNul(char c) {
        if (c == '\u0000')
            throw new IllegalArgumentException("Nul character not allowed in path");
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
            checkNotNul(c);
            sb.append(c);
            prevChar = c;
        }
        return sb.toString();
    }

    private static Map<String, ?> getObjectAttributes(ObjectModel objectModel) {
        if (StringUtils.isEmpty(objectModel.version) || ObjectModel.DEFAULT_VERSION.equals(objectModel.version)) {
            return null;
        }
        Map<String, String> attributes = new HashMap<>();
        attributes.put("ver", objectModel.version);
        return attributes;
    }
}
