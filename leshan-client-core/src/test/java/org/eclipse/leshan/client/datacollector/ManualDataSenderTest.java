package org.eclipse.leshan.client.datacollector;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.response.SendResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ManualDataSenderTest {
    private ManualDataSender manualDataSender;
    private FakeDataSenderManager fakeDataSenderManager;
    private Map<LwM2mPath, LwM2mNode> nodesBeforeSend;
    private final List<LwM2mPath> paths = Arrays.asList(new LwM2mPath(1, 2, 3), new LwM2mPath(4, 5, 6),
            new LwM2mPath(7, 8, 9));

    @Before
    public void prepareDataSender() {
        manualDataSender = new ManualDataSender("sender");
        fakeDataSenderManager = new FakeDataSenderManager(manualDataSender);
        manualDataSender.collectData(paths);
        nodesBeforeSend = manualDataSender.getBuilder().build().getNodes();
    }

    @Test
    public void test_data_collecting() {
        TimestampedLwM2mNodes timestampedLwM2mNodes = manualDataSender.getBuilder().build();
        timestampedLwM2mNodes.getNodes().forEach((key, value) -> {
            Assert.assertTrue(paths.contains(key));
            Assert.assertNotNull(value);
        });
    }

    @Test
    public void test_successful_data_send() {
        fakeDataSenderManager.setSendDataOutcome(FakeDataSenderManager.SendDataOutcome.SUCCESS);
        manualDataSender.sendCollectedData(ServerIdentity.SYSTEM, ContentFormat.SENML_CBOR, 0, false);
        Map<LwM2mPath, LwM2mNode> nodesAfterSend = manualDataSender.getBuilder().build().getNodes();
        Assert.assertEquals(0, nodesAfterSend.size());
    }

    @Test
    public void test_unsuccessful_data_send() {
        fakeDataSenderManager.setSendDataOutcome(FakeDataSenderManager.SendDataOutcome.NOT_FOUND);
        manualDataSender.sendCollectedData(ServerIdentity.SYSTEM, ContentFormat.SENML_CBOR, 0, false);
        Map<LwM2mPath, LwM2mNode> nodesAfterSend = manualDataSender.getBuilder().build().getNodes();
        Assert.assertEquals(nodesBeforeSend, nodesAfterSend);
    }

    @Test
    public void test_error_during_data_send() {
        fakeDataSenderManager.setSendDataOutcome(FakeDataSenderManager.SendDataOutcome.ERROR);
        manualDataSender.sendCollectedData(ServerIdentity.SYSTEM, ContentFormat.SENML_CBOR, 0, false);
        Map<LwM2mPath, LwM2mNode> nodesAfterSend = manualDataSender.getBuilder().build().getNodes();
        Assert.assertEquals(nodesBeforeSend, nodesAfterSend);
    }

    @Test
    public void test_successful_data_send_without_flush() {
        fakeDataSenderManager.setSendDataOutcome(FakeDataSenderManager.SendDataOutcome.SUCCESS);
        manualDataSender.sendCollectedData(ServerIdentity.SYSTEM, ContentFormat.SENML_CBOR, 0, true);
        Map<LwM2mPath, LwM2mNode> nodesAfterSend = manualDataSender.getBuilder().build().getNodes();
        Assert.assertEquals(nodesBeforeSend, nodesAfterSend);
    }

    private static class FakeDataSenderManager extends DataSenderManager {
        public enum SendDataOutcome {
            SUCCESS, NOT_FOUND, ERROR
        }

        private final Random random = new Random();
        private SendDataOutcome sendDataOutcome;

        public FakeDataSenderManager(DataSender dataSender) {
            super(Collections.singletonMap("", dataSender), null, null);
        }

        public void setSendDataOutcome(SendDataOutcome sendDataOutcome) {
            this.sendDataOutcome = sendDataOutcome;
        }

        @Override
        public Map<LwM2mPath, LwM2mNode> getCurrentValue(ServerIdentity server, List<LwM2mPath> paths) {
            return paths.stream().map(this::readRandomValue)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        @Override
        public void sendData(ServerIdentity server, ContentFormat format, TimestampedLwM2mNodes nodes,
                ResponseCallback<SendResponse> onResponse, ErrorCallback onError, long timeoutInMs) {
            switch (sendDataOutcome) {
            case SUCCESS:
                onResponse.onResponse(SendResponse.success());
                break;
            case NOT_FOUND:
                onResponse.onResponse(SendResponse.notFound());
                break;
            case ERROR:
                onError.onError(new Exception());
                break;
            }
        }

        private Map.Entry<LwM2mPath, LwM2mNode> readRandomValue(LwM2mPath path) {
            LwM2mSingleResource resource = LwM2mSingleResource.newResource(path.getResourceId(), random.nextInt(1000));
            return new AbstractMap.SimpleEntry<>(path, resource);
        }
    }
}