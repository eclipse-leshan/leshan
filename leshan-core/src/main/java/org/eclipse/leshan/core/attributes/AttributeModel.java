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
public class AttributeModel {

    public static final String DIMENSION = "dim";
    public static final String OBJECT_VERSION = "ver";
    public static final String MINIMUM_PERIOD = "pmin";
    public static final String MAXIMUM_PERIOD = "pmax";
    public static final String GREATER_THAN = "gt";
    public static final String LESSER_THAN = "lt";
    public static final String STEP = "st";
    public static final String EVALUATE_MINIMUM_PERIOD = "epmin";
    public static final String EVALUATE_MAXIMUM_PERIOD = "epmax";

    final String coRELinkParam;
    final Attachment attachment;
    final Set<AssignationLevel> assignationLevels;
    final AccessMode accessMode;
    final Class<?> valueClass;

    AttributeModel(String coRELinkParam, Attachment attachment, Set<AssignationLevel> assignationLevels,
            AccessMode accessMode, Class<?> valueClass) {
        this.coRELinkParam = coRELinkParam;
        this.attachment = attachment;
        this.assignationLevels = assignationLevels;
        this.accessMode = accessMode;
        this.valueClass = valueClass;
    }

    public static Map<String, AttributeModel> modelMap;

    static {
        modelMap = new HashMap<>();
        modelMap.put(DIMENSION, new AttributeModel(DIMENSION, Attachment.RESOURCE,
                EnumSet.of(AssignationLevel.RESOURCE), AccessMode.R, Long.class));
        modelMap.put(OBJECT_VERSION, new AttributeModel(OBJECT_VERSION, Attachment.OBJECT,
                EnumSet.of(AssignationLevel.OBJECT), AccessMode.R, String.class));
        modelMap.put(MINIMUM_PERIOD,
                new AttributeModel(MINIMUM_PERIOD, Attachment.RESOURCE,
                        EnumSet.of(AssignationLevel.OBJECT, AssignationLevel.INSTANCE, AssignationLevel.RESOURCE),
                        AccessMode.RW, Long.class));
        modelMap.put(MAXIMUM_PERIOD,
                new AttributeModel(MAXIMUM_PERIOD, Attachment.RESOURCE,
                        EnumSet.of(AssignationLevel.OBJECT, AssignationLevel.INSTANCE, AssignationLevel.RESOURCE),
                        AccessMode.RW, Long.class));
        modelMap.put(GREATER_THAN, new AttributeModel(GREATER_THAN, Attachment.RESOURCE,
                EnumSet.of(AssignationLevel.RESOURCE), AccessMode.RW, Double.class));
        modelMap.put(LESSER_THAN, new AttributeModel(LESSER_THAN, Attachment.RESOURCE,
                EnumSet.of(AssignationLevel.RESOURCE), AccessMode.RW, Double.class));
        modelMap.put(STEP, new AttributeModel(STEP, Attachment.RESOURCE, EnumSet.of(AssignationLevel.RESOURCE),
                AccessMode.RW, Double.class));
        modelMap.put(EVALUATE_MINIMUM_PERIOD,
                new AttributeModel(EVALUATE_MINIMUM_PERIOD, Attachment.RESOURCE,
                        EnumSet.of(AssignationLevel.OBJECT, AssignationLevel.INSTANCE, AssignationLevel.RESOURCE),
                        AccessMode.RW, Long.class));
        modelMap.put(EVALUATE_MAXIMUM_PERIOD,
                new AttributeModel(EVALUATE_MAXIMUM_PERIOD, Attachment.RESOURCE,
                        EnumSet.of(AssignationLevel.OBJECT, AssignationLevel.INSTANCE, AssignationLevel.RESOURCE),
                        AccessMode.RW, Long.class));
    }
}