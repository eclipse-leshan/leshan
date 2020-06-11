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

import java.util.List;

/**
 * Validate an LWM2M Object or Resource Model.
 * 
 * @since 1.1
 */
public interface ObjectModelValidator {

    /**
     * Validate a list of {@link ObjectModel}.
     * 
     * @param models the list of {@link ObjectModel} to validate
     * @param modelName a hint about where the object come from to make debug easier. e.g a filename if model was store
     *        in a file.
     * @throws InvalidModelException is raised when an {@link ObjectModel} is Invalid
     */
    public void validate(List<ObjectModel> models, String modelName) throws InvalidModelException;
}
