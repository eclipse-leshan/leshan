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

import org.w3c.dom.Node;

/**
 * A DDF File Validator.
 * <p>
 * Validate a DDF File against a LWM2M.xsd schema.
 * 
 * @since 1.1
 */
public interface DDFFileValidator {

    /**
     * Validate a DOM node (could be DOM Document) against the LWM2M.xsd Schema.
     * 
     * @param xmlToValidate DOM node to validate
     * @throws InvalidDDFFileException if ddf file is invalid.
     */
    public void validate(Node xmlToValidate) throws InvalidDDFFileException;
}
