/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.request;

import java.util.Arrays;
import java.util.List;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mNodeException;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.core.util.Validate;

/**
 * The "Read-Composite" operation can be used by the LwM2M Server to selectively read any combination of Objects, Object
 * Instance(s), Resources, and/or Resource Instances of different or same Objects in a single request.
 */
public class ReadCompositeRequest extends AbstractLwM2mRequest<ReadCompositeResponse>
        implements CompositeDownlinkRequest<ReadCompositeResponse> {

    private final List<LwM2mPath> paths;
    private final ContentFormat requestContentFormat;
    private final ContentFormat responseContentFormat;

    /**
     * Create ReadComposite Request.
     *
     * @param requestContentFormat The {@link ContentFormat} used to encode the list of {@link LwM2mPath}
     * @param responseContentFormat The {@link ContentFormat} requested to encode the {@link LwM2mNode} of the response.
     * @param paths List of {@link LwM2mPath} corresponding to {@link LwM2mNode} to read.
     * @exception InvalidRequestException if path has invalid format.
     *
     */
    public ReadCompositeRequest(ContentFormat requestContentFormat, ContentFormat responseContentFormat,
            String... paths) {
        this(getLwM2mPathList(Arrays.asList(paths)), requestContentFormat, responseContentFormat, null);
    }

    /**
     * Create ReadComposite Request.
     *
     * @param requestContentFormat The {@link ContentFormat} used to encode the list of {@link LwM2mPath}
     * @param responseContentFormat The {@link ContentFormat} requested to encode the {@link LwM2mNode} of the response.
     * @param paths List of {@link LwM2mPath} corresponding to {@link LwM2mNode} to read.
     * @exception InvalidRequestException if path has invalid format.
     *
     */
    public ReadCompositeRequest(ContentFormat requestContentFormat, ContentFormat responseContentFormat,
            List<String> paths) {
        this(getLwM2mPathList(paths), requestContentFormat, responseContentFormat, null);
    }

    private static List<LwM2mPath> getLwM2mPathList(List<String> paths) {
        try {
            return LwM2mPath.getLwM2mPathList(paths);
        } catch (LwM2mNodeException | IllegalArgumentException e) {
            throw new InvalidRequestException("invalid path format");
        }
    }

    /**
     * Create ReadComposite Request.
     * <p>
     * This constructor is more for internal usage.
     *
     * @param paths List of {@link LwM2mPath} corresponding to {@link LwM2mNode} to read.
     * @param requestContentFormat The {@link ContentFormat} used to encode the list of {@link LwM2mPath}
     * @param responseContentFormat The {@link ContentFormat} requested to encode the {@link LwM2mNode} of the response.
     * @param coapRequest the underlying request.
     * @exception InvalidRequestException if paths list is invalid.
     *
     */
    public ReadCompositeRequest(List<LwM2mPath> paths, ContentFormat requestContentFormat,
            ContentFormat responseContentFormat, Object coapRequest) {
        super(coapRequest);

        try {
            Validate.notEmpty(paths, "paths are mandatory");
            LwM2mPath.validateNotOverlapping(paths);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException(exception.getMessage());
        }

        this.paths = paths;
        this.requestContentFormat = requestContentFormat;
        this.responseContentFormat = responseContentFormat;
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    public ContentFormat getRequestContentFormat() {
        return requestContentFormat;
    }

    public ContentFormat getResponseContentFormat() {
        return responseContentFormat;
    }

    @Override
    public List<LwM2mPath> getPaths() {
        return paths;
    }

    @Override
    public String toString() {
        return String.format("ReadCompositeRequest [paths=%s, request format=%s, response format= %s]", getPaths(),
                requestContentFormat, responseContentFormat);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((paths == null) ? 0 : paths.hashCode());
        result = prime * result + ((requestContentFormat == null) ? 0 : requestContentFormat.hashCode());
        result = prime * result + ((responseContentFormat == null) ? 0 : responseContentFormat.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ReadCompositeRequest other = (ReadCompositeRequest) obj;
        if (paths == null) {
            if (other.paths != null)
                return false;
        } else if (!paths.equals(other.paths))
            return false;
        if (requestContentFormat == null) {
            if (other.requestContentFormat != null)
                return false;
        } else if (!requestContentFormat.equals(other.requestContentFormat))
            return false;
        if (responseContentFormat == null) {
            if (other.responseContentFormat != null)
                return false;
        } else if (!responseContentFormat.equals(other.responseContentFormat))
            return false;
        return true;
    }

}
