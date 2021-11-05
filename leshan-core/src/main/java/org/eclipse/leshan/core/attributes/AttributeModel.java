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

/**
 * Metadata container for LwM2m attributes
 */
public class AttributeModel<T> {

    // TODO this constant should be deleted replaced by the ones below
    public static final String DIMENSION = "dim";
    public static final String OBJECT_VERSION = "ver";
    public static final String MINIMUM_PERIOD = "pmin";
    public static final String MAXIMUM_PERIOD = "pmax";
    public static final String GREATER_THAN = "gt";
    public static final String LESSER_THAN = "lt";
    public static final String STEP = "st";
    public static final String EVALUATE_MINIMUM_PERIOD = "epmin";
    public static final String EVALUATE_MAXIMUM_PERIOD = "epmax";

    // TODO we should reuse the name above waiting, I suffix by _ATTR
    public static final AttributeModel<Long> DIMENSION_ATTR = new AttributeModel<Long>(//
            DIMENSION, //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.RESOURCE), //
            AccessMode.R, //
            Long.class);
    public static final AttributeModel<String> OBJECT_VERSION_ATTR = new AttributeModel<String>(//
            OBJECT_VERSION, //
            Attachment.OBJECT, //
            EnumSet.of(AssignationLevel.OBJECT), //
            AccessMode.R, //
            String.class);
    public static final AttributeModel<Long> MINIMUM_PERIOD_ATTR = new AttributeModel<Long>(//
            MINIMUM_PERIOD, //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.OBJECT, AssignationLevel.INSTANCE, AssignationLevel.RESOURCE), //
            AccessMode.RW, //
            Long.class);
    public static final AttributeModel<Long> MAXIMUM_PERIOD_ATTR = new AttributeModel<Long>( //
            MAXIMUM_PERIOD, //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.OBJECT, AssignationLevel.INSTANCE, AssignationLevel.RESOURCE), //
            AccessMode.RW, Long.class);
    public static final AttributeModel<Double> GREATER_THAN_ATTR = new AttributeModel<Double>(//
            GREATER_THAN, //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.RESOURCE), //
            AccessMode.RW, //
            Double.class);
    public static final AttributeModel<Double> LESSER_THAN_ATTR = new AttributeModel<Double>( //
            LESSER_THAN, //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.RESOURCE), //
            AccessMode.RW, //
            Double.class);
    public static final AttributeModel<Double> STEP_ATTR = new AttributeModel<Double>(//
            STEP, //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.RESOURCE), //
            AccessMode.RW, //
            Double.class);
    public static final AttributeModel<Long> EVALUATE_MINIMUM_PERIOD_ATTR = new AttributeModel<Long>(//
            EVALUATE_MINIMUM_PERIOD, //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.OBJECT, AssignationLevel.INSTANCE, AssignationLevel.RESOURCE), //
            AccessMode.RW, //
            Long.class);
    public static final AttributeModel<Long> EVALUATE_MAXIMUM_PERIOD_ATTR = new AttributeModel<Long>( //
            EVALUATE_MAXIMUM_PERIOD, //
            Attachment.RESOURCE,
            EnumSet.of(AssignationLevel.OBJECT, AssignationLevel.INSTANCE, AssignationLevel.RESOURCE), //
            AccessMode.RW, //
            Long.class);

    final String coRELinkParam;
    final Attachment attachment;
    final Set<AssignationLevel> assignationLevels;
    final AccessMode accessMode;
    final Class<?> valueClass;

    AttributeModel(String coRELinkParam, Attachment attachment, Set<AssignationLevel> assignationLevels,
            AccessMode accessMode, Class<T> valueClass) {
        this.coRELinkParam = coRELinkParam;
        this.attachment = attachment;
        this.assignationLevels = assignationLevels;
        this.accessMode = accessMode;
        this.valueClass = valueClass;
    }

    public static Map<String, AttributeModel<?>> modelMap;

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