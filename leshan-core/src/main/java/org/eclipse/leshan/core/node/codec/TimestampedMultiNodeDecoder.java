package org.eclipse.leshan.core.node.codec;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;

public interface TimestampedMultiNodeDecoder {

    TimestampedLwM2mNodes decodeMultiTimestampedNodes(byte[] content, LwM2mModel model) throws CodecException;

}
