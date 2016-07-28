/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.request;

import org.eclipse.leshan.ObserveSpec;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.util.Validate;

public class WriteAttributesRequest extends AbstractDownlinkRequest<WriteAttributesResponse> {

    private final ObserveSpec observeSpec;

    public WriteAttributesRequest(final int objectId, final ObserveSpec observeSpec) {
        this(new LwM2mPath(objectId), observeSpec);
    }

    public WriteAttributesRequest(final int objectId, final int objectInstanceId, final ObserveSpec observeSpec) {
        this(new LwM2mPath(objectId, objectInstanceId), observeSpec);
    }

    public WriteAttributesRequest(final int objectId, final int objectInstanceId, final int resourceId,
            final ObserveSpec observeSpec) {
        this(new LwM2mPath(objectId, objectInstanceId, resourceId), observeSpec);
    }

    public WriteAttributesRequest(final String path, final ObserveSpec observeSpec) {
        this(new LwM2mPath(path), observeSpec);
    }

    private WriteAttributesRequest(final LwM2mPath path, final ObserveSpec observeSpec) {
        super(path);
        Validate.notNull(observeSpec);
        this.observeSpec = observeSpec;
    }

    @Override
    public void accept(final DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    public ObserveSpec getObserveSpec() {
        return this.observeSpec;
    }

    @Override
    public String toString() {
        return String.format("WriteAttributesRequest [%s, attributes=%s]", getPath(), getObserveSpec());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((observeSpec == null) ? 0 : observeSpec.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        WriteAttributesRequest other = (WriteAttributesRequest) obj;
        if (observeSpec == null) {
            if (other.observeSpec != null)
                return false;
        } else if (!observeSpec.equals(other.observeSpec))
            return false;
        return true;
    }
}
