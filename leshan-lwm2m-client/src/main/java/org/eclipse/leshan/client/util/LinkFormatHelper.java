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
import org.eclipse.leshan.client.servers.LwM2mServer;
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
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.link.lwm2m.attributes.MixedLwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.NotificationAttributeTree;
import org.eclipse.leshan.core.model.LwM2mCoreObjectVersionRegistry;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mNodeLevel;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;

/**
 * An Utility class which help to generate @{link Link} from {@link LwM2mObjectEnabler} and {@link LwM2mModel}.<br>
 * Used for register and discover payload.
 */
public class LinkFormatHelper {

    protected LwM2mCoreObjectVersionRegistry versionRegistry = new LwM2mCoreObjectVersionRegistry();
    protected LwM2mVersion version;

    public LinkFormatHelper(LwM2mVersion version) {
        this.version = version;
    }

    public Link[] getClientDescription(Collection<LwM2mObjectEnabler> objectEnablers, String rootPath,
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
            LwM2mAttributeSet objectAttributes = getObjectAttributes(objectEnabler.getObjectModel());
            if (availableInstance.isEmpty() || (!objectAttributes.isEmpty())) {
                links.add(new MixedLwM2mLink(rootPath, new LwM2mPath(objectEnabler.getId()),
                        new MixedLwM2mAttributeSet(objectAttributes.asCollection())));
            }
            for (Integer instanceId : objectEnabler.getAvailableInstanceIds()) {
                links.add(new MixedLwM2mLink(rootPath, new LwM2mPath(objectEnabler.getId(), instanceId)));
            }
        }

        return links.toArray(new Link[] {});
    }

    public LwM2mLink[] getBootstrapClientDescription(Collection<LwM2mObjectEnabler> objectEnablers) {
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

    private Map<Integer, List<LwM2mAttribute<?>>> extractOscoreAttributes(
            Collection<LwM2mObjectEnabler> objectEnablers) {
        Map<Integer/* oscore instance id */, List<LwM2mAttribute<?>>> oscoreAttributes = new HashMap<>();
        for (LwM2mObjectEnabler objectEnabler : objectEnablers) {
            if (objectEnabler.getId() == LwM2mId.SECURITY) {
                ReadResponse response = objectEnabler.read(LwM2mServer.SYSTEM, new ReadRequest(LwM2mId.SECURITY));
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

    public LwM2mLink[] getObjectDescription(LwM2mServer server, LwM2mObjectEnabler objectEnabler, String rootPath) {
        return getObjectDescription(server, objectEnabler, rootPath, LwM2mNodeLevel.RESOURCE);
    }

    public LwM2mLink[] getObjectDescription(LwM2mServer server, LwM2mObjectEnabler objectEnabler, String rootPath,
            LwM2mNodeLevel depth) {
        List<LwM2mLink> links = new ArrayList<>();

        // create link for "object"
        LwM2mPath objectPath = new LwM2mPath(objectEnabler.getId());
        LwM2mAttributeSet objectAttributes = getObjectAttributes(objectEnabler.getObjectModel());
        LwM2mAttributeSet notificationAttributes = getNotificationAttributeFor(server, objectEnabler, objectPath);
        links.add(new LwM2mLink(rootPath, objectPath, objectAttributes.merge(notificationAttributes)));

        // create links for each available instance
        if (depth.isDeeperOrEqualThan(LwM2mNodeLevel.OBJECT_INSTANCE)) {
            for (Integer instanceId : objectEnabler.getAvailableInstanceIds()) {
                links.addAll(Arrays.asList(getInstanceDescription(server, objectEnabler, instanceId, rootPath, depth)));
            }
        }

        return links.toArray(new LwM2mLink[links.size()]);
    }

    public LwM2mLink[] getBootstrapObjectDescription(LwM2mObjectEnabler objectEnabler) {
        List<LwM2mLink> links = new ArrayList<>();
        links.add(new LwM2mLink("/", LwM2mPath.ROOTPATH,
                // TODO should be version 1.1 ?
                LwM2mAttributes.create(LwM2mAttributes.ENABLER_VERSION, LwM2mVersion.V1_0)));

        links.addAll(getBootstrapObjectDescriptionWithoutRoot(objectEnabler, null));

        return links.toArray(new LwM2mLink[] {});
    }

    protected List<LwM2mLink> getBootstrapObjectDescriptionWithoutRoot(LwM2mObjectEnabler objectEnabler,
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

    public LwM2mLink[] getInstanceDescription(LwM2mServer server, LwM2mObjectEnabler objectEnabler, int instanceId,
            String rootPath) {
        return getInstanceDescription(server, objectEnabler, instanceId, rootPath, LwM2mNodeLevel.RESOURCE);
    }

    public LwM2mLink[] getInstanceDescription(LwM2mServer server, LwM2mObjectEnabler objectEnabler, int instanceId,
            String rootPath, LwM2mNodeLevel depth) {
        List<LwM2mLink> links = new ArrayList<>();

        // create link for "instance"
        LwM2mPath instancePath = new LwM2mPath(objectEnabler.getId(), instanceId);
        LwM2mAttributeSet instanceAttributes = getNotificationAttributeFor(server, objectEnabler, instancePath);
        links.add(new LwM2mLink(rootPath, instancePath, instanceAttributes));

        // create links for each available resource
        if (depth.isDeeperOrEqualThan(LwM2mNodeLevel.RESOURCE)) {
            for (Integer resourceId : objectEnabler.getAvailableResourceIds(instanceId)) {
                links.addAll(Arrays.asList(
                        getResourceDescription(server, objectEnabler, instanceId, resourceId, rootPath, depth)));
            }
        }
        return links.toArray(new LwM2mLink[links.size()]);
    }

    public LwM2mLink[] getResourceDescription(LwM2mServer server, LwM2mObjectEnabler objectEnabler, int instanceId,
            int resourceId, String rootPath) {
        return getResourceDescription(server, objectEnabler, instanceId, resourceId, rootPath,
                LwM2mNodeLevel.RESOURCE_INSTANCE);
    }

    public LwM2mLink[] getResourceDescription(LwM2mServer server, LwM2mObjectEnabler objectEnabler, int instanceId,
            int resourceId, String rootPath, LwM2mNodeLevel depth) {
        List<LwM2mLink> links = new ArrayList<>();

        // create link for "resource"
        LwM2mPath resourcePath = new LwM2mPath(objectEnabler.getId(), instanceId, resourceId);
        LwM2mAttributeSet resourceAttributes = getNotificationAttributeFor(server, objectEnabler, resourcePath);

        List<Integer> availableInstanceResourceIds = objectEnabler.getAvailableInstanceResourceIds(instanceId,
                resourceId);
        if (!availableInstanceResourceIds.isEmpty()) {
            links.add(new LwM2mLink(rootPath, resourcePath, resourceAttributes.merge(
                    LwM2mAttributes.create(LwM2mAttributes.DIMENSION, (long) availableInstanceResourceIds.size()))));
        } else {
            links.add(new LwM2mLink(rootPath, resourcePath, resourceAttributes));
        }

        // create links for each available instance resource
        if (!availableInstanceResourceIds.isEmpty() && depth.isResourceInstance()) {
            for (Integer resourceInstanceId : availableInstanceResourceIds) {
                LwM2mPath resourceInstancePath = new LwM2mPath(objectEnabler.getId(), instanceId, resourceId,
                        resourceInstanceId);
                LwM2mAttributeSet resourceInstanceAttributes = getNotificationAttributeFor(server, objectEnabler,
                        resourceInstancePath);
                links.add(new LwM2mLink(rootPath, resourceInstancePath, resourceInstanceAttributes));
            }
        }
        return links.toArray(new LwM2mLink[links.size()]);
    }

    protected LwM2mAttributeSet getNotificationAttributeFor(LwM2mServer server, LwM2mObjectEnabler objectEnabler,
            LwM2mPath path) {
        NotificationAttributeTree attributeTree = objectEnabler.getAttributesFor(server);
        if (attributeTree == null) {
            return new LwM2mAttributeSet();
        }
        LwM2mAttributeSet attributes = attributeTree.get(path);
        if (attributes == null) {
            return new LwM2mAttributeSet();
        }
        return attributes;
    }

    protected LwM2mAttributeSet getObjectAttributes(ObjectModel objectModel) {
        Version version = getVersion(objectModel);
        if (version == null) {
            return new LwM2mAttributeSet();
        }

        List<LwM2mAttribute<?>> attributes = new ArrayList<>();
        attributes.add(LwM2mAttributes.create(LwM2mAttributes.OBJECT_VERSION, version));
        return new LwM2mAttributeSet(attributes);
    }

    protected Version getVersion(ObjectModel objectModel) {
        if (versionRegistry.isCoreObject(objectModel.id, version)) {
            if (versionRegistry.isDefaultVersion(new Version(objectModel.version), objectModel.id, version)) {
                return null;
            }
        } else {
            if (Version.V1_0.equals(new Version(objectModel.version))) {
                return null;
            }
        }
        return new Version(objectModel.version);
    }
}
