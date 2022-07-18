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
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mPath;

/**
 * An {@link AttributeModel} for a {@link LwM2mAttribute}.
 * <p>
 * Constants models are available at {@link LwM2mAttributes}
 *
 * @see LwM2mAttributes
 */
public abstract class LwM2mAttributeModel<T> extends AttributeModel<LwM2mAttribute<T>> {

    private final Attachment attachment;
    private final Set<AssignationLevel> assignationLevels;
    private final AccessMode accessMode;
    private final AttributeClass attributeClass;

    protected LwM2mAttributeModel(String coRELinkParam, Attachment attachment, Set<AssignationLevel> assignationLevels,
            AccessMode accessMode, AttributeClass attributeClass) {
        super(coRELinkParam);
        this.attachment = attachment;
        this.assignationLevels = assignationLevels;
        this.accessMode = accessMode;
        this.attributeClass = attributeClass;
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

    public AttributeClass getAttributeClass() {
        return attributeClass;
    }

    /**
     * @return <code>true</code> if the Attribute can be used without value in a query param Format.
     */
    public boolean queryParamCanBeValueless() throws InvalidAttributeException {
        // AFAIK, only writable value can be null. (mainly to remove write attributes)
        return accessMode.isWritable() && attributeClass == AttributeClass.NOTIFICATION;
    }

    /**
     * @return null is the value is valid, else an error message about the cause
     */
    public String getInvalidValueCause(T value) {
        // do nothing by default
        return null;
    }

    /**
     * return true if the attribute can be assigned to the given assignation level.
     */
    public boolean canBeAssignedTo(AssignationLevel assignation) {
        return getAssignationLevels().contains(assignation);
    }

    /**
     * @param path the LWM2M path to which this attribute is applied
     * @param model the LWM2M model used, if this model is null checks which need model will be ignored.
     * @return null is the attribute can be applied to the LWM2M node identified by the given path.
     */
    public String getApplicabilityError(LwM2mPath path, LwM2mModel model) {
        if (!canBeAssignedTo(AssignationLevel.fromPath(path))) {
            return String.format("%s attribute is only applicable to %s, and so can not be assigned to %s", getName(),
                    getAssignationLevels(), path);
        }
        return null;
    }
}
