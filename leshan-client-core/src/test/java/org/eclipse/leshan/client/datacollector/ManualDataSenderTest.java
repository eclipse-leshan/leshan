package org.eclipse.leshan.client.datacollector;

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

import java.util.*;
import java.util.stream.Collectors;

public class ManualDataSenderTest {
    private ManualDataSender manualDataSender;
    private Map<LwM2mPath, LwM2mNode> nodesBeforeSend;
    private final List<LwM2mPath> paths = Arrays.asList(new LwM2mPath(1, 2, 3), new LwM2mPath(4, 5, 6),
            new LwM2mPath(7, 8, 9));

    @Before
    public void prepareDataSender() {
        manualDataSender = new ManualDataSender();
        new FakeDataSenderManager(manualDataSender);
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
        manualDataSender.sendCollectedData(ServerIdentity.SYSTEM, ContentFormat.SENML_CBOR, 0, false);
        Map<LwM2mPath, LwM2mNode> nodesAfterSend = manualDataSender.getBuilder().build().getNodes();
        Assert.assertEquals(0, nodesAfterSend.size());
    }

    @Test
    public void test_unsuccessful_data_send() {
        manualDataSender.sendCollectedData(ServerIdentity.SYSTEM, ContentFormat.SENML_CBOR, 1, false);
        Map<LwM2mPath, LwM2mNode> nodesAfterSend = manualDataSender.getBuilder().build().getNodes();
        Assert.assertEquals(nodesBeforeSend, nodesAfterSend);
    }

    @Test
    public void test_error_during_data_send() {
        manualDataSender.sendCollectedData(ServerIdentity.SYSTEM, ContentFormat.SENML_CBOR, 2, false);
        Map<LwM2mPath, LwM2mNode> nodesAfterSend = manualDataSender.getBuilder().build().getNodes();
        Assert.assertEquals(nodesBeforeSend, nodesAfterSend);
    }

    @Test
    public void test_successful_data_send_without_flush() {
        manualDataSender.sendCollectedData(ServerIdentity.SYSTEM, ContentFormat.SENML_CBOR, 0, true);
        Map<LwM2mPath, LwM2mNode> nodesAfterSend = manualDataSender.getBuilder().build().getNodes();
        Assert.assertEquals(nodesBeforeSend, nodesAfterSend);
    }

    private static class FakeDataSenderManager extends DataSenderManager {
        private final Random random = new Random();

        public FakeDataSenderManager(DataSender dataSender) {
            super(Collections.singletonMap("", dataSender), null, null);
        }

        @Override
        public Map<LwM2mPath, LwM2mNode> getCurrentValue(ServerIdentity server, List<LwM2mPath> paths) {
            return paths.stream().map(this::readRandomValue)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        @Override
        public void sendData(ServerIdentity server, ContentFormat format, TimestampedLwM2mNodes nodes,
                ResponseCallback<SendResponse> onResponse, ErrorCallback onError, long timeoutInMs) {
            // hacky way of determining the outcome of a request
            if (timeoutInMs == 0) {
                onResponse.onResponse(SendResponse.success());
            } else if (timeoutInMs == 1) {
                onResponse.onResponse(SendResponse.notFound());
            } else if (timeoutInMs == 2) {
                onError.onError(new Exception());
            }
        }

        private Map.Entry<LwM2mPath, LwM2mNode> readRandomValue(LwM2mPath path) {
            LwM2mSingleResource resource = LwM2mSingleResource.newResource(path.getResourceId(), random.nextInt(1000));
            return new AbstractMap.SimpleEntry<>(path, resource);
        }
    }
}