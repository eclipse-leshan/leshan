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

import java.util.Map;
import java.util.Set;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;

/**
 * Extract some registration Data (supported object, supported content format, alternate path) from object Links.
 */
public interface RegistrationDataExtractor {

    RegistrationData extractDataFromObjectLinks(Link[] objectLinks, LwM2mVersion lwM2mVersion);

    public class RegistrationData {
        private String alternatePath;
        private Set<ContentFormat> supportedContentFormats;
        private Map<Integer, Version> supportedObjects;
        private Set<LwM2mPath> availableInstances;

        public String getAlternatePath() {
            return alternatePath;
        }

        public void setAlternatePath(String alternatePath) {
            this.alternatePath = alternatePath;
        }

        public Set<ContentFormat> getSupportedContentFormats() {
            return supportedContentFormats;
        }

        public void setSupportedContentFormats(Set<ContentFormat> supportedContentFormats) {
            this.supportedContentFormats = supportedContentFormats;
        }

        public Map<Integer, Version> getSupportedObjects() {
            return supportedObjects;
        }

        public void setSupportedObjects(Map<Integer, Version> supportedObjects) {
            this.supportedObjects = supportedObjects;
        }

        public Set<LwM2mPath> getAvailableInstances() {
            return availableInstances;
        }

        public void setAvailableInstances(Set<LwM2mPath> availableInstances) {
            this.availableInstances = availableInstances;
        }
    }
}
