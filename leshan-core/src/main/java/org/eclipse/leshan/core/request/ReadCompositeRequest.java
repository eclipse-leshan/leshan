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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.ReadCompositeResponse;

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
     * 
     */
    public ReadCompositeRequest(ContentFormat requestContentFormat, ContentFormat responseContentFormat,
            String... paths) {
        this(newPaths(paths), requestContentFormat, responseContentFormat, null);
    }

    /**
     * Create ReadComposite Request.
     * 
     * @param requestContentFormat The {@link ContentFormat} used to encode the list of {@link LwM2mPath}
     * @param responseContentFormat The {@link ContentFormat} requested to encode the {@link LwM2mNode} of the response.
     * @param paths List of {@link LwM2mPath} corresponding to {@link LwM2mNode} to read.
     * 
     */
    public ReadCompositeRequest(ContentFormat requestContentFormat, ContentFormat responseContentFormat,
            List<String> paths) {
        this(newPaths(paths), requestContentFormat, responseContentFormat, null);
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
     * 
     */
    public ReadCompositeRequest(List<LwM2mPath> paths, ContentFormat requestContentFormat,
            ContentFormat responseContentFormat, Object coapRequest) {
        super(coapRequest);
        if (paths == null || paths.size() == 0)
            throw new InvalidRequestException("path is mandatory");

        // Ensure there is no overlapped Path (e.g. "3/0" and "/3/0/1")
        for (int i = 0; i < paths.size(); i++) {
            LwM2mPath firstPath = paths.get(i);
            for (int j = i + 1; j < paths.size(); j++) {
                LwM2mPath secondPath = paths.get(j);
                if (firstPath.startWith(secondPath) || secondPath.startWith(firstPath)) {
                    throw new InvalidRequestException("Invalid path list :  %s and %s are overlapped paths", firstPath,
                            secondPath);
                }
            }
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

    protected static List<LwM2mPath> newPaths(List<String> paths) {
        try {
            List<LwM2mPath> res = new ArrayList<>(paths.size());
            for (String path : paths) {
                res.add(new LwM2mPath(path));
            }
            return res;
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException();
        }
    }

    protected static List<LwM2mPath> newPaths(String[] paths) {
        try {
            List<LwM2mPath> res = new ArrayList<>(paths.length);
            for (String path : paths) {
                res.add(new LwM2mPath(path));
            }
            return res;
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException();
        }
    }
}
