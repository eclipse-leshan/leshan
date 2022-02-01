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
package org.eclipse.leshan.core.attributes;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.leshan.core.attributes.model.DoubleAttributeModel;
import org.eclipse.leshan.core.attributes.model.LongAttributeModel;
import org.eclipse.leshan.core.attributes.model.LwM2mVersionAttributeModel;
import org.eclipse.leshan.core.attributes.model.ObjectVersionAttributeModel;
import org.eclipse.leshan.core.attributes.model.StringAttributeModel;
import org.eclipse.leshan.core.link.attributes.AttributeModel;
import org.eclipse.leshan.core.link.attributes.InvalidAttributeException;

/**
 * Metadata container for LwM2m attributes
 */
public abstract class LwM2mAttributeModel<T> extends AttributeModel<LwM2mAttribute<T>> {

    // TODO this constant should be deleted replaced by the ones below
    public static final String DIMENSION = "dim";
    public static final String SHORT_SERVER_ID = "ssid";
    public static final String SERVER_URI = "uri";
    public static final String ENABLER_VERSION = "lwm2m";
    public static final String OBJECT_VERSION = "ver";
    public static final String MINIMUM_PERIOD = "pmin";
    public static final String MAXIMUM_PERIOD = "pmax";
    public static final String GREATER_THAN = "gt";
    public static final String LESSER_THAN = "lt";
    public static final String STEP = "st";
    public static final String EVALUATE_MINIMUM_PERIOD = "epmin";
    public static final String EVALUATE_MAXIMUM_PERIOD = "epmax";

    // TODO we should reuse the name above waiting, I suffix by _ATTR
    // dim
    public static final LongAttributeModel DIMENSION_ATTR = new LongAttributeModel(//
            DIMENSION, //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.RESOURCE), //
            AccessMode.R);
    // ssid
    public static final LongAttributeModel SHORT_SERVER_ID_ATTR = new LongAttributeModel(//
            SHORT_SERVER_ID, //
            Attachment.INSTANCE, //
            EnumSet.of(AssignationLevel.INSTANCE), //
            AccessMode.R);
    // uri
    public static final StringAttributeModel SERVER_URI_ATTR = new StringAttributeModel(//
            SERVER_URI, //
            Attachment.INSTANCE, //
            EnumSet.of(AssignationLevel.INSTANCE), //
            AccessMode.R);
    // ver
    public static final ObjectVersionAttributeModel OBJECT_VERSION_ATTR = new ObjectVersionAttributeModel();
    // lwm2m
    public static final LwM2mVersionAttributeModel ENABLER_VERSION_ATTR = new LwM2mVersionAttributeModel();
    // pmin
    public static final LwM2mAttributeModel<Long> MINIMUM_PERIOD_ATTR = new LongAttributeModel(MINIMUM_PERIOD, //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.OBJECT, AssignationLevel.INSTANCE, AssignationLevel.RESOURCE), //
            AccessMode.RW);
    // pmax
    public static final LongAttributeModel MAXIMUM_PERIOD_ATTR = new LongAttributeModel( //
            MAXIMUM_PERIOD, //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.OBJECT, AssignationLevel.INSTANCE, AssignationLevel.RESOURCE), //
            AccessMode.RW);
    // gt
    public static final LwM2mAttributeModel<Double> GREATER_THAN_ATTR = new DoubleAttributeModel(//
            GREATER_THAN, //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.RESOURCE), //
            AccessMode.RW);
    // lt
    public static final LwM2mAttributeModel<Double> LESSER_THAN_ATTR = new DoubleAttributeModel( //
            LESSER_THAN, //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.RESOURCE), //
            AccessMode.RW);
    // st
    public static final LwM2mAttributeModel<Double> STEP_ATTR = new DoubleAttributeModel(//
            STEP, //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.RESOURCE), //
            AccessMode.RW);
    // epmin
    public static final LwM2mAttributeModel<Long> EVALUATE_MINIMUM_PERIOD_ATTR = new LongAttributeModel(//
            EVALUATE_MINIMUM_PERIOD, //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.OBJECT, AssignationLevel.INSTANCE, AssignationLevel.RESOURCE), //
            AccessMode.RW);
    // epmax
    public static final LwM2mAttributeModel<Long> EVALUATE_MAXIMUM_PERIOD_ATTR = new LongAttributeModel( //
            EVALUATE_MAXIMUM_PERIOD, //
            Attachment.RESOURCE,
            EnumSet.of(AssignationLevel.OBJECT, AssignationLevel.INSTANCE, AssignationLevel.RESOURCE), //
            AccessMode.RW);

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

    public static Map<String, LwM2mAttributeModel<?>> modelMap;

    static {
        modelMap = new HashMap<>();
        modelMap.put(DIMENSION, DIMENSION_ATTR);
        modelMap.put(OBJECT_VERSION, OBJECT_VERSION_ATTR);
        modelMap.put(MINIMUM_PERIOD, MINIMUM_PERIOD_ATTR);
        modelMap.put(MAXIMUM_PERIOD, MAXIMUM_PERIOD_ATTR);
        modelMap.put(GREATER_THAN, GREATER_THAN_ATTR);
        modelMap.put(LESSER_THAN, LESSER_THAN_ATTR);
        modelMap.put(STEP, STEP_ATTR);
        modelMap.put(EVALUATE_MINIMUM_PERIOD, EVALUATE_MINIMUM_PERIOD_ATTR);
        modelMap.put(EVALUATE_MAXIMUM_PERIOD, EVALUATE_MAXIMUM_PERIOD_ATTR);
    }
}