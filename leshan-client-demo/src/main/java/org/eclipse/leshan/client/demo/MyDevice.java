package org.eclipse.leshan.client.demo;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyDevice extends BaseInstanceEnabler {

    private static final Logger LOG = LoggerFactory.getLogger(MyDevice.class);

    private static final Random RANDOM = new Random();

    public MyDevice() {
        // notify new date each 5 second
        Timer timer = new Timer("Device-Current Time");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                fireResourcesChange(13);
            }
        }, 5000, 5000);
    }

    @Override
    public ReadResponse read(int resourceid) {
        LOG.info("Read on Device Resource " + resourceid);
        switch (resourceid) {
        case 0:
            return ReadResponse.success(resourceid, getManufacturer());
        case 1:
            return ReadResponse.success(resourceid, getModelNumber());
        case 2:
            return ReadResponse.success(resourceid, getSerialNumber());
        case 3:
            return ReadResponse.success(resourceid, getFirmwareVersion());
        case 9:
            return ReadResponse.success(resourceid, getBatteryLevel());
        case 10:
            return ReadResponse.success(resourceid, getMemoryFree());
        case 11:
            Map<Integer, Long> errorCodes = new HashMap<>();
            errorCodes.put(0, getErrorCode());
            return ReadResponse.success(resourceid, errorCodes, Type.INTEGER);
        case 13:
            return ReadResponse.success(resourceid, getCurrentTime());
        case 14:
            return ReadResponse.success(resourceid, getUtcOffset());
        case 15:
            return ReadResponse.success(resourceid, getTimezone());
        case 16:
            return ReadResponse.success(resourceid, getSupportedBinding());
        case 17:
            return ReadResponse.success(resourceid, getDeviceType());
        case 18:
            return ReadResponse.success(resourceid, getHardwareVersion());
        case 19:
            return ReadResponse.success(resourceid, getSoftwareVersion());
        case 20:
            return ReadResponse.success(resourceid, getBatteryStatus());
        case 21:
            return ReadResponse.success(resourceid, getMemoryTotal());
        default:
            return super.read(resourceid);
        }
    }

    @Override
    public ExecuteResponse execute(int resourceid, String params) {
        LOG.info("Execute on Device resource " + resourceid);
        if (params != null && params.length() != 0)
            System.out.println("\t params " + params);
        return ExecuteResponse.success();
    }

    @Override
    public WriteResponse write(int resourceid, LwM2mResource value) {
        LOG.info("Write on Device Resource " + resourceid + " value " + value);
        switch (resourceid) {
        case 13:
            return WriteResponse.notFound();
        case 14:
            setUtcOffset((String) value.getValue());
            fireResourcesChange(resourceid);
            return WriteResponse.success();
        case 15:
            setTimezone((String) value.getValue());
            fireResourcesChange(resourceid);
            return WriteResponse.success();
        default:
            return super.write(resourceid, value);
        }
    }

    private String getManufacturer() {
        return "Leshan Demo Device";
    }

    private String getModelNumber() {
        return "Model 500";
    }

    private String getSerialNumber() {
        return "LT-500-000-0001";
    }

    private String getFirmwareVersion() {
        return "1.0.0";
    }

    private long getErrorCode() {
        return 0;
    }

    private int getBatteryLevel() {
        return RANDOM.nextInt(101);
    }

    private long getMemoryFree() {
        return Runtime.getRuntime().freeMemory() / 1024;
    }

    private Date getCurrentTime() {
        return new Date();
    }

    private String utcOffset = new SimpleDateFormat("X").format(Calendar.getInstance().getTime());

    private String getUtcOffset() {
        return utcOffset;
    }

    private void setUtcOffset(String t) {
        utcOffset = t;
    }

    private String timeZone = TimeZone.getDefault().getID();

    private String getTimezone() {
        return timeZone;
    }

    private void setTimezone(String t) {
        timeZone = t;
    }

    private String getSupportedBinding() {
        return "U";
    }

    private String getDeviceType() {
        return "Demo";
    }

    private String getHardwareVersion() {
        return "1.0.1";
    }

    private String getSoftwareVersion() {
        return "1.0.2";
    }

    private int getBatteryStatus() {
        return RANDOM.nextInt(7);
    }

    private long getMemoryTotal() {
        return Runtime.getRuntime().totalMemory() / 1024;
    }
}
