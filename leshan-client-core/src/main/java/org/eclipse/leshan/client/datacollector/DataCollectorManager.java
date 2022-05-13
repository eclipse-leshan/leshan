package org.eclipse.leshan.client.datacollector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.client.resource.LwM2mRootEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.response.ReadCompositeResponse;

// TODO I don't know if we need a DataCollector Manager ?
// I'm not even sure we want to have several data collector by client ?
public class DataCollectorManager {
    private final List<DataCollector> dataCollectors;
    private final LwM2mRootEnabler rootEnabler;

    public DataCollectorManager(List<DataCollector> dataCollectors, LwM2mRootEnabler rootEnabler) {
        this.rootEnabler = rootEnabler;
        this.dataCollectors = dataCollectors != null ? dataCollectors : new ArrayList<DataCollector>();
    }

    /**
     * Get current LWM2M node value.
     */
    // TODO maybe we can find a better name
    public Map<LwM2mPath, LwM2mNode> readFromEnabler(LwM2mPath... paths) {
        // HACK we use SENML_CBOR content format but it will not be really used...

        // Here we use ServerIdentity.System to read data but we should probably use a real server ?
        // to be sure we only collect allowed data ? (e.g. if one day we implement ACL)
        // Maybe this means that a collector could only act for 1 server (but this could generate duplicate collection
        // of same data)?
        // OR we collect everything with ServerIdentity.SYSTEM and then we filter on send depending to who we send (but
        // this will generate duplicate checks)?
        // anyway for now we don't support ACL and we only support 1 server, so I don't know if this is a real question.
        ReadCompositeResponse response = rootEnabler.read(ServerIdentity.SYSTEM, new ReadCompositeRequest(
                Arrays.asList(paths), ContentFormat.SENML_CBOR, ContentFormat.SENML_CBOR, null));
        if (response.isSuccess()) {
            return response.getContent();
        } else {
            // TODO handle this case at least a LOG ?
            return null;
        }
    }

    public TimestampedLwM2mNodes getCollectedData() {
        TimestampedLwM2mNodes.Builder builder = TimestampedLwM2mNodes.builder();
        for (DataCollector dataCollector : dataCollectors) {
            builder.add(dataCollector.getTimestampedNodes());
        }
        return builder.build();
    }
}
