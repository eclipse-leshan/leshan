/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.core.request;

import org.eclipse.leshan.core.CustomTaskContainer;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;

/**
 * A Lightweight M2M request for retrieving the values of resources from a LWM2M Client.
 *
 * The request can be used to retrieve the value(s) of one or all attributes of one particular or all instances of a
 * particular object type.
 */
public class CustomTaskRequest {
    public static void handleCustomTask(String taskName, String imei) {
        checkRequest(taskName, imei);
    }

    static void checkRequest(String taskName, String imei) {
        if (taskName.equals("NoAnswerRequest")) {
            requestUpdateNoAnswer(imei);
        }
        if (taskName.equals("WaitForSend")) {
            waitForSend(imei);
        }
        if (taskName.equals("CheckForSend")) {
            checkForSend(imei);
        }
        if (taskName.equals("DoubleRead")) {
            doubleRead(imei);
        }
    }

    static void requestUpdateNoAnswer(String imei) {
        CustomTaskContainer.createInstance(imei).get(imei).put("requestUpdateAnswer", false);
    }

    static void waitForSend(String imei) {
        CustomTaskContainer.createInstance(imei).get(imei).put("waitForSend", true);
    }

    static void checkForSend(String imei) {
        CustomTaskContainer.createInstance(imei);
        if (!CustomTaskContainer.createInstance(imei).get(imei).get("waitForSend")
                && !CustomTaskContainer.createInstance(imei).get(imei).get("sendReceived")) {
            throw new InvalidRequestException("Nothing is happening");
        } else if (CustomTaskContainer.createInstance(imei).get(imei).get("waitForSend")
                && !CustomTaskContainer.createInstance(imei).get(imei).get("sendReceived")) {
            throw new InvalidRequestException("Send has not been received");
        } else if (!CustomTaskContainer.createInstance(imei).get(imei).get("waitForSend")
                && CustomTaskContainer.createInstance(imei).get(imei).get("sendReceived")) {
            CustomTaskContainer.createInstance(imei).get(imei).put("sendReceived", false);
        } else {
            CustomTaskContainer.createInstance(imei).get(imei).put("sendReceived", false);
            CustomTaskContainer.createInstance(imei).get(imei).put("waitForSend", false);
        }
    }

    static void doubleRead(String imei) {
        CustomTaskContainer.createInstance(imei).get(imei).put("doubleRead", true);
    }
}
