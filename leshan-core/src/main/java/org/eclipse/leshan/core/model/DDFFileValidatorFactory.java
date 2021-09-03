/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.core.model;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.w3c.dom.Document;

/**
 * Factory responsible to create {@link DDFFileValidator} for a given version of the specification.
 */
public interface DDFFileValidatorFactory {

    /**
     * Extract LWM2M version from DDF file
     * 
     * @param document the DDF file.
     * @param documentName the DDF file name for error raising or logging purpose.
     * 
     * @return the version of the minimal LWM2M version supported by this DDF file.
     */
    LwM2mVersion extractLWM2MVersion(Document document, String documentName) throws InvalidDDFFileException;

    /**
     * Create a {@link DDFFileValidator} for the given LWM2M {@link LwM2mVersion}
     */
    DDFFileValidator create(LwM2mVersion lwm2mVersion);
}
