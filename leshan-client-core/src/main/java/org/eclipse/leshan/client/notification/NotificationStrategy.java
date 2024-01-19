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

import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.link.lwm2m.attributes.NotificationAttributeTree;
import org.eclipse.leshan.core.node.LwM2mChildNode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;

// TODO The way write attribute should be handle is not clear in LWM2M specification (-_-!) ... which is not an ideal situation ...
// See : https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/478
// So we create a dedicated class about how this attributes should be handled.
// The idea to then create a Interface + Default implement and let users implements their own strategy depending of their understanding or needs.
public class NotificationStrategy {

    // TODO this first implementaiton is a very over simple one. Just for validate design

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
        // attributes.contains(LwM2mAttributes.GREATER_THAN) || attributes.contains(LwM2mAttributes.LESSER_THAN)
        // || attributes.contains(LwM2mAttributes.STEP
        // TODO not implemented yet
        return false;
    }

    public boolean shouldTriggerNotificationBasedOnValueChange(LwM2mPath path, NotificationAttributeTree attributes,
            LwM2mNode lastSentValue, LwM2mChildNode newValue) {
        // TODO not implemented yet
        return false;
    }
}
