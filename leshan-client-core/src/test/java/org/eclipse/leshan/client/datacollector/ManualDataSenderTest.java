package org.eclipse.leshan.client.datacollector;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.eclipse.leshan.client.datacollector.ManualDataSenderTest.FakeDataSenderManager.SendDataOutcome;
import org.eclipse.leshan.client.send.DataSender;
import org.eclipse.leshan.client.send.DataSenderManager;
import org.eclipse.leshan.client.send.ManualDataSender;
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

    private final ServerIdentity givenServer = ServerIdentity.SYSTEM;
    private final List<LwM2mPath> givenPaths = Arrays.asList(new LwM2mPath(1, 2, 3), new LwM2mPath(4, 5, 6),
            new LwM2mPath(7, 8, 9));

    @Before
    public void prepareDataSender() {
        manualDataSender = new ManualDataSender();
        fakeDataSenderManager = new FakeDataSenderManager(manualDataSender);
        fakeDataSenderManager.changeCurrentValues(givenServer, givenPaths);
    }

    @Test
    public void test_successful_data_send() {
        // store current value
        Map<LwM2mPath, LwM2mNode> currentValues = fakeDataSenderManager.getCurrentValues(givenServer, givenPaths);

        // Test successful collect and send
        fakeDataSenderManager.setSendDataOutcome(SendDataOutcome.SUCCESS);

        manualDataSender.collectData(givenPaths);
        manualDataSender.sendCollectedData(givenServer, ContentFormat.SENML_CBOR, 0, false);

        // ensure that sent values equals collected ones.
        TimestampedLwM2mNodes lastValuesSent = fakeDataSenderManager.getLastValuesSent();
        Assert.assertEquals(currentValues, lastValuesSent.getNodes());

        // re send to ensure data was flushed (we do not resent same data)
        Map<LwM2mPath, LwM2mNode> newValue = fakeDataSenderManager.changeCurrentValues(givenServer, givenPaths);
        manualDataSender.collectData(givenPaths);
        manualDataSender.sendCollectedData(givenServer, ContentFormat.SENML_CBOR, 0, false);
        lastValuesSent = fakeDataSenderManager.getLastValuesSent();

        Assert.assertEquals(1, lastValuesSent.getTimestamps().size());
        Assert.assertEquals(newValue, lastValuesSent.getNodes());
    }

    @Test
    public void test_collect_several_data() throws InterruptedException {
        // Test successful collect and send
        fakeDataSenderManager.setSendDataOutcome(SendDataOutcome.SUCCESS);

        // collect several timestamped values
        Map<LwM2mPath, LwM2mNode> firstValue = fakeDataSenderManager.getCurrentValues(givenServer, givenPaths);
        manualDataSender.collectData(givenPaths);

        Thread.sleep(100);
        Map<LwM2mPath, LwM2mNode> secondValue = fakeDataSenderManager.changeCurrentValues(givenServer, givenPaths);
        manualDataSender.collectData(givenPaths);

        Thread.sleep(100);
        Map<LwM2mPath, LwM2mNode> thirdValue = fakeDataSenderManager.changeCurrentValues(givenServer, givenPaths);
        manualDataSender.collectData(givenPaths);

        // send data
        manualDataSender.sendCollectedData(givenServer, ContentFormat.SENML_CBOR, 0, false);

        // ensure that sent values equals collected ones.
        TimestampedLwM2mNodes lastValuesSent = fakeDataSenderManager.getLastValuesSent();
        List<Long> timestamps = new ArrayList<>(lastValuesSent.getTimestamps());
        Assert.assertEquals(3, timestamps.size());
        Assert.assertEquals(firstValue, lastValuesSent.getNodesAt(timestamps.get(0)));
        Assert.assertEquals(secondValue, lastValuesSent.getNodesAt(timestamps.get(1)));
        Assert.assertEquals(thirdValue, lastValuesSent.getNodesAt(timestamps.get(2)));
    }

    @Test
    public void test_unsuccessful_data_send() {
        // store current value
        Map<LwM2mPath, LwM2mNode> currentValues = fakeDataSenderManager.getCurrentValues(givenServer, givenPaths);

        // Test collect and send with error response
        fakeDataSenderManager.setSendDataOutcome(SendDataOutcome.NOT_FOUND);

        manualDataSender.collectData(givenPaths);
        manualDataSender.sendCollectedData(givenServer, ContentFormat.SENML_CBOR, 0, false);

        // now resent with success
        fakeDataSenderManager.setSendDataOutcome(SendDataOutcome.SUCCESS);
        manualDataSender.sendCollectedData(givenServer, ContentFormat.SENML_CBOR, 0, false);

        // ensure that sent values equals collected ones.
        TimestampedLwM2mNodes lastValuesSent = fakeDataSenderManager.getLastValuesSent();
        Assert.assertEquals(currentValues, lastValuesSent.getNodes());
    }

    @Test
    public void test_error_during_data_send() {
        // store current value
        Map<LwM2mPath, LwM2mNode> currentValues = fakeDataSenderManager.getCurrentValues(givenServer, givenPaths);

        // Test collect and send failure
        fakeDataSenderManager.setSendDataOutcome(SendDataOutcome.ERROR);

        manualDataSender.collectData(givenPaths);
        manualDataSender.sendCollectedData(givenServer, ContentFormat.SENML_CBOR, 0, false);

        // now resent with success
        fakeDataSenderManager.setSendDataOutcome(SendDataOutcome.SUCCESS);
        manualDataSender.sendCollectedData(givenServer, ContentFormat.SENML_CBOR, 0, false);

        // ensure that sent values equals collected ones.
        TimestampedLwM2mNodes lastValuesSent = fakeDataSenderManager.getLastValuesSent();
        Assert.assertEquals(currentValues, lastValuesSent.getNodes());
    }

    @Test
    public void test_successful_data_send_without_flush() {
        // store current value
        Map<LwM2mPath, LwM2mNode> currentValues = fakeDataSenderManager.getCurrentValues(givenServer, givenPaths);

        // Test successful collect and send with flush
        boolean noFlush = true;
        fakeDataSenderManager.setSendDataOutcome(SendDataOutcome.SUCCESS);

        manualDataSender.collectData(givenPaths);
        manualDataSender.sendCollectedData(givenServer, ContentFormat.SENML_CBOR, 0, noFlush);

        // ensure that sent values equals collected ones.
        TimestampedLwM2mNodes lastValuesSent = fakeDataSenderManager.getLastValuesSent();
        Assert.assertEquals(currentValues, lastValuesSent.getNodes());

        // re send to ensure data was not flushed
        manualDataSender.sendCollectedData(givenServer, ContentFormat.SENML_CBOR, 0, false);

        lastValuesSent = fakeDataSenderManager.getLastValuesSent();
        Assert.assertEquals(currentValues, lastValuesSent.getNodes());
    }

    static class FakeDataSenderManager extends DataSenderManager {
        public static enum SendDataOutcome {
            SUCCESS, NOT_FOUND, ERROR
        }

        private final Random random = new Random();
        private Map<LwM2mPath, LwM2mNode> currentValues = new HashMap<>();
        private TimestampedLwM2mNodes lastValuesSent;
        private SendDataOutcome sendDataOutcome;

        public FakeDataSenderManager(DataSender dataSender) {
            super(Collections.singletonMap(dataSender.getName(), dataSender), null, null);
        }

        public void setSendDataOutcome(SendDataOutcome sendDataOutcome) {
            this.sendDataOutcome = sendDataOutcome;
        }

        @Override
        public Map<LwM2mPath, LwM2mNode> getCurrentValues(ServerIdentity server, List<LwM2mPath> paths) {
            return currentValues;
        }

        public Map<LwM2mPath, LwM2mNode> changeCurrentValues(ServerIdentity server, List<LwM2mPath> paths) {
            currentValues = paths.stream().map(this::readRandomValue)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            return currentValues;
        }

        @Override
        public void sendData(ServerIdentity server, ContentFormat format, TimestampedLwM2mNodes nodes,
                ResponseCallback<SendResponse> onResponse, ErrorCallback onError, long timeoutInMs) {
            switch (sendDataOutcome) {
            case SUCCESS:
                lastValuesSent = nodes;
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

        public TimestampedLwM2mNodes getLastValuesSent() {
            return lastValuesSent;
        }

        private Map.Entry<LwM2mPath, LwM2mNode> readRandomValue(LwM2mPath path) {
            LwM2mSingleResource resource = LwM2mSingleResource.newResource(path.getResourceId(), random.nextInt(1000));
            return new AbstractMap.SimpleEntry<>(path, resource);
        }
    }
}