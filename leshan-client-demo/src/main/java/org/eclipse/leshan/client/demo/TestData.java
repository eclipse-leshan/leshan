package org.eclipse.leshan.client.demo;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestData extends BaseInstanceEnabler implements Destroyable {

    private static final Logger LOG = LoggerFactory.getLogger(TestData.class);

    private static final int INT_VAL = 100;
    private static final int FLOAT_VAL = 101;
    private static final int STRING_VAL = 102;
    private static final int BOOL_VAL = 103;
    private static final int TIME_VAL = 104;
    private static final int LINK_VAL = 105;
    private static final int OPAQUE_VAL = 106;
    private static final int INT_ARRAY_VAL = 1000;
    private static final int FLOAT_ARRAY_VAL = 1001;
    private static final int STRING_ARRAY_VAL = 1002;
    private static final int BOOL_ARRAY_VAL = 1003;
    private static final int TIME_ARRAY_VAL = 1004;
    private static final int LINK_ARRAY_VAL = 1005;
    private static final int OPAQUE_ARRAY_VAL = 1006;
    private static final List<Integer> supportedResources = Arrays.asList(INT_VAL, FLOAT_VAL, STRING_VAL, BOOL_VAL,
            TIME_VAL, LINK_VAL, OPAQUE_VAL, INT_ARRAY_VAL, FLOAT_ARRAY_VAL, STRING_ARRAY_VAL, BOOL_ARRAY_VAL,
            TIME_ARRAY_VAL, LINK_ARRAY_VAL, OPAQUE_ARRAY_VAL);
    private final ScheduledExecutorService scheduler;
    private int intVal = 20;
    private double floatVal = 20d;
    private String stringVal = "hello world";
    private boolean boolVal = true;
    private Date timeVal = new Date();
    private ObjectLink linkVal = new ObjectLink(3, 0);
    private byte[] opaqueVal = fromHexString("00112233445566778899");
    private Map<Integer, Long> intArrayVal = new HashMap<>();
    private Map<Integer, Double> floatArrayVal = new HashMap<>();
    private Map<Integer, String> stringArrayVal = new HashMap<>();
    private Map<Integer, Boolean> boolArrayVal = new HashMap<>();
    private Map<Integer, Date> timeArrayVal = new HashMap<>();
    private Map<Integer, ObjectLink> linkArrayVal = new HashMap<>();
    private Map<Integer, byte[]> opaqueArrayVal = new HashMap<>();

    private static byte[] fromHexString(String src) {
        byte[] biBytes = new BigInteger("10" + src.replaceAll("\\s", ""), 16).toByteArray();
        return Arrays.copyOfRange(biBytes, 1, biBytes.length);
    }

    public TestData() {
        intArrayVal.put(0, 10L);
        intArrayVal.put(2, 20L);
        intArrayVal.put(3, 30L);
        floatArrayVal.put(0, 1.5d);
        floatArrayVal.put(1, 2.5d);
        stringArrayVal.put(10, "hello world");
        stringArrayVal.put(20, "another test");
        boolArrayVal.put(0, false);
        boolArrayVal.put(1, true);
        timeArrayVal.put(0, new Date());
        timeArrayVal.put(1, new Date());
        linkArrayVal.put(10, new ObjectLink(3, 0));
        linkArrayVal.put(11, new ObjectLink(7, 0));
        opaqueArrayVal.put(12, fromHexString("AABBCCDD"));
        opaqueArrayVal.put(24, fromHexString("11223344"));
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Test Incrementer"));
        scheduler.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                adjustValues();
            }
        }, 2, 2, TimeUnit.SECONDS);
    }

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        LOG.info("Read on test resource /{}/{}/{}", getModel().id, getId(), resourceId);
        switch (resourceId) {
        case INT_VAL:
            return ReadResponse.success(resourceId, intVal);
        case FLOAT_VAL:
            return ReadResponse.success(resourceId, floatVal);
        case STRING_VAL:
            return ReadResponse.success(resourceId, stringVal);
        case BOOL_VAL:
            return ReadResponse.success(resourceId, boolVal);
        case TIME_VAL:
            return ReadResponse.success(resourceId, timeVal);
        case LINK_VAL:
            return ReadResponse.success(resourceId, linkVal);
        case OPAQUE_VAL:
            return ReadResponse.success(resourceId, opaqueVal);
        case INT_ARRAY_VAL:
            return ReadResponse.success(resourceId, intArrayVal, ResourceModel.Type.INTEGER);
        case FLOAT_ARRAY_VAL:
            return ReadResponse.success(resourceId, floatArrayVal, ResourceModel.Type.FLOAT);
        case STRING_ARRAY_VAL:
            return ReadResponse.success(resourceId, stringArrayVal, ResourceModel.Type.STRING);
        case BOOL_ARRAY_VAL:
            return ReadResponse.success(resourceId, boolArrayVal, ResourceModel.Type.BOOLEAN);
        case TIME_ARRAY_VAL:
            return ReadResponse.success(resourceId, timeArrayVal, ResourceModel.Type.TIME);
        case LINK_ARRAY_VAL:
            return ReadResponse.success(resourceId, linkArrayVal, ResourceModel.Type.OBJLNK);
        case OPAQUE_ARRAY_VAL:
            return ReadResponse.success(resourceId, opaqueArrayVal, ResourceModel.Type.OPAQUE);
        default:
            return super.read(identity, resourceId);
        }
    }

    @Override
    public synchronized WriteResponse write(ServerIdentity identity, boolean replace, int resourceId,
            LwM2mResource value) {
        LOG.info("Write on test resource /{}/{}/{}", getModel().id, getId(), resourceId);
        switch (resourceId) {
        case INT_VAL:
            intVal = ((Long) value.getValue()).intValue();
            return WriteResponse.success();
        case FLOAT_VAL:
            floatVal = (double) value.getValue();
            return WriteResponse.success();
        case STRING_VAL:
            stringVal = (String) value.getValue();
            return WriteResponse.success();
        case BOOL_VAL:
            boolVal = (boolean) value.getValue();
            return WriteResponse.success();
        case TIME_VAL:
            timeVal = (Date) value.getValue();
            return WriteResponse.success();
        case LINK_VAL:
            linkVal = (ObjectLink) value.getValue();
            return WriteResponse.success();
        case OPAQUE_VAL:
            opaqueVal = (byte[]) value.getValue();
            return WriteResponse.success();
        case INT_ARRAY_VAL:
            if (replace) {
                intArrayVal.clear();
            }
            if (value.isMultiInstances()) {
                Map<Integer, LwM2mResourceInstance> intMap = value.getInstances();
                for (Map.Entry<Integer, LwM2mResourceInstance> entry : intMap.entrySet()) {
                    intArrayVal.put(entry.getKey(), (Long) entry.getValue().getValue());
                }
                return WriteResponse.success();
            }
            return WriteResponse.badRequest("expected array");
        case FLOAT_ARRAY_VAL:
            if (replace) {
                floatArrayVal.clear();
            }
            if (value.isMultiInstances()) {
                Map<Integer, LwM2mResourceInstance> intMap = value.getInstances();
                for (Map.Entry<Integer, LwM2mResourceInstance> entry : intMap.entrySet()) {
                    floatArrayVal.put(entry.getKey(), (double) entry.getValue().getValue());
                }
                return WriteResponse.success();
            }
            return WriteResponse.badRequest("expected array");
        case STRING_ARRAY_VAL:
            if (replace) {
                stringArrayVal.clear();
            }
            if (value.isMultiInstances()) {
                Map<Integer, LwM2mResourceInstance> intMap = value.getInstances();
                for (Map.Entry<Integer, LwM2mResourceInstance> entry : intMap.entrySet()) {
                    stringArrayVal.put(entry.getKey(), (String) entry.getValue().getValue());
                }
                return WriteResponse.success();
            }
            return WriteResponse.badRequest("expected array");
        case BOOL_ARRAY_VAL:
            if (replace) {
                boolArrayVal.clear();
            }
            if (value.isMultiInstances()) {
                Map<Integer, LwM2mResourceInstance> intMap = value.getInstances();
                for (Map.Entry<Integer, LwM2mResourceInstance> entry : intMap.entrySet()) {
                    boolArrayVal.put(entry.getKey(), (boolean) entry.getValue().getValue());
                }
                return WriteResponse.success();
            }
            return WriteResponse.badRequest("expected array");
        case TIME_ARRAY_VAL:
            if (replace) {
                timeArrayVal.clear();
            }
            if (value.isMultiInstances()) {
                Map<Integer, LwM2mResourceInstance> intMap = value.getInstances();
                for (Map.Entry<Integer, LwM2mResourceInstance> entry : intMap.entrySet()) {
                    timeArrayVal.put(entry.getKey(), (Date) entry.getValue().getValue());
                }
                return WriteResponse.success();
            }
            return WriteResponse.badRequest("expected array");
        case LINK_ARRAY_VAL:
            if (replace) {
                linkArrayVal.clear();
            }
            if (value.isMultiInstances()) {
                Map<Integer, LwM2mResourceInstance> intMap = value.getInstances();
                for (Map.Entry<Integer, LwM2mResourceInstance> entry : intMap.entrySet()) {
                    linkArrayVal.put(entry.getKey(), (ObjectLink) entry.getValue().getValue());
                }
                return WriteResponse.success();
            }
            return WriteResponse.badRequest("expected array");
        case OPAQUE_ARRAY_VAL:
            if (replace) {
                opaqueArrayVal.clear();
            }
            if (value.isMultiInstances()) {
                Map<Integer, LwM2mResourceInstance> intMap = value.getInstances();
                for (Map.Entry<Integer, LwM2mResourceInstance> entry : intMap.entrySet()) {
                    opaqueArrayVal.put(entry.getKey(), (byte[]) entry.getValue().getValue());
                }
                return WriteResponse.success();
            }
            return WriteResponse.badRequest("expected array");
        default:
            return WriteResponse.notFound();
        }
    }

    private void adjustValues() {
        floatVal += 0.5d;
        intVal += 1;
        fireResourcesChange(INT_VAL, FLOAT_VAL);
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

    @Override
    public void destroy() {
        scheduler.shutdown();
    }
}