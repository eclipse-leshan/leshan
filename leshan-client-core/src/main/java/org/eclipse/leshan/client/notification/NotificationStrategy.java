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
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.link.lwm2m.attributes.NotificationAttributeTree;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mChildNode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;

// TODO The way write attribute should be handle is not clear in LWM2M specification (-_-!) ... which is not an ideal situation ...
// See : https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/478
// So we create a dedicated class about how this attributes should be handled.
// The idea to then create a Interface + Default implement and let users implements their own strategy depending of their understanding or needs.
public class NotificationStrategy {

    protected Map<Type, CriteriaBasedOnValueChecker> checkers = new HashMap<>();

    public NotificationStrategy() {
        checkers.put(Type.FLOAT, new FloatChecker());
        checkers.put(Type.INTEGER, new IntegerChecker());
        checkers.put(Type.UNSIGNED_INTEGER, new UnsignedIntegerChecker());
    }

    public NotificationAttributeTree selectNotificationsAttributes(LwM2mPath path,
            NotificationAttributeTree attributes) {

        LwM2mAttributeSet set = attributes.getWithInheritance(path);
        NotificationAttributeTree result = new NotificationAttributeTree();
        result.put(path, set);

        return result;
    }

    public boolean hasPmin(NotificationAttributeTree attributes, LwM2mPath path) {
        return getPmin(path, attributes) != null;
    }

    public Long getPmin(LwM2mPath path, NotificationAttributeTree attributes) {
        LwM2mAttributeSet set = attributes.get(path);
        if (set == null)
            return null;
        else {
            LwM2mAttribute<Long> pmin = set.get(LwM2mAttributes.MINIMUM_PERIOD);
            if (pmin == null)
                return null;
            else
                return pmin.getValue();
        }
    }

    public boolean hasPmax(LwM2mPath path, NotificationAttributeTree attributes) {
        return getPmax(path, attributes) != null;
    }

    public Long getPmax(LwM2mPath path, NotificationAttributeTree attributes) {
        LwM2mAttributeSet set = attributes.get(path);
        if (set == null)
            return null;
        else {
            LwM2mAttribute<Long> pmax = set.get(LwM2mAttributes.MAXIMUM_PERIOD);
            if (pmax == null)
                return null;
            else
                return pmax.getValue();
        }
    }

    public boolean hasCriteriaBasedOnValue(LwM2mPath path, NotificationAttributeTree attributes) {
        LwM2mAttributeSet set = attributes.get(path);
        return set != null && (set.contains(LwM2mAttributes.GREATER_THAN) || set.contains(LwM2mAttributes.LESSER_THAN)
                || set.contains(LwM2mAttributes.STEP));
    }

    public boolean shouldTriggerNotificationBasedOnValueChange(LwM2mPath path, NotificationAttributeTree attributes,
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
