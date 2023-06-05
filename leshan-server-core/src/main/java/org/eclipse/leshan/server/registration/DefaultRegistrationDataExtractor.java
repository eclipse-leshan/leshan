/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.server.registration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.attributes.Attributes;
import org.eclipse.leshan.core.link.attributes.ContentFormatAttribute;
import org.eclipse.leshan.core.link.attributes.ResourceTypeAttribute;
import org.eclipse.leshan.core.link.lwm2m.MixedLwM2mLink;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.model.LwM2mCoreObjectVersionRegistry;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;

public class DefaultRegistrationDataExtractor implements RegistrationDataExtractor {

    protected LwM2mCoreObjectVersionRegistry versionRegistry = new LwM2mCoreObjectVersionRegistry();

    @Override
    public RegistrationData extractDataFromObjectLinks(Link[] objectLinks, LwM2mVersion lwM2mVersion) {
        RegistrationData data = new RegistrationData();
        if (objectLinks != null) {

            // Search LWM2M root link
            Link root = searchRootLink(objectLinks);

            // extract alternate path
            data.setAlternatePath(extractAlternatePath(root));

            // extract supported Content format in root link
            data.setSupportedContentFormats(extractContentFormat(lwM2mVersion, root));

            // Extract data from link object
            Map<Integer, Version> supportedObjects = new HashMap<>();
            Set<LwM2mPath> availableInstances = new HashSet<>();
            extractAvailableInstancesAndSupportedObjects(lwM2mVersion, objectLinks, supportedObjects,
                    availableInstances);
            data.setSupportedObjects(supportedObjects);
            data.setAvailableInstances(availableInstances);

        }
        return data;
    }

    protected Link searchRootLink(Link[] objectLinks) {
        Link root = null;
        for (Link link : objectLinks) {
            if (link != null) {
                ResourceTypeAttribute rt = link.getAttributes().get(Attributes.RT);
                if (rt != null && rt.getValue().contains("oma.lwm2m")) {
                    // this link has the ResourceType oma.lwm2m this is the LWM2M root for sure.
                    root = link;
                    break;
                } else if (link.getUriReference().equals("/")) {
                    // this link refer to "/", so could be the LWM2M root link unless another one has the
                    // ResourceType oma.lwm2m, so we continue to search.
                    root = link;
                }
            }
        }
        return root;
    }

    protected String extractAlternatePath(Link rootLink) {
        String rootPath;
        if (rootLink != null) {
            rootPath = rootLink.getUriReference();
            if (!rootPath.endsWith("/")) {
                rootPath = rootPath + "/";
            }
        } else {
            rootPath = "/";
        }
        return rootPath;
    }

    protected Set<ContentFormat> extractContentFormat(LwM2mVersion lwM2mVersion, Link rootLink) {
        Set<ContentFormat> supportedContentFormats = new HashSet<>();

        // add content format from ct attributes
        if (rootLink != null) {
            ContentFormatAttribute ctValue = rootLink.getAttributes().get(Attributes.CT);
            if (ctValue != null) {
                supportedContentFormats.addAll(ctValue.getValue());
            }
        }

        // add mandatory content format
        for (ContentFormat format : ContentFormat.knownContentFormat) {
            if (format.isMandatoryForClient(lwM2mVersion)) {
                supportedContentFormats.add(format);
            }
        }
        return supportedContentFormats;
    }

    protected void extractAvailableInstancesAndSupportedObjects(LwM2mVersion lwM2mVersion, Link[] objectLinks,
            Map<Integer, Version> supportedObjects, Set<LwM2mPath> availableInstances) {
        for (Link link : objectLinks) {
            if (link instanceof MixedLwM2mLink) {
                LwM2mPath path = ((MixedLwM2mLink) link).getPath();
                // add supported objects
                if (path.isObject()) {
                    addSupportedObject(lwM2mVersion, link, path, supportedObjects);
                } else if (path.isObjectInstance()) {
                    addSupportedObject(lwM2mVersion, link, path, supportedObjects);
                    availableInstances.add(path);
                }
            }
        }
    }

    protected void addSupportedObject(LwM2mVersion lwM2mVersion, Link link, LwM2mPath path,
            Map<Integer, Version> supportedObjects) {
        // extract object id and version
        int objectId = path.getObjectId();
        LwM2mAttribute<Version> versionParamValue = link.getAttributes().get(LwM2mAttributes.OBJECT_VERSION);

        if (versionParamValue != null) {
            // if there is a version attribute then use it as version for this object
            supportedObjects.put(objectId, versionParamValue.getValue());
        } else {
            // there is no version attribute attached.
            // In this case we use the DEFAULT_VERSION only if this object stored as supported object.
            Version currentVersion = supportedObjects.get(objectId);
            if (currentVersion == null) {
                supportedObjects.put(objectId, getDefaultVersion(lwM2mVersion, objectId));
            }

        }
    }

    protected Version getDefaultVersion(LwM2mVersion lwM2mVersion, int objectId) {
        // Implements : https://github.com/eclipse/leshan/issues/1434 behavior

        Version defaultVersion = versionRegistry.getDefaultVersion(objectId, lwM2mVersion);
        if (defaultVersion != null) {
            return defaultVersion;
        } else {
            // this is not a core object, use v1.0
            return Version.V1_0;
        }
    }
}
