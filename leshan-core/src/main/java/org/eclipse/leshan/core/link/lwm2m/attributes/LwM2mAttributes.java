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

import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.link.attributes.InvalidAttributeException;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mPath;

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
                return "'Dimension' attribute value must be between [0-255]";
            }
            return null;
        };

        @Override
        public String getApplicabilityError(LwM2mPath path, LwM2mModel model) {
            String error = super.getApplicabilityError(path, model);
            if (error != null)
                return error;

            if (model != null) {
                // here the path should be a resource path one.
                ResourceModel resourceModel = model.getResourceModel(path.getObjectId(), path.getResourceId());
                if (resourceModel != null && !resourceModel.multiple) {
                    return "'Dimension' attribute is only applicable to multi-Instance resource";
                }
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
                return "'Short Server ID' attribute value must be between [1-65534]";
            }
            return null;
        };

        @Override
        public String getApplicabilityError(LwM2mPath path, LwM2mModel model) {
            String error = super.getApplicabilityError(path, model);
            if (error != null)
                return error;

            // here the path should be a object instance.
            if (path.getObjectId() != LwM2mId.SECURITY//
                    && path.getObjectId() != LwM2mId.SERVER) {
                // the LWM2M v1.1 specification says not that it is applicable to SERVER.
                // See :
                // http://www.openmobilealliance.org/release/LightweightM2M/V1_1_1-20190617-A/HTML-Version/OMA-TS-LightweightM2M_Core-V1_1_1-20190617-A.html#5-1-2-0-512-Attributes-Classification)
                // But :
                // http://www.openmobilealliance.org/release/LightweightM2M/V1_1_1-20190617-A/HTML-Version/OMA-TS-LightweightM2M_Core-V1_1_1-20190617-A.html#6-1-7-3-0-6173-Bootstrap-Discover-Operation
                // says the opposite.
                return "'Short Server ID' attribute is only applicable to Security (ID:0), Server(ID:1) object.";
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
            AttributeClass.PROPERTIES) {

        @Override
        public String getApplicabilityError(LwM2mPath path, LwM2mModel model) {
            String error = super.getApplicabilityError(path, model);
            if (error != null)
                return error;

            // here the path should be a object instance.
            if (path.getObjectId() != LwM2mId.SECURITY) {
                return "'Server URI' attribute is only applicable to Security(ID:0)";
            }
            return null;
        };
    };
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
            AttributeClass.NOTIFICATION) {
        @Override
        public String getApplicabilityError(LwM2mPath path, LwM2mModel model) {
            String error = super.getApplicabilityError(path, model);
            if (error != null)
                return error;

            if (model != null) {
                Integer resourceId = path.getResourceId();
                if (resourceId != null) {
                    // if assigned to at least resource level.
                    ResourceModel resourceModel = model.getResourceModel(path.getObjectId(), path.getResourceId());
                    if (resourceModel != null && !resourceModel.operations.isReadable()) {
                        return "'pmin' attribute  can not be applied to not readable resource";
                    }
                }
            }
            return null;
        };
    };
    // pmax
    public static final LongAttributeModel MAXIMUM_PERIOD = new LongAttributeModel( //
            "pmax", //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.OBJECT, AssignationLevel.OBJECT_INSTANCE, AssignationLevel.RESOURCE), //
            AccessMode.RW, //
            AttributeClass.NOTIFICATION) {
        @Override
        public String getApplicabilityError(LwM2mPath path, LwM2mModel model) {
            String error = super.getApplicabilityError(path, model);
            if (error != null)
                return error;

            if (model != null) {
                Integer resourceId = path.getResourceId();
                if (resourceId != null) {
                    // if assigned to at least resource level.
                    ResourceModel resourceModel = model.getResourceModel(path.getObjectId(), path.getResourceId());
                    if (resourceModel != null && !resourceModel.operations.isReadable()) {
                        return "'pmax' attribute can not be applied to not readable resource";
                    }
                }
            }
            return null;
        };
    };
    // gt
    public static final LwM2mAttributeModel<Double> GREATER_THAN = new DoubleAttributeModel(//
            "gt", //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.RESOURCE), //
            AccessMode.RW, //
            AttributeClass.NOTIFICATION) {
        @Override
        public String getApplicabilityError(LwM2mPath path, LwM2mModel model) {
            String error = super.getApplicabilityError(path, model);
            if (error != null)
                return error;

            if (model != null) {
                Integer resourceId = path.getResourceId();
                if (resourceId != null) {
                    // if assigned to at least resource level.
                    ResourceModel resourceModel = model.getResourceModel(path.getObjectId(), path.getResourceId());
                    if (resourceModel != null) {
                        if (!resourceModel.operations.isReadable()) {
                            return "'gt' attribute is can not be applied to not readable resource";
                        }
                        if (!resourceModel.type.isNumeric()) {
                            return "'gt' attribute is can not be applied to not numeric resource";
                        }
                    }
                }
            }
            return null;
        };
    };
    // lt
    public static final LwM2mAttributeModel<Double> LESSER_THAN = new DoubleAttributeModel( //
            "lt", //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.RESOURCE), //
            AccessMode.RW, //
            AttributeClass.NOTIFICATION) {
        @Override
        public String getApplicabilityError(LwM2mPath path, LwM2mModel model) {
            String error = super.getApplicabilityError(path, model);
            if (error != null)
                return error;

            if (model != null) {
                Integer resourceId = path.getResourceId();
                if (resourceId != null) {
                    // if assigned to at least resource level.
                    ResourceModel resourceModel = model.getResourceModel(path.getObjectId(), path.getResourceId());
                    if (resourceModel != null) {
                        if (!resourceModel.operations.isReadable()) {
                            return "'lt' attribute is can not be applied to not readable resource";
                        }
                        if (!resourceModel.type.isNumeric()) {
                            return "'lt' attribute is can not be applied to not numeric resource";
                        }
                    }
                }
            }
            return null;
        };
    };
    // st
    public static final LwM2mAttributeModel<Double> STEP = new DoubleAttributeModel(//
            "st", //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.RESOURCE), //
            AccessMode.RW, //
            AttributeClass.NOTIFICATION) {
        @Override
        public String getApplicabilityError(LwM2mPath path, LwM2mModel model) {
            String error = super.getApplicabilityError(path, model);
            if (error != null)
                return error;

            if (model != null) {
                Integer resourceId = path.getResourceId();
                if (resourceId != null) {
                    // if assigned to at least resource level.
                    ResourceModel resourceModel = model.getResourceModel(path.getObjectId(), path.getResourceId());
                    if (resourceModel != null) {
                        if (!resourceModel.operations.isReadable()) {
                            return "'st' attribute is can not be applied to not readable resource";
                        }
                        if (!resourceModel.type.isNumeric()) {
                            return "'st' attribute is can not be applied to not numeric resource";
                        }
                    }
                }
            }
            return null;
        };
    };
    // epmin
    public static final LwM2mAttributeModel<Long> EVALUATE_MINIMUM_PERIOD = new LongAttributeModel(//
            "epmin", //
            Attachment.RESOURCE, //
            EnumSet.of(AssignationLevel.OBJECT, AssignationLevel.OBJECT_INSTANCE, AssignationLevel.RESOURCE), //
            AccessMode.RW, //
            AttributeClass.NOTIFICATION) {
        @Override
        public String getApplicabilityError(LwM2mPath path, LwM2mModel model) {
            String error = super.getApplicabilityError(path, model);
            if (error != null)
                return error;

            if (model != null) {
                Integer resourceId = path.getResourceId();
                if (resourceId != null) {
                    // if assigned to at least resource level.
                    ResourceModel resourceModel = model.getResourceModel(path.getObjectId(), path.getResourceId());
                    if (resourceModel != null && !resourceModel.operations.isReadable()) {
                        return "'epmin' attribute is can not be applied to not readable resource";
                    }
                }
            }
            return null;
        };
    };
    // epmax
    public static final LwM2mAttributeModel<Long> EVALUATE_MAXIMUM_PERIOD = new LongAttributeModel( //
            "epmax", //
            Attachment.RESOURCE,
            EnumSet.of(AssignationLevel.OBJECT, AssignationLevel.OBJECT_INSTANCE, AssignationLevel.RESOURCE), //
            AccessMode.RW, //
            AttributeClass.NOTIFICATION) {
        @Override
        public String getApplicabilityError(LwM2mPath path, LwM2mModel model) {
            String error = super.getApplicabilityError(path, model);
            if (error != null)
                return error;

            if (model != null) {
                Integer resourceId = path.getResourceId();
                if (resourceId != null) {
                    // if assigned to at least resource level.
                    ResourceModel resourceModel = model.getResourceModel(path.getObjectId(), path.getResourceId());
                    if (resourceModel != null && !resourceModel.operations.isReadable()) {
                        return "'epmax' attribute is can not be applied to not readable resource";
                    }
                }
            }
            return null;
        };
    };
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
