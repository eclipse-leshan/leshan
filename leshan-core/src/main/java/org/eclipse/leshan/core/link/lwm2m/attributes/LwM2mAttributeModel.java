/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.core.link.lwm2m.attributes;

import java.util.Set;

import org.eclipse.leshan.core.link.attributes.AttributeModel;
import org.eclipse.leshan.core.link.attributes.InvalidAttributeException;

/**
 * Metadata container for LwM2m attributes
 */
public abstract class LwM2mAttributeModel<T> extends AttributeModel<LwM2mAttribute<T>> {

    final Attachment attachment;
    final Set<AssignationLevel> assignationLevels;
    final AccessMode accessMode;
    final Class<?> valueClass;

    protected LwM2mAttributeModel(String coRELinkParam, Attachment attachment, Set<AssignationLevel> assignationLevels,
            AccessMode accessMode, Class<T> valueClass) {
        super(coRELinkParam);
        this.attachment = attachment;
        this.assignationLevels = assignationLevels;
        this.accessMode = accessMode;
        this.valueClass = valueClass;
    }

    public String toCoreLinkValue(LwM2mAttribute<T> attr) {
        return attr.getValue().toString();
    }

    public AccessMode getAccessMode() {
        return accessMode;
    }

    public Set<AssignationLevel> getAssignationLevels() {
        return assignationLevels;
    }

    public Attachment getAttachment() {
        return attachment;
    }

    protected void canBeValueless() throws InvalidAttributeException {
        if (!accessMode.isWritable()) {
            // AFAIK, only writable value can be null. (mainly to remove write attributes)
            throw new InvalidAttributeException("Attribute %s must have a value", getName());
        }
    }
}