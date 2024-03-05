/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
package org.eclipse.leshan.client.notification;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.client.notification.checker.CriteriaBasedOnValueChecker;
import org.eclipse.leshan.client.notification.checker.FloatChecker;
import org.eclipse.leshan.client.notification.checker.IntegerChecker;
import org.eclipse.leshan.client.notification.checker.UnsignedIntegerChecker;
import org.eclipse.leshan.core.link.lwm2m.attributes.InvalidAttributesException;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeModel;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.link.lwm2m.attributes.NotificationAttributeTree;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mChildNode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;

/**
 * Default implementation of {@link NotificationStrategy}
 */
public class DefaultNotificationStrategy implements NotificationStrategy {

    private final Map<Type, CriteriaBasedOnValueChecker> checkers;

    public static Map<Type, CriteriaBasedOnValueChecker> createDefaultCheckers() {
        Map<Type, CriteriaBasedOnValueChecker> checkers = new HashMap<>();
        checkers.put(Type.FLOAT, new FloatChecker());
        checkers.put(Type.INTEGER, new IntegerChecker());
        checkers.put(Type.UNSIGNED_INTEGER, new UnsignedIntegerChecker());
        return checkers;
    }

    public DefaultNotificationStrategy() {
        this(createDefaultCheckers());
    }

    public DefaultNotificationStrategy(Map<Type, CriteriaBasedOnValueChecker> checkers) {
        this.checkers = checkers;
    }

    @Override
    public NotificationAttributeTree selectNotificationsAttributes(LwM2mPath path, NotificationAttributeTree attributes)
            throws InvalidAttributesException {

        LwM2mAttributeSet set = attributes.getWithInheritance(path);
        if (set == null || set.isEmpty()) {
            return null;
        }
        set.validate(path, null);

        NotificationAttributeTree result = new NotificationAttributeTree();
        result.put(path, set);
        return result;
    }

    @Override
    public boolean hasAttribute(NotificationAttributeTree attributes, LwM2mPath path, LwM2mAttributeModel<?> model) {
        return getAttributeValue(attributes, path, model) != null;
    }

    @Override
    public <T> T getAttributeValue(NotificationAttributeTree attributes, LwM2mPath path, LwM2mAttributeModel<T> model) {
        LwM2mAttributeSet set = attributes.get(path);
        if (set == null)
            return null;
        else {
            LwM2mAttribute<T> attr = set.get(model);
            if (attr == null)
                return null;
            else
                return attr.getValue();
        }
    }

    @Override
    public boolean hasCriteriaBasedOnValue(NotificationAttributeTree attributes, LwM2mPath path) {
        LwM2mAttributeSet set = attributes.get(path);
        return set != null && (set.contains(LwM2mAttributes.GREATER_THAN) || set.contains(LwM2mAttributes.LESSER_THAN)
                || set.contains(LwM2mAttributes.STEP));
    }

    @Override
    public boolean shouldTriggerNotificationBasedOnValueChange(NotificationAttributeTree attributes, LwM2mPath path,
            LwM2mNode lastSentNode, LwM2mChildNode newNode) {

        // Get Previous and New Values
        Object lastSentValue;
        Object newValue;
        Type resourceType;
        if (lastSentNode instanceof LwM2mSingleResource && newNode instanceof LwM2mSingleResource) {
            lastSentValue = ((LwM2mSingleResource) lastSentNode).getValue();
            newValue = ((LwM2mSingleResource) newNode).getValue();
            resourceType = ((LwM2mSingleResource) newNode).getType();
        } else if (lastSentNode instanceof LwM2mResourceInstance && newNode instanceof LwM2mResourceInstance) {
            lastSentValue = ((LwM2mResourceInstance) lastSentNode).getValue();
            newValue = ((LwM2mResourceInstance) newNode).getValue();
            resourceType = ((LwM2mResourceInstance) newNode).getType();
        } else {
            throw new IllegalArgumentException(String.format(
                    "Unexpected nodes (last send node  %s new value %s) for check about value changed, only LwM2mSingleResource or LwM2mResourceInstance are supported",
                    lastSentNode.getClass().getSimpleName(), newNode.getClass().getSimpleName()));
        }

        // Check criteria
        CriteriaBasedOnValueChecker checker = checkers.get(resourceType);
        if (checker == null) {
            throw new IllegalArgumentException(
                    String.format("Unexpected resource type : %s is not supported", resourceType));
        }
        LwM2mAttributeSet set = attributes.get(path);
        return checker.shouldTriggerNotificationBasedOnValueChange(set, lastSentValue, newValue);
    }

}
