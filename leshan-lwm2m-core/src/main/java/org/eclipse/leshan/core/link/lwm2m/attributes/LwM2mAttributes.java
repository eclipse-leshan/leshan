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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mPath;

public final class LwM2mAttributes {

    private LwM2mAttributes() {
    }

    // dim
    public static final LwM2mAttributeModel<Long> DIMENSION = new PositiveLongAttributeModel(//
            "dim", //
            EnumSet.of(Attachment.RESOURCE), //
            AccessMode.R, //
            AttributeClass.PROPERTIES) {
        @Override
        public String getInvalidValueCause(Long value) {
            if (value < 0 || value > 255) {
                return "'Dimension' attribute value must be between [0-255]";
            }
            return null;
        }

        @Override
        public String getApplicabilityError(LwM2mPath path, ObjectModel model) {
            String error = super.getApplicabilityError(path, model);
            if (error != null)
                return error;

            if (model != null) {
                // here the path should be a resource path one.
                ResourceModel resourceModel = model.resources.get(path.getResourceId());
                if (resourceModel != null && !resourceModel.multiple) {
                    return "'Dimension' attribute is only applicable to multi-Instance resource";
                }
            }
            return null;
        }
    };
    // ssid
    public static final LwM2mAttributeModel<Long> SHORT_SERVER_ID = new PositiveLongAttributeModel(//
            "ssid", //
            EnumSet.of(Attachment.OBJECT_INSTANCE), //
            AccessMode.R, //
            AttributeClass.PROPERTIES) {
        @Override
        public String getInvalidValueCause(Long value) {
            if (value < 1 || value > 65534) {
                return "'Short Server ID' attribute value must be between [1-65534]";
            }
            return null;
        }

        @Override
        public String getApplicabilityError(LwM2mPath path, ObjectModel model) {
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
        }
    };
    // uri
    public static final LwM2mAttributeModel<String> SERVER_URI = new StringAttributeModel(//
            "uri", //
            EnumSet.of(Attachment.OBJECT_INSTANCE), //
            AccessMode.R, //
            AttributeClass.PROPERTIES) {

        @Override
        public String getApplicabilityError(LwM2mPath path, ObjectModel model) {
            String error = super.getApplicabilityError(path, model);
            if (error != null)
                return error;

            // here the path should be a object instance.
            if (path.getObjectId() != LwM2mId.SECURITY) {
                return "'Server URI' attribute is only applicable to Security(ID:0)";
            }
            return null;
        }
    };
    // ver
    public static final LwM2mAttributeModel<Version> OBJECT_VERSION = new ObjectVersionAttributeModel();
    // lwm2m
    public static final LwM2mAttributeModel<LwM2mVersion> ENABLER_VERSION = new LwM2mVersionAttributeModel();
    // pmin
    // TODO : wait for confirmation before to move to PositiveDouble
    // See : https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/563
    public static final LwM2mAttributeModel<Long> MINIMUM_PERIOD = new PositiveLongAttributeModel(//
            "pmin", //
            EnumSet.of(Attachment.OBJECT, Attachment.OBJECT_INSTANCE, Attachment.RESOURCE, Attachment.RESOURCE_INTANCE), //
            AccessMode.RW, //
            AttributeClass.NOTIFICATION) {
        @Override
        public String getApplicabilityError(LwM2mPath path, ObjectModel model) {
            String error = super.getApplicabilityError(path, model);
            if (error != null)
                return error;

            if (model != null) {
                Integer resourceId = path.getResourceId();
                if (resourceId != null) {
                    // if assigned to at least resource level.
                    ResourceModel resourceModel = model.resources.get(path.getResourceId());
                    if (resourceModel != null && !resourceModel.operations.isReadable()) {
                        return "'pmin' attribute  can not be applied to not readable resource";
                    }
                }
            }
            return null;
        }
    };
    // pmax
    // TODO : wait for confirmation before to move to PositiveDouble
    // See : https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/563
    public static final LwM2mAttributeModel<Long> MAXIMUM_PERIOD = new PositiveLongAttributeModel( //
            "pmax", //
            EnumSet.of(Attachment.OBJECT, Attachment.OBJECT_INSTANCE, Attachment.RESOURCE, Attachment.RESOURCE_INTANCE), //
            AccessMode.RW, //
            AttributeClass.NOTIFICATION) {
        @Override
        public String getApplicabilityError(LwM2mPath path, ObjectModel model) {
            String error = super.getApplicabilityError(path, model);
            if (error != null)
                return error;

            if (model != null) {
                Integer resourceId = path.getResourceId();
                if (resourceId != null) {
                    // if assigned to at least resource level.
                    ResourceModel resourceModel = model.resources.get(path.getResourceId());
                    if (resourceModel != null && !resourceModel.operations.isReadable()) {
                        return "'pmax' attribute can not be applied to not readable resource";
                    }
                }
            }
            return null;
        }
    };
    // gt
    // LWM2M v1.1.1 doesn't allow negative value but this is a bug in the specification
    // See : https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/563
    public static final LwM2mAttributeModel<BigDecimal> GREATER_THAN = new BigDecimalAttributeModel(//
            "gt", //
            EnumSet.of(Attachment.RESOURCE, Attachment.RESOURCE_INTANCE), //
            AccessMode.RW, //
            AttributeClass.NOTIFICATION) {
        @Override
        public String getApplicabilityError(LwM2mPath path, ObjectModel model) {
            String error = super.getApplicabilityError(path, model);
            if (error != null)
                return error;

            if (model != null) {
                Integer resourceId = path.getResourceId();
                if (resourceId != null) {
                    // if assigned to at least resource level.
                    ResourceModel resourceModel = model.resources.get(path.getResourceId());
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
        }
    };
    // lt
    // LWM2M v1.1.1 doesn't allow negative value but this is a bug in the specification
    // See : https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/563
    public static final LwM2mAttributeModel<BigDecimal> LESSER_THAN = new BigDecimalAttributeModel( //
            "lt", //
            EnumSet.of(Attachment.RESOURCE, Attachment.RESOURCE_INTANCE), //
            AccessMode.RW, //
            AttributeClass.NOTIFICATION) {
        @Override
        public String getApplicabilityError(LwM2mPath path, ObjectModel model) {
            String error = super.getApplicabilityError(path, model);
            if (error != null)
                return error;

            if (model != null) {
                Integer resourceId = path.getResourceId();
                if (resourceId != null) {
                    // if assigned to at least resource level.
                    ResourceModel resourceModel = model.resources.get(path.getResourceId());
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
        }
    };
    // st
    public static final LwM2mAttributeModel<BigDecimal> STEP = new PositiveBigDecimalAttributeModel(//
            "st", //
            EnumSet.of(Attachment.RESOURCE, Attachment.RESOURCE_INTANCE), //
            AccessMode.RW, //
            AttributeClass.NOTIFICATION) {
        @Override
        public String getApplicabilityError(LwM2mPath path, ObjectModel model) {
            String error = super.getApplicabilityError(path, model);
            if (error != null)
                return error;

            if (model != null) {
                Integer resourceId = path.getResourceId();
                if (resourceId != null) {
                    // if assigned to at least resource level.
                    ResourceModel resourceModel = model.resources.get(path.getResourceId());
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
        }
    };
    // epmin
    // TODO : wait for confirmation before to move to PositiveDouble
    // See : https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/563
    public static final LwM2mAttributeModel<Long> EVALUATE_MINIMUM_PERIOD = new PositiveLongAttributeModel(//
            "epmin", //
            EnumSet.of(Attachment.OBJECT, Attachment.OBJECT_INSTANCE, Attachment.RESOURCE, Attachment.RESOURCE_INTANCE), //
            AccessMode.RW, //
            AttributeClass.NOTIFICATION) {
        @Override
        public String getApplicabilityError(LwM2mPath path, ObjectModel model) {
            String error = super.getApplicabilityError(path, model);
            if (error != null)
                return error;

            if (model != null) {
                Integer resourceId = path.getResourceId();
                if (resourceId != null) {
                    // if assigned to at least resource level.
                    ResourceModel resourceModel = model.resources.get(path.getResourceId());
                    if (resourceModel != null && !resourceModel.operations.isReadable()) {
                        return "'epmin' attribute is can not be applied to not readable resource";
                    }
                }
            }
            return null;
        }
    };
    // epmax
    // TODO : wait for confirmation before to move to PositiveDouble
    // See : https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/563
    public static final LwM2mAttributeModel<Long> EVALUATE_MAXIMUM_PERIOD = new PositiveLongAttributeModel( //
            "epmax", //
            EnumSet.of(Attachment.OBJECT, Attachment.OBJECT_INSTANCE, Attachment.RESOURCE, Attachment.RESOURCE_INTANCE), //
            AccessMode.RW, //
            AttributeClass.NOTIFICATION) {
        @Override
        public String getApplicabilityError(LwM2mPath path, ObjectModel model) {
            String error = super.getApplicabilityError(path, model);
            if (error != null)
                return error;

            if (model != null) {
                Integer resourceId = path.getResourceId();
                if (resourceId != null) {
                    // if assigned to at least resource level.
                    ResourceModel resourceModel = model.resources.get(path.getResourceId());
                    if (resourceModel != null && !resourceModel.operations.isReadable()) {
                        return "'epmax' attribute is can not be applied to not readable resource";
                    }
                }
            }
            return null;
        }
    };

    /**
     * All LWM2M attributes known by Leshan.
     */
    public static final Collection<LwM2mAttributeModel<?>> ALL = Collections.unmodifiableList(
            Arrays.asList(DIMENSION, SHORT_SERVER_ID, SERVER_URI, OBJECT_VERSION, ENABLER_VERSION, MINIMUM_PERIOD,
                    MAXIMUM_PERIOD, GREATER_THAN, LESSER_THAN, STEP, EVALUATE_MINIMUM_PERIOD, EVALUATE_MAXIMUM_PERIOD));

    public static <T> LwM2mAttribute<T> create(LwM2mAttributeModel<T> model, T value) {
        return new LwM2mAttribute<>(model, value);
    }

    public static LwM2mAttribute<BigDecimal> create(LwM2mAttributeModel<BigDecimal> model, double value) {
        return new LwM2mAttribute<>(model, BigDecimal.valueOf(value));
    }

    public static LwM2mAttribute<BigDecimal> create(LwM2mAttributeModel<BigDecimal> model, long value) {
        return new LwM2mAttribute<>(model, new BigDecimal(value));
    }

    public static LwM2mAttribute<BigDecimal> create(LwM2mAttributeModel<BigDecimal> model, String value) {
        return new LwM2mAttribute<>(model, new BigDecimal(value));
    }

    public static <T> LwM2mAttribute<T> create(LwM2mAttributeModel<T> model) {
        try {
            return model.createEmptyAttribute();
        } catch (UnsupportedOperationException e) {
            throw new IllegalArgumentException(String.format("Attribute %s must have a value", model.getName()));
        }
    }
}
