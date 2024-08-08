/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.javacoap.client.resource;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletableFuture;

import org.eclipse.leshan.client.endpoint.ClientEndpointToolbox;
import org.eclipse.leshan.client.notification.NotificationManager;
import org.eclipse.leshan.client.request.DownlinkRequestReceiver;
import org.eclipse.leshan.client.resource.NotificationSender;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.link.attributes.InvalidAttributeException;
import org.eclipse.leshan.core.link.lwm2m.attributes.InvalidAttributesException;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.node.InvalidLwM2mPathException;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
import org.eclipse.leshan.core.request.BootstrapReadRequest;
import org.eclipse.leshan.core.request.BootstrapWriteRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.BootstrapDeleteResponse;
import org.eclipse.leshan.core.response.BootstrapDiscoverResponse;
import org.eclipse.leshan.core.response.BootstrapReadResponse;
import org.eclipse.leshan.core.response.BootstrapWriteResponse;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.transport.javacoap.client.observe.LwM2mKeys;
import org.eclipse.leshan.transport.javacoap.client.observe.ObserversListener;
import org.eclipse.leshan.transport.javacoap.client.observe.ObserversManager;
import org.eclipse.leshan.transport.javacoap.identity.IdentityHandler;
import org.eclipse.leshan.transport.javacoap.request.ResponseCodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.MediaTypes;
import com.mbed.coap.packet.Opaque;

public class ObjectResource extends LwM2mClientCoapResource {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectResource.class);

    protected DownlinkRequestReceiver requestReceiver;
    protected ClientEndpointToolbox toolbox;
    protected NotificationManager notificationManager;
    protected ObserversManager observersManager;

    public ObjectResource(DownlinkRequestReceiver requestReceiver, String uri, ClientEndpointToolbox toolbox,
            IdentityHandler identityHandler, ServerIdentityExtractor serverIdentityExtractor,
            NotificationManager notificationManager, ObserversManager observersManager) {
        super(uri, identityHandler, serverIdentityExtractor);
        this.requestReceiver = requestReceiver;
        this.toolbox = toolbox;
        this.notificationManager = notificationManager;
        this.observersManager = observersManager;

        this.observersManager.addListener(new ObserversListener() {
            @Override
            public void observersRemoved(CoapRequest coapRequest) {
                // Get object URI
                String URI = coapRequest.options().getUriPath();
                // we don't manage observation on root path
                if (URI == null)
                    return;

                // Get Server identity
                LwM2mServer extractIdentity = extractIdentity(coapRequest);

                // handle content format for Read and Observe Request
                ContentFormat requestedContentFormat = null;
                if (coapRequest.options().getAccept() != null) {
                    // If an request ask for a specific content format, use it (if we support it)
                    requestedContentFormat = ContentFormat.fromCode(coapRequest.options().getAccept());
                }

                // Create Observe request
                ObserveRequest observeRequest = new ObserveRequest(requestedContentFormat, URI, coapRequest);

                // Remove notification data for this request
                notificationManager.clear(extractIdentity, observeRequest);
            }

            @Override
            public void observersAdded(CoapRequest request) {
            }
        });
    }

    @Override
    public CompletableFuture<CoapResponse> handleGET(CoapRequest coapRequest) {
        // Get server identity
        LwM2mServer identity = extractIdentity(coapRequest);
        if (identity == null) {
            return unknownServer();
        }

        // Get object URI
        String URI = coapRequest.options().getUriPath();

        // Handle request
        if (coapRequest.options().getAccept() != null
                && coapRequest.options().getAccept() == MediaTypes.CT_APPLICATION_LINK__FORMAT) {
            if (identity.isLwm2mBootstrapServer()) {
                // Manage Bootstrap Discover Request
                BootstrapDiscoverResponse response = requestReceiver
                        .requestReceived(identity, new BootstrapDiscoverRequest(URI, coapRequest)).getResponse();
                if (response.getCode().isError()) {
                    return errorMessage(response.getCode(), response.getErrorMessage());
                } else {
                    return completedFuture(CoapResponse //
                            .coapResponse(ResponseCodeUtil.toCoapResponseCode(response.getCode())) //
                            .payload(Opaque
                                    .of(toolbox.getLinkSerializer().serializeCoreLinkFormat(response.getObjectLinks())))
                            .contentFormat(MediaTypes.CT_APPLICATION_LINK__FORMAT) //
                            .build());
                }
            } else {
                // Manage Discover Request
                DiscoverResponse response = requestReceiver
                        .requestReceived(identity, new DiscoverRequest(URI, coapRequest)).getResponse();
                if (response.getCode().isError()) {
                    return errorMessage(response.getCode(), response.getErrorMessage());
                } else {
                    return completedFuture(CoapResponse //
                            .coapResponse(ResponseCodeUtil.toCoapResponseCode(response.getCode())) //
                            .payload(Opaque
                                    .of(toolbox.getLinkSerializer().serializeCoreLinkFormat(response.getObjectLinks()))) //
                            .contentFormat(MediaTypes.CT_APPLICATION_LINK__FORMAT) //
                            .build());
                }
            }
        } else {
            // handle content format for Read and Observe Request
            ContentFormat requestedContentFormat = null;
            if (coapRequest.options().getAccept() != null) {
                // If an request ask for a specific content format, use it (if we support it)
                requestedContentFormat = ContentFormat.fromCode(coapRequest.options().getAccept());
                if (!toolbox.getEncoder().isSupported(requestedContentFormat)) {
                    return emptyResponse(ResponseCode.NOT_ACCEPTABLE);
                }
            }

            // Manage Observe Request
            if (coapRequest.options().getObserve() != null) {
                ObserveRequest observeRequest = new ObserveRequest(requestedContentFormat, URI, coapRequest);
                ObserveResponse response = requestReceiver.requestReceived(identity, observeRequest).getResponse();
                if (response.getCode() == ResponseCode.CONTENT) {
                    ContentFormat format = getContentFormat(observeRequest, requestedContentFormat);
                    CompletableFuture<CoapResponse> coapResponse = responseWithPayload( //
                            response.getCode(), //
                            format, //
                            toolbox.getEncoder().encode(response.getContent(), format, null, getPath(URI),
                                    toolbox.getModel()));

                    // store observation relation if this is not a active observe cancellation
                    if (coapRequest.options().getObserve() != 1) {
                        try {
                            notificationManager.initRelation(identity, observeRequest, response.getContent(),
                                    createNotificationSender(coapRequest, identity, observeRequest,
                                            requestedContentFormat));
                        } catch (InvalidAttributesException e) {
                            return errorMessage(ResponseCode.INTERNAL_SERVER_ERROR,
                                    "Invalid Attributes state : " + e.getMessage());
                        }
                    }
                    return coapResponse;
                } else {
                    CompletableFuture<CoapResponse> errorMessage = errorMessage(response.getCode(),
                            response.getErrorMessage());
                    notificationManager.clear(identity, observeRequest);
                    return errorMessage;
                }
            } else if (coapRequest.getTransContext(LwM2mKeys.LESHAN_NOTIFICATION, false)) {
                // Manage Notifications
                ObserveRequest observeRequest = new ObserveRequest(requestedContentFormat, URI, coapRequest);
                notificationManager.notificationTriggered(identity, observeRequest,
                        createNotificationSender(coapRequest, identity, observeRequest, requestedContentFormat));
                return null;
            } else {
                if (identity.isLwm2mBootstrapServer()) {
                    // Manage Bootstrap Read Request
                    BootstrapReadRequest readRequest = new BootstrapReadRequest(requestedContentFormat, URI,
                            coapRequest);
                    BootstrapReadResponse response = requestReceiver.requestReceived(identity, readRequest)
                            .getResponse();
                    if (response.getCode() == ResponseCode.CONTENT) {
                        ContentFormat format = getContentFormat(readRequest, requestedContentFormat);
                        return responseWithPayload( //
                                response.getCode(), //
                                format, //
                                toolbox.getEncoder().encode(response.getContent(), format, null, getPath(URI),
                                        toolbox.getModel()));
                    } else {
                        return errorMessage(response.getCode(), response.getErrorMessage());
                    }
                } else {
                    // Manage Read Request
                    ReadRequest readRequest = new ReadRequest(requestedContentFormat, URI, coapRequest);
                    ReadResponse response = requestReceiver.requestReceived(identity, readRequest).getResponse();
                    if (response.getCode() == ResponseCode.CONTENT) {
                        ContentFormat format = getContentFormat(readRequest, requestedContentFormat);
                        return responseWithPayload( //
                                response.getCode(), //
                                format, //
                                toolbox.getEncoder().encode(response.getContent(), format, null, getPath(URI),
                                        toolbox.getModel()));
                    } else {
                        return errorMessage(response.getCode(), response.getErrorMessage());
                    }
                }
            }
        }

    }

    protected ContentFormat getContentFormat(DownlinkRequest<?> request, ContentFormat requestedContentFormat) {
        if (requestedContentFormat != null) {
            // we already check before this content format is supported.
            return requestedContentFormat;
        }

        // TODO TL : should we keep this feature ?
        // ContentFormat format = nodeEnabler.getDefaultEncodingFormat(request);
        // return format == null ? ContentFormat.DEFAULT : format;

        return ContentFormat.DEFAULT;
    }

    protected LwM2mPath getPath(String URI) throws InvalidRequestException {
        try {
            return new LwM2mPath(URI);
        } catch (InvalidLwM2mPathException e) {
            throw new InvalidRequestException(e, "Invalid path : %s", e.getMessage());
        }
    }

    @Override
    public CompletableFuture<CoapResponse> handlePUT(CoapRequest coapRequest) {
        // Get server identity
        LwM2mServer identity = extractIdentity(coapRequest);
        if (identity == null) {
            return unknownServer();
        }

        // Get object URI
        String URI = coapRequest.options().getUriPath();

        // get Observe Spec
        LwM2mAttributeSet attributes = null;
        if (coapRequest.options().getUriQuery() != null && !coapRequest.options().getUriQuery().isEmpty()) {
            String uriQueries = coapRequest.options().getUriQuery();
            try {
                attributes = new LwM2mAttributeSet(toolbox.getAttributeParser().parseUriQuery(uriQueries));
            } catch (InvalidAttributeException e) {
                return handleInvalidRequest(coapRequest, "Unable to parse Attributes", e);
            }
        }

        // Manage Write Attributes Request
        if (attributes != null) {
            WriteAttributesResponse response = requestReceiver
                    .requestReceived(identity, new WriteAttributesRequest(URI, attributes, coapRequest)).getResponse();
            if (response.getCode().isError()) {
                return errorMessage(response.getCode(), response.getErrorMessage());
            } else {
                return emptyResponse(response.getCode());
            }
        }
        // Manage Write and Bootstrap Write Request (replace)
        else {
            LwM2mPath path = getPath(URI);

            if (coapRequest.options().getContentFormat() == null) {
                return handleInvalidRequest(coapRequest, "Content Format is mandatory");
            }

            ContentFormat contentFormat = ContentFormat.fromCode(coapRequest.options().getContentFormat());
            if (!toolbox.getDecoder().isSupported(contentFormat)) {
                return emptyResponse(ResponseCode.UNSUPPORTED_CONTENT_FORMAT);
            }
            LwM2mNode lwM2mNode;
            try {
                lwM2mNode = toolbox.getDecoder().decode(coapRequest.getPayload().getBytes(), contentFormat, null, path,
                        toolbox.getModel());
                if (identity.isLwm2mBootstrapServer()) {
                    BootstrapWriteResponse response = requestReceiver
                            .requestReceived(identity,
                                    new BootstrapWriteRequest(path, lwM2mNode, contentFormat, coapRequest))
                            .getResponse();
                    if (response.getCode().isError()) {
                        return errorMessage(response.getCode(), response.getErrorMessage());
                    } else {
                        return emptyResponse(response.getCode());
                    }
                } else {
                    WriteResponse response = requestReceiver
                            .requestReceived(identity,
                                    new WriteRequest(Mode.REPLACE, contentFormat, URI, lwM2mNode, coapRequest))
                            .getResponse();
                    if (response.getCode().isError()) {
                        return errorMessage(response.getCode(), response.getErrorMessage());
                    } else {
                        return emptyResponse(response.getCode());
                    }
                }
            } catch (CodecException e) {
                return handleInvalidRequest(coapRequest, "Unable to decode payload on WRITE", e);
            }
        }
    }

    @Override
    public CompletableFuture<CoapResponse> handlePOST(CoapRequest coapRequest) {
        // Get server identity
        LwM2mServer identity = extractIdentity(coapRequest);
        if (identity == null) {
            return unknownServer();
        }

        // Get object URI
        String URI = coapRequest.options().getUriPath();
        LwM2mPath path = getPath(URI);

        // Manage Execute Request
        if (path.isResource()) {
            // execute request has no content format at all or a TEXT content format for parameters.
            if (coapRequest.options().getContentFormat() == null
                    || ContentFormat.fromCode(coapRequest.options().getContentFormat()) == ContentFormat.TEXT) {

                String arguments = coapRequest.getPayload() != null && !coapRequest.getPayload().isEmpty()
                        ? new String(coapRequest.getPayload().getBytes())
                        : null;

                ExecuteResponse response = requestReceiver
                        .requestReceived(identity, new ExecuteRequest(URI, arguments, coapRequest)).getResponse();
                if (response.getCode().isError()) {
                    return errorMessage(response.getCode(), response.getErrorMessage());
                } else {
                    return emptyResponse(response.getCode());
                }
            }
        }

        // handle content format for Write (Update) and Create request
        if (coapRequest.options().getContentFormat() == null) {
            return handleInvalidRequest(coapRequest, "Content Format is mandatory");
        }
        ContentFormat contentFormat = ContentFormat.fromCode(coapRequest.options().getContentFormat());
        if (!toolbox.getDecoder().isSupported(contentFormat)) {
            return emptyResponse(ResponseCode.UNSUPPORTED_CONTENT_FORMAT);
        }

        // manage partial update of multi-instance resource
        if (path.isResource()) {
            try {
                LwM2mNode lwM2mNode = toolbox.getDecoder().decode(coapRequest.getPayload().getBytes(), contentFormat,
                        null, path, toolbox.getModel());
                WriteResponse response = requestReceiver
                        .requestReceived(identity,
                                new WriteRequest(Mode.UPDATE, contentFormat, URI, lwM2mNode, coapRequest))
                        .getResponse();
                if (response.getCode().isError()) {
                    return errorMessage(response.getCode(), response.getErrorMessage());
                } else {
                    return emptyResponse(response.getCode());
                }
            } catch (CodecException e) {
                return handleInvalidRequest(coapRequest, "Unable to decode payload on WRITE", e);
            }
        }
        // Manage Update Instance
        if (path.isObjectInstance()) {
            try {
                LwM2mNode lwM2mNode = toolbox.getDecoder().decode(coapRequest.getPayload().getBytes(), contentFormat,
                        null, path, toolbox.getModel());
                WriteResponse response = requestReceiver
                        .requestReceived(identity,
                                new WriteRequest(Mode.UPDATE, contentFormat, URI, lwM2mNode, coapRequest))
                        .getResponse();
                if (response.getCode().isError()) {
                    return errorMessage(response.getCode(), response.getErrorMessage());
                } else {
                    return emptyResponse(response.getCode());
                }
            } catch (CodecException e) {
                return handleInvalidRequest(coapRequest, "Unable to decode payload on WRITE", e);
            }
        }

        // Manage Create Request
        try {
            // decode the payload as an instance
            Opaque payload = coapRequest.getPayload();
            LwM2mObject object = toolbox.getDecoder().decode(payload.getBytes(), contentFormat, null,
                    new LwM2mPath(path.getObjectId()), toolbox.getModel(), LwM2mObject.class);

            CreateRequest createRequest;
            // check if this is the "special" case where instance ID is not defined ...
            LwM2mObjectInstance newInstance = object.getInstance(LwM2mObjectInstance.UNDEFINED);
            if (object.getInstances().isEmpty()) {
                // This is probably the pretty strange use case where
                // instance ID is not defined an no resources available.
                createRequest = new CreateRequest(contentFormat, coapRequest, URI, new LwM2mResource[0]);
            } else if (object.getInstances().size() == 1 && newInstance != null) {
                // the instance Id was not part of the create request payload.
                // will be assigned by the client.
                createRequest = new CreateRequest(contentFormat, coapRequest, URI, newInstance.getResources().values());
            } else {
                createRequest = new CreateRequest(contentFormat, coapRequest, URI, object.getInstances().values()
                        .toArray(new LwM2mObjectInstance[object.getInstances().values().size()]));
            }

            CreateResponse response = requestReceiver.requestReceived(identity, createRequest).getResponse();
            if (response.getCode() == org.eclipse.leshan.core.ResponseCode.CREATED) {
                CoapResponse coapResponse = CoapResponse.of(ResponseCodeUtil.toCoapResponseCode(response.getCode()));
                if (response.getLocation() != null) {
                    coapResponse.options().setLocationPath(response.getLocation());
                }
                return completedFuture(coapResponse);
            } else {
                return errorMessage(response.getCode(), response.getErrorMessage());
            }
        } catch (CodecException e) {
            return handleInvalidRequest(coapRequest, "Unable to decode payload on CREATE", e);
        }
    }

    @Override
    public CompletableFuture<CoapResponse> handleDELETE(CoapRequest coapRequest) {
        // Get server identity
        LwM2mServer identity = extractIdentity(coapRequest);
        if (identity == null) {
            return unknownServer();
        }

        // Get object URI
        String URI = coapRequest.options().getUriPath();

        // Manage Delete Request
        if (identity.isLwm2mBootstrapServer()) {
            BootstrapDeleteResponse response = requestReceiver
                    .requestReceived(identity, new BootstrapDeleteRequest(URI, coapRequest)).getResponse();
            if (response.getCode().isError()) {
                return errorMessage(response.getCode(), response.getErrorMessage());
            } else {
                return emptyResponse(response.getCode());
            }
        } else {
            DeleteResponse response = requestReceiver.requestReceived(identity, new DeleteRequest(URI, coapRequest))
                    .getResponse();
            if (response.getCode().isError()) {
                return errorMessage(response.getCode(), response.getErrorMessage());
            } else {
                return emptyResponse(response.getCode());
            }
        }
    }

    protected NotificationSender createNotificationSender(CoapRequest coapRequest, LwM2mServer server,
            ObserveRequest observeRequest, ContentFormat requestedContentFormat) {
        return new NotificationSender() {
            @Override
            public boolean sendNotification(ObserveResponse response) {
                try {
                    if (observersManager.contains(coapRequest))
                        if (response.getCode() == ResponseCode.CONTENT) {
                            ContentFormat format = getContentFormat(observeRequest, requestedContentFormat);
                            CompletableFuture<CoapResponse> coapResponse = responseWithPayload( //
                                    response.getCode(), //
                                    format, //
                                    toolbox.getEncoder().encode(response.getContent(), format, null,
                                            getPath(coapRequest.options().getUriPath()), toolbox.getModel()));

                            // store observation relation
                            observersManager.sendObservation(coapRequest, coapResponse);
                            return true;
                        } else {
                            CompletableFuture<CoapResponse> errorMessage = errorMessage(response.getCode(),
                                    response.getErrorMessage());
                            observersManager.sendObservation(coapRequest, errorMessage);
                            return false;
                        }
                    else {
                        return false;
                    }
                } catch (Exception e) {
                    LOG.error("Exception while sending notification [{}] for [{}] to {}", response, observeRequest,
                            server, e);
                    errorMessage(ResponseCode.INTERNAL_SERVER_ERROR, "failure sending notification");
                    return false;
                }
            }
        };
    }
}
