/*******************************************************************************
 * Copyright (c) 2021 Orange.
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
 *     Michał Wadowski (Orange) - Add Observe-Composite feature.
 *******************************************************************************/
package org.eclipse.leshan.core.request;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mNodeException;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;

/**
 * A Lightweight M2M request for observing changes of multiple Resources, Resources within an Object Instance or for all
 * the Object Instances of an Object within the LWM2M Client.
 */
public class ObserveCompositeRequest extends AbstractLwM2mRequest<ObserveCompositeResponse>
        implements CompositeDownlinkRequest<ObserveCompositeResponse> {

    private final ContentFormat requestContentFormat;
    private final ContentFormat responseContentFormat;

    private final List<LwM2mPath> paths;

    private final Map<String, String> context;

    /**
     * Create ObserveCompositeRequest Request.
     *
     * @param requestContentFormat The {@link ContentFormat} used to encode the list of {@link LwM2mPath}
     * @param responseContentFormat The {@link ContentFormat} requested to encode the {@link LwM2mNode} of the response.
     * @param paths List of {@link LwM2mPath} corresponding to {@link LwM2mNode} to read.
     * @exception InvalidRequestException if path has invalid format.
     *
     */
    public ObserveCompositeRequest(ContentFormat requestContentFormat, ContentFormat responseContentFormat,
            String... paths) {
        this(requestContentFormat, responseContentFormat, getLwM2mPathList(Arrays.asList(paths)));
    }

    private static List<LwM2mPath> getLwM2mPathList(List<String> paths) {
        try {
            return LwM2mPath.getLwM2mPathList(paths);
        } catch (LwM2mNodeException | IllegalArgumentException e) {
            throw new InvalidRequestException("invalid path format");
        }
    }

    /**
     * Create ObserveCompositeRequest Request.
     *
     * @param requestContentFormat The {@link ContentFormat} used to encode the list of {@link LwM2mPath}
     * @param responseContentFormat The {@link ContentFormat} requested to encode the {@link LwM2mNode} of the response.
     * @param paths List of {@link LwM2mPath} corresponding to {@link LwM2mNode} to read.
     *
     */
    public ObserveCompositeRequest(ContentFormat requestContentFormat, ContentFormat responseContentFormat,
            List<LwM2mPath> paths) {
        this(requestContentFormat, responseContentFormat, paths, null);
    }

    /**
     * Create ObserveCompositeRequest Request.
     *
     * @param requestContentFormat The {@link ContentFormat} used to encode the list of {@link LwM2mPath}
     * @param responseContentFormat The {@link ContentFormat} requested to encode the {@link LwM2mNode} of the response.
     * @param paths List of {@link LwM2mPath} corresponding to {@link LwM2mNode} to read.
     * @param coapRequest the underlying request.
     *
     */
    public ObserveCompositeRequest(ContentFormat requestContentFormat, ContentFormat responseContentFormat,
            List<LwM2mPath> paths, Object coapRequest) {
        super(coapRequest);

        validatePaths(paths);
        this.requestContentFormat = requestContentFormat;
        this.responseContentFormat = responseContentFormat;
        this.paths = paths;

        this.context = Collections.emptyMap();
    }

    private void validatePaths(List<LwM2mPath> paths) throws InvalidRequestException {
        if (paths == null || paths.size() == 0)
            throw new InvalidRequestException("Path is mandatory");

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
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * @return List of {@link LwM2mPath} corresponding to {@link LwM2mNode} to read.
     */
    @Override
    public List<LwM2mPath> getPaths() {
        return paths;
    }

    /**
     * @return the {@link ContentFormat} used to encode the list of {@link LwM2mPath}
     */
    public ContentFormat getRequestContentFormat() {
        return requestContentFormat;
    }

    /**
     * @return the {@link ContentFormat} requested to encode the {@link LwM2mNode} of the response.
     */
    public ContentFormat getResponseContentFormat() {
        return responseContentFormat;
    }

    /**
     * @return map containing the additional information relative to this observe-composite request.
     */
    public Map<String, String> getContext() {
        return context;
    }
}
