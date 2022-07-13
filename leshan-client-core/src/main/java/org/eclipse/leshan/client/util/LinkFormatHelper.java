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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690.
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

import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.client.servers.ServersInfoExtractor;
import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.attributes.Attribute;
import org.eclipse.leshan.core.link.attributes.ContentFormatAttribute;
import org.eclipse.leshan.core.link.attributes.ResourceTypeAttribute;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLink;
import org.eclipse.leshan.core.link.lwm2m.MixedLwM2mLink;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.util.StringUtils;

/**
 * An Utility class which help to generate @{link Link} from {@link LwM2mObjectEnabler} and {@link LwM2mModel}.<br>
 * Used for register and discover payload.
 */
public final class LinkFormatHelper {

    private LinkFormatHelper() {
    }

    public static Link[] getClientDescription(Collection<LwM2mObjectEnabler> objectEnablers, String rootPath,
            List<ContentFormat> supportedContentFormats) {
        List<Link> links = new ArrayList<>();

        // create links for root
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new ResourceTypeAttribute("oma.lwm2m"));
        // serialize contentFormat;
        if (supportedContentFormats != null && !supportedContentFormats.isEmpty()) {
            attributes.add(new ContentFormatAttribute(supportedContentFormats));
        }

        links.add(new MixedLwM2mLink(rootPath, LwM2mPath.ROOTPATH, attributes));

        // sort object
        List<LwM2mObjectEnabler> objEnablerList = new ArrayList<>(objectEnablers);
        Collections.sort(objEnablerList, new Comparator<LwM2mObjectEnabler>() {
            @Override
            public int compare(LwM2mObjectEnabler o1, LwM2mObjectEnabler o2) {
                return o1.getId() - o2.getId();
            }
        });
        for (LwM2mObjectEnabler objectEnabler : objEnablerList) {
            // skip the security and oscore Object
            if (objectEnabler.getId() == LwM2mId.SECURITY || objectEnabler.getId() == LwM2mId.OSCORE)
                continue;

            List<Integer> availableInstance = objectEnabler.getAvailableInstanceIds();
            // Include an object link if there are no instances or there are object attributes (e.g. "ver")
            List<LwM2mAttribute<?>> objectAttributes = getObjectAttributes(objectEnabler.getObjectModel());
            if (availableInstance.isEmpty() || (objectAttributes != null)) {
                links.add(new MixedLwM2mLink(rootPath, new LwM2mPath(objectEnabler.getId()), objectAttributes));
            }
            for (Integer instanceId : objectEnabler.getAvailableInstanceIds()) {
                links.add(new MixedLwM2mLink(rootPath, new LwM2mPath(objectEnabler.getId(), instanceId)));
            }
        }

        return links.toArray(new Link[] {});
    }

    public static LwM2mLink[] getBootstrapClientDescription(Collection<LwM2mObjectEnabler> objectEnablers) {
        List<Link> links = new ArrayList<>();
        // TODO should be version 1.1 ?
        links.add(new LwM2mLink("/", LwM2mPath.ROOTPATH,
                LwM2mAttributes.create(LwM2mAttributes.ENABLER_VERSION, LwM2mVersion.V1_0)));

        // handle attribute for oscore
        Map<Integer /* oscore instance id */, List<LwM2mAttribute<?>>> oscoreAttributesByInstanceId = extractOscoreAttributes(
                objectEnablers);

        for (LwM2mObjectEnabler objectEnabler : objectEnablers) {
            links.addAll(getBootstrapObjectDescriptionWithoutRoot(objectEnabler, oscoreAttributesByInstanceId));
        }
        return links.toArray(new LwM2mLink[] {});
    }

    private static Map<Integer, List<LwM2mAttribute<?>>> extractOscoreAttributes(
            Collection<LwM2mObjectEnabler> objectEnablers) {
        Map<Integer/* oscore instance id */, List<LwM2mAttribute<?>>> oscoreAttributes = new HashMap<>();
        for (LwM2mObjectEnabler objectEnabler : objectEnablers) {
            if (objectEnabler.getId() == LwM2mId.SECURITY) {
                ReadResponse response = objectEnabler.read(ServerIdentity.SYSTEM, new ReadRequest(LwM2mId.SECURITY));
                if (response.isSuccess()) {
                    LwM2mObject object = (LwM2mObject) response.getContent();
                    for (LwM2mObjectInstance instance : object.getInstances().values()) {
                        Integer oscoreSecurityMode = ServersInfoExtractor.getOscoreSecurityMode(instance);
                        if (oscoreSecurityMode != null) {
                            List<LwM2mAttribute<?>> attributes = new ArrayList<>(2);
                            // extract ssid
                            Long shortServerId = ServersInfoExtractor.getServerId(objectEnabler, instance.getId());
                            if (shortServerId != null)
                                attributes.add(LwM2mAttributes.create(LwM2mAttributes.SHORT_SERVER_ID, shortServerId));
                            // extract uri
                            String uri = ServersInfoExtractor.getServerURI(objectEnabler, instance.getId());
                            if (uri != null)
                                attributes.add(LwM2mAttributes.create(LwM2mAttributes.SERVER_URI, uri));

                            oscoreAttributes.put(oscoreSecurityMode, attributes);
                        }
                    }
                }
                return oscoreAttributes;
            }

        }
        return oscoreAttributes;
    }

    public static LwM2mLink[] getObjectDescription(LwM2mObjectEnabler objectEnabler, String rootPath) {
        List<LwM2mLink> links = new ArrayList<>();

        // create link for "object"
        List<LwM2mAttribute<?>> objectAttributes = getObjectAttributes(objectEnabler.getObjectModel());
        links.add(new LwM2mLink(rootPath, new LwM2mPath(objectEnabler.getId()), objectAttributes));

        // create links for each available instance
        for (Integer instanceId : objectEnabler.getAvailableInstanceIds()) {
            links.addAll(Arrays.asList(getInstanceDescription(objectEnabler, instanceId, rootPath)));
        }

        return links.toArray(new LwM2mLink[links.size()]);
    }

    public static LwM2mLink[] getBootstrapObjectDescription(LwM2mObjectEnabler objectEnabler) {
        List<LwM2mLink> links = new ArrayList<>();
        links.add(new LwM2mLink("/", LwM2mPath.ROOTPATH,
                // TODO should be version 1.1 ?
                LwM2mAttributes.create(LwM2mAttributes.ENABLER_VERSION, LwM2mVersion.V1_0)));

        links.addAll(getBootstrapObjectDescriptionWithoutRoot(objectEnabler, null));

        return links.toArray(new LwM2mLink[] {});
    }

    private static List<LwM2mLink> getBootstrapObjectDescriptionWithoutRoot(LwM2mObjectEnabler objectEnabler,
            Map<Integer, List<LwM2mAttribute<?>>> oscoreAttributesByInstanceId) {
        List<LwM2mLink> links = new ArrayList<>();

        // create link for "object"
        LwM2mLink objectLink;
        Version version = getVersion(objectEnabler.getObjectModel());
        if (version != null) {
            objectLink = new LwM2mLink("/", new LwM2mPath(objectEnabler.getId()),
                    LwM2mAttributes.create(LwM2mAttributes.OBJECT_VERSION, version));
        } else {
            objectLink = new LwM2mLink("/", new LwM2mPath(objectEnabler.getId()));
        }

        // add object link if needed
        List<Integer> availableInstanceIds = objectEnabler.getAvailableInstanceIds();
        if (availableInstanceIds.isEmpty() || !objectLink.getAttributes().isEmpty()) {
            links.add(objectLink);
        }

        // add instance link
        for (Integer instanceId : objectEnabler.getAvailableInstanceIds()) {
            List<LwM2mAttribute<?>> objectAttributes = new ArrayList<>();

            // handle oscore
            if (objectEnabler.getId() == LwM2mId.OSCORE) {
                if (oscoreAttributesByInstanceId != null) {
                    List<LwM2mAttribute<?>> oscoreAttributes = oscoreAttributesByInstanceId.get(instanceId);
                    if (oscoreAttributes != null && !oscoreAttributes.isEmpty()) {
                        objectAttributes.addAll(oscoreAttributes);
                    }
                }
            } else {
                // get short id
                if (objectEnabler.getId() == LwM2mId.SECURITY || objectEnabler.getId() == LwM2mId.SERVER) {
                    Boolean isBootstrapServer = objectEnabler.getId() == LwM2mId.SECURITY
                            && ServersInfoExtractor.isBootstrapServer(objectEnabler, instanceId);
                    if (isBootstrapServer != null && !isBootstrapServer) {
                        Long shortServerId = ServersInfoExtractor.getServerId(objectEnabler, instanceId);
                        if (shortServerId != null)
                            objectAttributes
                                    .add(LwM2mAttributes.create(LwM2mAttributes.SHORT_SERVER_ID, shortServerId));
                    }

                }

                // get uri
                if (objectEnabler.getId() == LwM2mId.SECURITY) {
                    String uri = ServersInfoExtractor.getServerURI(objectEnabler, instanceId);
                    if (uri != null)
                        objectAttributes.add(LwM2mAttributes.create(LwM2mAttributes.SERVER_URI, uri));
                }
            }

            // create link
            links.add(new LwM2mLink("/", new LwM2mPath(objectEnabler.getId(), instanceId), objectAttributes));
        }
        return links;
    }

    public static LwM2mLink[] getInstanceDescription(LwM2mObjectEnabler objectEnabler, int instanceId,
            String rootPath) {
        List<LwM2mLink> links = new ArrayList<>();

        // create link for "instance"
        links.add(new LwM2mLink(rootPath, new LwM2mPath(objectEnabler.getId(), instanceId)));

        // create links for each available resource
        for (Integer resourceId : objectEnabler.getAvailableResourceIds(instanceId)) {
            links.add(getResourceDescription(objectEnabler, instanceId, resourceId, rootPath));
        }
        return links.toArray(new LwM2mLink[links.size()]);
    }

    public static LwM2mLink getResourceDescription(LwM2mObjectEnabler objectEnabler, int instanceId, int resourceId,
            String rootPath) {
        // create link for "resource"
        return new LwM2mLink(rootPath, new LwM2mPath(objectEnabler.getId(), instanceId, resourceId));
    }

    private static List<LwM2mAttribute<?>> getObjectAttributes(ObjectModel objectModel) {
        Version version = getVersion(objectModel);
        if (version == null) {
            return null;
        }

        List<LwM2mAttribute<?>> attributes = new ArrayList<>();
        attributes.add(LwM2mAttributes.create(LwM2mAttributes.OBJECT_VERSION, version));
        return attributes;
    }

    private static Version getVersion(ObjectModel objectModel) {
        if (StringUtils.isEmpty(objectModel.version) || ObjectModel.DEFAULT_VERSION.equals(objectModel.version)) {
            return null;
        }
        return new Version(objectModel.version);
    }
}
