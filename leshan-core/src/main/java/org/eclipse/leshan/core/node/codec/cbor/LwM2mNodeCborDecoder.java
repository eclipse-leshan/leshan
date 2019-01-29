package org.eclipse.leshan.core.node.codec.cbor;

import java.util.List;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;

public class LwM2mNodeCborDecoder {
    public static <T extends LwM2mNode> T decode(byte[] content, LwM2mPath path, LwM2mModel model, Class<T> nodeClass) {
        // TODO Auto-generated method stub
        return null;
    }

    public static List<TimestampedLwM2mNode> decodeTimestamped(byte[] content, LwM2mPath path, LwM2mModel model,
            Class<? extends LwM2mNode> nodeClassFromPath) {
        // TODO Auto-generated method stub
        return null;
    }
}
