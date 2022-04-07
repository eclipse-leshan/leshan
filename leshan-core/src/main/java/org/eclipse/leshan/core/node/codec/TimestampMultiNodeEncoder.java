package org.eclipse.leshan.core.node.codec;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;

public interface TimestampMultiNodeEncoder {
    byte[] encodeTimestampedNodes(TimestampedLwM2mNodes timestampedNodes, LwM2mModel model, LwM2mValueConverter converter) throws CodecException;
}
