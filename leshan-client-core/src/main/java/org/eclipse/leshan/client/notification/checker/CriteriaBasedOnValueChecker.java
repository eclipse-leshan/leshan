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

import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;

/**
 * A {@link CriteriaBasedOnValueChecker} MUST evaluate new value based on Notification {@link LwM2mAttribute} and
 * previous value sent to determine if new notification should be sent.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-core-dynlink-07#section-4.7">Attribute Interactions
 *      </a>
 */
public interface CriteriaBasedOnValueChecker {
    /**
     * @param attributes {@link LwM2mAttributeSet} attached to the node.
     * @param lastSentValue value sent in previous notification.
     * @param newValue value for which it should be decided if a notification should be sent.
     * @return <code>true</code> if new notification should be sent.
     */
    boolean shouldTriggerNotificationBasedOnValueChange(LwM2mAttributeSet attributes, Object lastSentValue,
            Object newValue);
}
