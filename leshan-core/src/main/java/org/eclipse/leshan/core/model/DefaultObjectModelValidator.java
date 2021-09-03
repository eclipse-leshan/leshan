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

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.model.ResourceModel.Type;

public class DefaultObjectModelValidator implements ObjectModelValidator {

    @Override
    public void validate(List<ObjectModel> models, String modelName) throws InvalidModelException {
        for (ObjectModel model : models) {
            validate(model, modelName);
        }
    }

    /**
     * Validate a {@link ObjectModel}.
     * 
     * @param object the {@link ObjectModel} to validate
     * @param modelName a hint about where the object come from to make debug easier. e.g a filename in model was store
     *        in a file.
     * @throws InvalidModelException is raised when {@link ObjectModel} is Invalid
     */
    public void validate(ObjectModel object, String modelName) throws InvalidModelException {
        // validate name
        if (object.name == null || object.name.isEmpty()) {
            throw new InvalidModelException(
                    "Model for Object (%d) in %s is invalid : Object name MUST NOT be null or empty", object.id,
                    modelName);
        }

        // validate id
        validateModelFieldNotNull(object, modelName, object.id, "id");
        if (!(0 <= object.id && object.id <= 42768)) {
            throw new InvalidModelException(
                    "Model for Object (%d) in %s is invalid : Object id MUST be between 0 and 42768", object.id,
                    modelName);
        }
        if (1024 <= object.id && object.id <= 2047) {
            throw new InvalidModelException(
                    "Model for Object (%d) in %s is invalid : Object id is in reserved range (1024-2047)", object.id,
                    modelName);
        }

        // validate others fields
        validateModelFieldNotNull(object, modelName, object.multiple, "multiple");
        validateModelFieldNotNull(object, modelName, object.mandatory, "mandatory");
        validateVersion(object.version, object, "version", modelName);
        validateVersion(object.lwm2mVersion, object, "lwm2mVersion", modelName);
        validateURN(object.urn, object, modelName);

        // validate resources
        if (object.resources == null || object.resources.isEmpty()) {
            throw new InvalidModelException(
                    "Model for Object %s(%d) in %s is invalid : Resource lists MUST NOT be null or empty", object.name,
                    object.id, modelName);
        }
        for (ResourceModel resource : object.resources.values()) {
            validate(resource, modelName, object.lwm2mVersion);
        }
    }

    /**
     * Validate an {@link ResourceModel}
     * 
     * @param resource the {@link ResourceModel} to validate
     * @param modelName a hint about where the resource come from to make debug easier. e.g a filename in model was
     *        store in a file.
     * @param lwm2mVersion the minimal version of the specification supported by the object
     * @throws InvalidModelException is raised when {@link ResourceModel} is Invalid
     */
    public void validate(ResourceModel resource, String modelName, String lwm2mVersion) throws InvalidModelException {
        // validate name
        if (resource.name == null || resource.name.isEmpty()) {
            throw new InvalidModelException(
                    "Model for Resource (%d) in %s is invalid : resource name MUST NOT be null or empty", resource.id,
                    modelName);
        }
        // validate id
        validateResourceFieldNotNull(resource, modelName, resource.id, "id");
        if (!(0 <= resource.id && resource.id <= 32768)) {
            throw new InvalidModelException(
                    "Model for Resource (%d) in %s is invalid : Resource id MUST be between 0 and 32768", resource.id,
                    modelName);
        }

        // validate others fields
        validateResourceFieldNotNull(resource, modelName, resource.operations, "operations");
        validateResourceFieldNotNull(resource, modelName, resource.multiple, "multiple");
        validateResourceFieldNotNull(resource, modelName, resource.mandatory, "mandatory");

        // validate type
        validateResourceFieldNotNull(resource, modelName, resource.type, "type");
        if (resource.operations.isExecutable() && resource.type != Type.NONE) {
            throw new InvalidModelException(
                    "Model for Resource %s(%d) in %s is invalid : an executable resource MUST NOT have a type(%s)",
                    resource.name, resource.id, modelName, resource.type);
        } else if (!resource.operations.isExecutable() && resource.type == Type.NONE) {
            throw new InvalidModelException(
                    "Model for Resource %s(%d) in %s is invalid : a none executable resource MUST have a type.",
                    resource.name, resource.id, modelName, resource.type);
        }
        if (lwm2mVersion.equals(LwM2mVersion.V1_0.toString()))
            switch (resource.type) {
            case NONE:
            case STRING:
            case INTEGER:
            case FLOAT:
            case BOOLEAN:
            case OPAQUE:
            case TIME:
            case OBJLNK:
                return;
            default:
                throw new InvalidModelException(
                        "Model for Resource (%d) in %s is invalid : Resource type % is not supported by LWM2M v%s",
                        resource.id, modelName, resource.type, lwm2mVersion);
            }
    }

    protected void validateURN(String urn, ObjectModel object, String modelName) throws InvalidModelException {
        if (!urn.startsWith("urn:oma:lwm2m:")) {
            throw new InvalidModelException(
                    "Model for Object %s(%d) in %s is invalid : URN (%s) MUST start with urn:oma:lwm2m.", object.name,
                    object.id, modelName, urn);
        }
        String[] urnParts = urn.split(":");
        if (urnParts.length != 5 && urnParts.length != 6) {
            throw new InvalidModelException(
                    "Model for Object %s(%d) in %s is invalid : URN (%s) MUST be composed of 5 or 6 parts.",
                    object.name, object.id, modelName, urn);
        }
        String objectId = urnParts[4];
        if (!objectId.equals(Integer.toString(object.id))) {
            throw new InvalidModelException(
                    "Model for Object %s(%d) in %s is invalid : URN (%s) object id part MUST be equals to object id",
                    object.name, object.id, modelName, urn);
        }
        String kind = urnParts[3];
        String expectedKind = URN.getUrnKind(object.id);
        if (!expectedKind.equals(kind)) {
            throw new InvalidModelException(
                    "Model for Object %s(%d) in %s is invalid : URN (%s) kind MUST be %s instead of %s", object.name,
                    object.id, modelName, urn, expectedKind, kind);
        }

        if (urnParts.length == 6) {
            String version = urnParts[5];
            String expectedVersion = object.version;
            if (!expectedVersion.equals(version)) {
                throw new InvalidModelException(
                        "Model for Object %s(%d) in %s is invalid : URN (%s) version MUST be equals to object version (%s)",
                        object.name, object.id, modelName, urn, expectedVersion);
            }
        } else {
            if (!ObjectModel.DEFAULT_VERSION.equals(object.version)) {
                throw new InvalidModelException(
                        "Model for Object %s(%d) in %s is invalid : URN (%s) version MUST be present as object version is not %s",
                        object.name, object.id, modelName, urn, ObjectModel.DEFAULT_VERSION);
            }
        }
    }

    protected void validateVersion(String version, ObjectModel object, String fieldName, String modelName)
            throws InvalidModelException {

        String errorMsg = Version.validate(version);
        if (errorMsg != null)
            throw new InvalidModelException(
                    "Model for Object (%d) in %s is invalid : field %s is invalid : %s  MUST NOT be null or empty",
                    object.id, modelName, fieldName, errorMsg);
    }

    protected void validateResourceFieldNotNull(ResourceModel resource, String modelName, Object fieldValue,
            String fieldName) throws InvalidModelException {
        if (fieldValue == null) {
            throw new InvalidModelException("Model for Resource (%d) in %s is invalid : '%s' field MUST NOT be null",
                    resource.id, modelName, fieldName);
        }
    }

    protected void validateModelFieldNotNull(ObjectModel object, String modelName, Object fieldValue, String fieldName)
            throws InvalidModelException {
        if (fieldValue == null) {
            throw new InvalidModelException("Model for Object (%d) in %s is invalid : '%s' field MUST NOT be null",
                    object.id, modelName, fieldName);
        }
    }
}
