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
package org.eclipse.leshan.client.notification.checker;

import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;

public class IntegerChecker implements CriteriaBasedOnValueChecker {

    @Override
    public boolean shouldTriggerNotificationBasedOnValueChange(LwM2mAttributeSet attributes, Object lastSentValue,
            Object newValue) {
        Long lastSentLong = (Long) lastSentValue;
        Long newLong = (Long) newValue;

        if (attributes.contains(LwM2mAttributes.STEP)) {
            return Math.abs(lastSentLong - newLong) >= attributes.get(LwM2mAttributes.STEP).getValue();
        } else if (attributes.contains(LwM2mAttributes.LESSER_THAN)) {
            Double lessThan = attributes.get(LwM2mAttributes.LESSER_THAN).getValue();
            return lastSentLong >= lessThan && newLong < lessThan;
        } else if (attributes.contains(LwM2mAttributes.GREATER_THAN)) {
            Double greaterThan = attributes.get(LwM2mAttributes.GREATER_THAN).getValue();
            return lastSentLong <= greaterThan && newLong > greaterThan;
        }
        return true;
    }

}
