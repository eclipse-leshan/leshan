/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;

import org.eclipse.leshan.core.link.attributes.InvalidAttributeException;

public final class LwM2mAttributes {

    // dim
    public static final LongAttributeModel DIMENSION = new LongAttributeModel(//
            "dim", //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.RESOURCE), //
            AccessMode.R, //
            AttributeClass.PROPERTIES) {
        @Override
        public String getInvalidValueCause(Long value) {
            if (value < 0 || value > 255) {
                return "Dimension attribute value must be between [0-255]";
            }
            return null;
        };
    };
    // ssid
    public static final LongAttributeModel SHORT_SERVER_ID = new LongAttributeModel(//
            "ssid", //
            Attachment.OBJECT_INSTANCE, //
            EnumSet.of(AssignationLevel.OBJECT_INSTANCE), //
            AccessMode.R, //
            AttributeClass.PROPERTIES) {
        @Override
        public String getInvalidValueCause(Long value) {
            if (value < 1 || value > 65534) {
                return "Short Server ID attribute value must be between [1-65534]";
            }
            return null;
        };
    };
    // uri
    public static final StringAttributeModel SERVER_URI = new StringAttributeModel(//
            "uri", //
            Attachment.OBJECT_INSTANCE, //
            EnumSet.of(AssignationLevel.OBJECT_INSTANCE), //
            AccessMode.R, //
            AttributeClass.PROPERTIES);
    // ver
    public static final ObjectVersionAttributeModel OBJECT_VERSION = new ObjectVersionAttributeModel();
    // lwm2m
    public static final LwM2mVersionAttributeModel ENABLER_VERSION = new LwM2mVersionAttributeModel();
    // pmin
    public static final LwM2mAttributeModel<Long> MINIMUM_PERIOD = new LongAttributeModel(//
            "pmin", //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.OBJECT, AssignationLevel.OBJECT_INSTANCE, AssignationLevel.RESOURCE), //
            AccessMode.RW, //
            AttributeClass.NOTIFICATION);
    // pmax
    public static final LongAttributeModel MAXIMUM_PERIOD = new LongAttributeModel( //
            "pmax", //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.OBJECT, AssignationLevel.OBJECT_INSTANCE, AssignationLevel.RESOURCE), //
            AccessMode.RW, //
            AttributeClass.NOTIFICATION);
    // gt
    public static final LwM2mAttributeModel<Double> GREATER_THAN = new DoubleAttributeModel(//
            "gt", //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.RESOURCE), //
            AccessMode.RW, //
            AttributeClass.NOTIFICATION);
    // lt
    public static final LwM2mAttributeModel<Double> LESSER_THAN = new DoubleAttributeModel( //
            "lt", //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.RESOURCE), //
            AccessMode.RW, //
            AttributeClass.NOTIFICATION);
    // st
    public static final LwM2mAttributeModel<Double> STEP = new DoubleAttributeModel(//
            "st", //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.RESOURCE), //
            AccessMode.RW, //
            AttributeClass.NOTIFICATION);
    // epmin
    public static final LwM2mAttributeModel<Long> EVALUATE_MINIMUM_PERIOD = new LongAttributeModel(//
            "epmin", //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.OBJECT, AssignationLevel.OBJECT_INSTANCE, AssignationLevel.RESOURCE), //
            AccessMode.RW, //
            AttributeClass.NOTIFICATION);
    // epmax
    public static final LwM2mAttributeModel<Long> EVALUATE_MAXIMUM_PERIOD = new LongAttributeModel( //
            "epmax", //
            Attachment.RESOURCE,
            EnumSet.of(AssignationLevel.OBJECT, AssignationLevel.OBJECT_INSTANCE, AssignationLevel.RESOURCE), //
            AccessMode.RW, //
            AttributeClass.NOTIFICATION);
    public static Map<String, LwM2mAttributeModel<?>> modelMap;

    /**
     * All LWM2M attributes known by Leshan.
     */
    public static final Collection<LwM2mAttributeModel<?>> ALL = Arrays.asList(DIMENSION, SHORT_SERVER_ID, SERVER_URI,
            OBJECT_VERSION, ENABLER_VERSION, MINIMUM_PERIOD, MAXIMUM_PERIOD, GREATER_THAN, LESSER_THAN, STEP,
            EVALUATE_MINIMUM_PERIOD, EVALUATE_MAXIMUM_PERIOD);

    public static <T> LwM2mAttribute<T> create(LwM2mAttributeModel<T> model, T value) {
        return new LwM2mAttribute<>(model, value);
    }

    public static <T> LwM2mAttribute<T> create(LwM2mAttributeModel<T> model) throws InvalidAttributeException {
        return model.createEmptyAttribute();
    }
}
