package org.eclipse.leshan.core.attributes;

import java.util.Collections;
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

    private static Map<String, AttributeModel> initializeModelMap() {
        HashMap<String, AttributeModel> modelMap = new HashMap<>();
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
        return Collections.unmodifiableMap(modelMap);
    }
    
    private static final Map<String, AttributeModel> modelMap = initializeModelMap();

    private final String coRELinkParam;
    private final Attachment attachment;
    private final Set<AssignationLevel> assignationLevels;
    private final AccessMode accessMode;
    private final Class<?> valueClass;

    AttributeModel(String coRELinkParam, Attachment attachment, Set<AssignationLevel> assignationLevels,
            AccessMode accessMode, Class<?> valueClass) {
        this.coRELinkParam = coRELinkParam;
        this.attachment = attachment;
        this.assignationLevels = assignationLevels;
        this.accessMode = accessMode;
        this.valueClass = valueClass;
    }

    public static AttributeModel get(String coRELinkParam) {
        return modelMap.get(coRELinkParam);
    }

    public String getCoRELinkParam() {
        return coRELinkParam;
    }

    public Attachment getAttachment() {
        return attachment;
    }

    public Set<AssignationLevel> getAssignationLevels() {
        return assignationLevels;
    }

    public AccessMode getAccessMode() {
        return accessMode;
    }

    public Class<?> getValueClass() {
        return valueClass;
    }
}
