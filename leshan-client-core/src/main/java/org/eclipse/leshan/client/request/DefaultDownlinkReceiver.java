/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.client.request;

import org.eclipse.leshan.client.bootstrap.BootstrapHandler;
import org.eclipse.leshan.client.engine.RegistrationEngine;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.client.resource.LwM2mRootEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.request.BootstrapReadRequest;
import org.eclipse.leshan.core.request.BootstrapWriteRequest;
import org.eclipse.leshan.core.request.CancelCompositeObservationRequest;
import org.eclipse.leshan.core.request.CancelObservationRequest;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.DownlinkRequestVisitor;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.LwM2mRequest;
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.SimpleDownlinkRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.BootstrapDeleteResponse;
import org.eclipse.leshan.core.response.BootstrapDiscoverResponse;
import org.eclipse.leshan.core.response.BootstrapFinishResponse;
import org.eclipse.leshan.core.response.BootstrapReadResponse;
import org.eclipse.leshan.core.response.BootstrapWriteResponse;
import org.eclipse.leshan.core.response.CancelCompositeObservationResponse;
import org.eclipse.leshan.core.response.CancelObservationResponse;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.core.response.WriteCompositeResponse;
import org.eclipse.leshan.core.response.WriteResponse;

public class DefaultDownlinkReceiver implements DownlinkRequestReceiver {

    private final RegistrationEngine registrationEngine;
    private final BootstrapHandler bootstrapHandler;
    private final LwM2mRootEnabler rootEnabler;
    private final LwM2mObjectTree objectTree;

    public DefaultDownlinkReceiver(BootstrapHandler bootstrapHandler, LwM2mRootEnabler rootEnabler,
            LwM2mObjectTree objectTree, RegistrationEngine registrationEngine) {
        this.bootstrapHandler = bootstrapHandler;
        this.rootEnabler = rootEnabler;
        this.objectTree = objectTree;
        this.registrationEngine = registrationEngine;
    }

    @Override
    public <T extends LwM2mResponse> SendableResponse<T> requestReceived(ServerIdentity server,
            DownlinkRequest<T> request) {

        // Check if this is a well-known server
        if (!registrationEngine.isAllowedToCommunicate(server)) {
            ErrorResponseCreator<T> errorResponseCreator = new ErrorResponseCreator<>(
                    ResponseCode.INTERNAL_SERVER_ERROR, "server not allow to communicate");
            request.accept(errorResponseCreator);
            return errorResponseCreator.getResponse();
        }
        // Handle the request
        RequestHandler<T> requestHandler = new RequestHandler<T>(server);
        request.accept(requestHandler);
        return requestHandler.getResponse();
    }

    @Override
    public void onError(ServerIdentity identity, Exception e,
            Class<? extends LwM2mRequest<? extends LwM2mResponse>> requestType) {
    }

    public class RequestHandler<T extends LwM2mResponse> implements DownlinkRequestVisitor {

        private final ServerIdentity sender;
        private SendableResponse<? extends LwM2mResponse> response;

        public RequestHandler(ServerIdentity identity) {
            this.sender = identity;
        }

        @Override
        public void visit(ReadRequest request) {
            LwM2mObjectEnabler objectEnabler = getObjectEnabler(request);
            if (objectEnabler == null) {
                response = toSendableResponse(ReadResponse.notFound());
            } else {
                response = toSendableResponse(objectEnabler.read(sender, request));
            }
        }

        @Override
        public void visit(DiscoverRequest request) {
            LwM2mObjectEnabler objectEnabler = getObjectEnabler(request);
            if (objectEnabler == null) {
                response = toSendableResponse(DiscoverResponse.notFound());
            } else {
                response = toSendableResponse(objectEnabler.discover(sender, request));
            }
        }

        @Override
        public void visit(WriteRequest request) {
            LwM2mObjectEnabler objectEnabler = getObjectEnabler(request);
            if (objectEnabler == null) {
                response = toSendableResponse(WriteResponse.notFound());
            } else {
                response = toSendableResponse(objectEnabler.write(sender, request));
            }
        }

        @Override
        public void visit(WriteAttributesRequest request) {
            LwM2mObjectEnabler objectEnabler = getObjectEnabler(request);
            if (objectEnabler == null) {
                response = toSendableResponse(WriteAttributesResponse.notFound());
            } else {
                response = toSendableResponse(objectEnabler.writeAttributes(sender, request));
            }

        }

        @Override
        public void visit(ExecuteRequest request) {
            LwM2mObjectEnabler objectEnabler = getObjectEnabler(request);
            if (objectEnabler == null) {
                response = toSendableResponse(ExecuteResponse.notFound());
            } else {
                response = toSendableResponse(objectEnabler.execute(sender, request));
            }

        }

        @Override
        public void visit(CreateRequest request) {
            LwM2mObjectEnabler objectEnabler = getObjectEnabler(request);
            if (objectEnabler == null) {
                response = toSendableResponse(CreateResponse.notFound());
            } else {
                response = toSendableResponse(objectEnabler.create(sender, request));
            }

        }

        @Override
        public void visit(DeleteRequest request) {
            LwM2mObjectEnabler objectEnabler = getObjectEnabler(request);
            if (objectEnabler == null) {
                response = toSendableResponse(DeleteResponse.notFound());
            } else {
                response = toSendableResponse(objectEnabler.delete(sender, request));
            }

        }

        @Override
        public void visit(ObserveRequest request) {
            LwM2mObjectEnabler objectEnabler = getObjectEnabler(request);
            if (objectEnabler == null) {
                response = toSendableResponse(ObserveResponse.notFound());
            } else {
                response = toSendableResponse(objectEnabler.observe(sender, request));
            }
        }

        @Override
        public void visit(CancelObservationRequest request) {
            // TODO TL : check if there is something to call here
        }

        @Override
        public void visit(ReadCompositeRequest request) {
            response = toSendableResponse(rootEnabler.read(sender, request));
        }

        @Override
        public void visit(ObserveCompositeRequest request) {
            response = toSendableResponse(rootEnabler.observe(sender, request));
        }

        @Override
        public void visit(CancelCompositeObservationRequest request) {
            // TODO TL : check if there is something to call here
        }

        @Override
        public void visit(WriteCompositeRequest request) {
            response = toSendableResponse(rootEnabler.write(sender, request));
        }

        @Override
        public void visit(BootstrapDiscoverRequest request) {
            if (request.getPath().isRoot()) {
                response = toSendableResponse(bootstrapHandler.discover(sender, request));
            } else {
                LwM2mObjectEnabler objectEnabler = getObjectEnabler(request);
                if (objectEnabler == null) {
                    response = toSendableResponse(DeleteResponse.notFound());
                } else {
                    response = toSendableResponse(objectEnabler.discover(sender, request));
                }
            }
        }

        @Override
        public void visit(BootstrapWriteRequest request) {
            LwM2mObjectEnabler objectEnabler = getObjectEnabler(request);
            if (objectEnabler == null) {
                // NOT_FOUND is not defined for Bootstrap Write ... and no other code match,
                // The spec says :
                //
                // If any operation cannot be completed in the client and the reason cannot
                // be described by a more specific response code, then a generic response code
                // of "5.00 Internal Server Error" MUST be returned.
                //
                // See :
                // http://www.openmobilealliance.org/release/LightweightM2M/V1_1_1-20190617-A/HTML-Version/OMA-TS-LightweightM2M_Transport-V1_1_1-20190617-A.html#6-7-0-67-Response-Codes
                response = toSendableResponse(BootstrapWriteResponse.internalServerError("object not supported"));
            } else {
                response = toSendableResponse(objectEnabler.write(sender, request));
            }
        }

        @Override
        public void visit(BootstrapReadRequest request) {
            LwM2mObjectEnabler objectEnabler = getObjectEnabler(request);
            if (objectEnabler == null) {
                response = toSendableResponse(BootstrapReadResponse.notFound());
            } else {
                response = toSendableResponse(objectEnabler.read(sender, request));
            }
        }

        @Override
        public void visit(BootstrapDeleteRequest request) {
            if (request.getPath().isRoot()) {
                response = toSendableResponse(bootstrapHandler.delete(sender, request));
            } else {
                LwM2mObjectEnabler objectEnabler = getObjectEnabler(request);
                if (objectEnabler == null) {
                    // Bootstrap delete operation does not support Not Found Response code :
                    // See
                    // http://www.openmobilealliance.org/release/LightweightM2M/V1_1_1-20190617-A/HTML-Version/OMA-TS-LightweightM2M_Transport-V1_1_1-20190617-A.html#Table-642-1-Operation-to-Method-and-URI-Mapping-Bootstrap-Interface
                    response = toSendableResponse(BootstrapDeleteResponse.badRequest("not found"));
                } else {
                    response = toSendableResponse(objectEnabler.delete(sender, request));
                }
            }
        }

        @Override
        public void visit(BootstrapFinishRequest request) {
            response = bootstrapHandler.finished(sender, request);

        }

        private <R extends LwM2mResponse> SendableResponse<R> toSendableResponse(R response) {
            return new SendableResponse<R>(response);
        }

        private LwM2mObjectEnabler getObjectEnabler(SimpleDownlinkRequest<?> request) {
            // TODO TL : handle path has no object id
            LwM2mPath path = request.getPath();
            return objectTree.getObjectEnabler(path.getObjectId());
        }

        @SuppressWarnings("unchecked")
        public SendableResponse<T> getResponse() {
            return (SendableResponse<T>) response;
        }
    }

    public class ErrorResponseCreator<T extends LwM2mResponse> implements DownlinkRequestVisitor {

        private final ResponseCode code;
        private final String errorMessage;
        private LwM2mResponse response;

        public ErrorResponseCreator(ResponseCode code, String errorMessage) {
            this.code = code;
            this.errorMessage = errorMessage;
        }

        @Override
        public void visit(ReadRequest request) {
            response = new ReadResponse(code, null, errorMessage);
        }

        @Override
        public void visit(DiscoverRequest request) {
            response = new DiscoverResponse(code, null, errorMessage);
        }

        @Override
        public void visit(WriteRequest request) {
            response = new WriteResponse(code, errorMessage);
        }

        @Override
        public void visit(WriteAttributesRequest request) {
            response = new WriteAttributesResponse(code, errorMessage);
        }

        @Override
        public void visit(ExecuteRequest request) {
            response = new ExecuteResponse(code, errorMessage);
        }

        @Override
        public void visit(CreateRequest request) {
            response = new CreateResponse(code, null, errorMessage);
        }

        @Override
        public void visit(DeleteRequest request) {
            response = new DeleteResponse(code, errorMessage);
        }

        @Override
        public void visit(ObserveRequest request) {
            response = new ObserveResponse(code, null, null, null, errorMessage);
        }

        @Override
        public void visit(CancelObservationRequest request) {
            // TODO TL :we should check if this is really handle
            response = new CancelObservationResponse(code, null, null, null, errorMessage);
        }

        @Override
        public void visit(ReadCompositeRequest request) {
            response = new ReadCompositeResponse(code, null, errorMessage, null);
        }

        @Override
        public void visit(ObserveCompositeRequest request) {
            response = new ObserveCompositeResponse(code, null, errorMessage, null, null);
        }

        @Override
        public void visit(CancelCompositeObservationRequest request) {
            // TODO TL : we should check if this is really handle
            response = new CancelCompositeObservationResponse(code, null, errorMessage, null, null);
        }

        @Override
        public void visit(WriteCompositeRequest request) {
            response = new WriteCompositeResponse(code, errorMessage, null);
        }

        @Override
        public void visit(BootstrapDiscoverRequest request) {
            response = new BootstrapDiscoverResponse(code, null, errorMessage);
        }

        @Override
        public void visit(BootstrapWriteRequest request) {
            response = new BootstrapWriteResponse(code, errorMessage);
        }

        @Override
        public void visit(BootstrapReadRequest request) {
            response = new BootstrapReadResponse(code, null, errorMessage);
        }

        @Override
        public void visit(BootstrapDeleteRequest request) {
            response = new BootstrapDeleteResponse(code, errorMessage);
        }

        @Override
        public void visit(BootstrapFinishRequest request) {
            response = new BootstrapFinishResponse(code, errorMessage);

        }

        @SuppressWarnings("unchecked")
        public SendableResponse<T> getResponse() {
            return (SendableResponse<T>) response;
        }
    }
}
