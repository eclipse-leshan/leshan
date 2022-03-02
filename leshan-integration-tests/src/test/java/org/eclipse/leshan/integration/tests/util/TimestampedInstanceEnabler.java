package org.eclipse.leshan.integration.tests.util;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.response.ReadResponse;

public class TimestampedInstanceEnabler extends BaseInstanceEnabler {

    public TimestampedInstanceEnabler(int id) {
        super(id);
    }

    @Override
    public ReadResponse read(ServerIdentity identity, int resourceid) {
        LwM2mSingleResource lwM2mSingleResource = LwM2mSingleResource.newFloatResource(resourceid, 111.1);
        return ReadResponse.success(lwM2mSingleResource);
    }
}
