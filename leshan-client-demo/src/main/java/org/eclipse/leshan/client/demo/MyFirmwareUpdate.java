/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.demo;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyFirmwareUpdate extends BaseInstanceEnabler {

    private static final Logger LOG = LoggerFactory.getLogger(MyFirmwareUpdate.class);

    public int state = 0;
    public int updateResult = 0;

    private final MyDevice device;

    public MyFirmwareUpdate(MyDevice device) {
        this.device = device;
    }

    @Override
    public ReadResponse read(int resourceid) {
        LOG.info("FW update - Read on resource " + resourceid);
        switch (resourceid) {
        case 3: // state
            return ReadResponse.success(LwM2mSingleResource.newIntegerResource(resourceid, state));
        case 5: // update result
            return ReadResponse.success(LwM2mSingleResource.newIntegerResource(resourceid, updateResult));

        default:
            return super.read(resourceid);
        }
    }

    @Override
    public WriteResponse write(int resourceid, LwM2mResource value) {
        LOG.info("FW update - write on resource " + resourceid);
        switch (resourceid) {
        case 1: // package URI resource
            LOG.info("Starting to download the firmware package from {}", value.getValue());

            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        LOG.info("Downloading...");
                        state = 1; // downloading
                        fireResourcesChange(3);

                        Thread.sleep(5000);

                        state = 2; // downloaded
                        fireResourcesChange(3);

                    } catch (Exception e) {
                        updateResult = 8; // failed
                        fireResourcesChange(5);
                    }

                }
            }).start();

            return WriteResponse.success();

        default:
            return super.write(resourceid, value);
        }
    }

    @Override
    public ExecuteResponse execute(int resourceid, String params) {
        LOG.info("FW update - Exec on resource " + resourceid);
        switch (resourceid) {
        case 2: // exec upgrade
            LOG.info("Updating the firmware");

            updateResult = 0;
            state = 3;
            fireResourcesChange(3, 5);

            new Thread(new Runnable() {

                @Override
                public void run() {

                    // perform upgrade
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        //
                    }

                    updateResult = 1; // udpate successful
                    state = 0; // idle
                    fireResourcesChange(3, 5);

                    // update the firmware version
                    device.firmwareVersion = "1.0.1";
                }
            }).start();

            return ExecuteResponse.success();

        default:
            return super.execute(resourceid, params);
        }
    }

}
