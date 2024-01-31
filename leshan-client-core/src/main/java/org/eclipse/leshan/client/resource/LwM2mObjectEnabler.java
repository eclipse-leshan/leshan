/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use ServerIdentity
 *******************************************************************************/
package org.eclipse.leshan.client.resource;

import java.util.List;

import org.eclipse.leshan.client.LwM2mClient;
import org.eclipse.leshan.client.resource.listener.ObjectListener;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.client.util.LinkFormatHelper;
import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.Startable;
import org.eclipse.leshan.core.Stoppable;
import org.eclipse.leshan.core.link.lwm2m.attributes.NotificationAttributeTree;
import org.eclipse.leshan.core.model.ObjectModel;
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

/**
 * This interface should be implemented to be able to add support for a given LWM2M object.
 * <p>
 * Except if you need really more flexibility, most of the time you will not implement this interface directly. You will
 * probably prefer the easier way based on {@link LwM2mInstanceEnabler} and {@link ObjectsInitializer}.
 * <p>
 * In case you really need the flexibility of this interface you should consider to inherit from
 * {@link BaseObjectEnabler}.
 * <p>
 * An instance that implements this interface synchronizes with the lifecycle of the LeshanClient. This means when
 * {@code LeshanClient#destroy()} is called, {@code LwM2mObjectEnabler#destroy()} is also called if it implements the
 * {@link Destroyable} interface. And {@link Startable} ({@code #start()}) and {@link Stoppable} ({@code #stop()}) are
 * also same as this. If you need to restart the instance, please implement {@link Startable} with {@link Stoppable}
 * together.
 */
public interface LwM2mObjectEnabler {

    int getId();

    ObjectModel getObjectModel();

    List<Integer> getAvailableInstanceIds();

    List<Integer> getAvailableResourceIds(int instanceId);

    List<Integer> getAvailableInstanceResourceIds(int instanceId, int multipleResourceId);

    CreateResponse create(LwM2mServer server, CreateRequest request);

    ReadResponse read(LwM2mServer server, ReadRequest request);

    BootstrapReadResponse read(LwM2mServer server, BootstrapReadRequest request);

    WriteResponse write(LwM2mServer server, WriteRequest request);

    BootstrapWriteResponse write(LwM2mServer server, BootstrapWriteRequest request);

    DeleteResponse delete(LwM2mServer server, DeleteRequest request);

    BootstrapDeleteResponse delete(LwM2mServer server, BootstrapDeleteRequest request);

    ExecuteResponse execute(LwM2mServer server, ExecuteRequest request);

    WriteAttributesResponse writeAttributes(LwM2mServer server, WriteAttributesRequest request);

    DiscoverResponse discover(LwM2mServer server, DiscoverRequest request);

    BootstrapDiscoverResponse discover(LwM2mServer server, BootstrapDiscoverRequest request);

    ObserveResponse observe(LwM2mServer server, ObserveRequest request);

    void addListener(ObjectListener listener);

    void removeListener(ObjectListener listener);

    void init(LwM2mClient client, LinkFormatHelper linkFormatHelper);

    void beginTransaction(byte level);

    void endTransaction(byte level);

    ContentFormat getDefaultEncodingFormat(DownlinkRequest<?> request);

    NotificationAttributeTree getAttributesFor(LwM2mServer server);
}
