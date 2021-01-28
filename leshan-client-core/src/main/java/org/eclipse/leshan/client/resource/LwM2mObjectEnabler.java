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
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.Startable;
import org.eclipse.leshan.core.Stoppable;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
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

    CreateResponse create(ServerIdentity identity, CreateRequest request);

    ReadResponse read(ServerIdentity identity, ReadRequest request);

    WriteResponse write(ServerIdentity identity, WriteRequest request);

    BootstrapWriteResponse write(ServerIdentity identity, BootstrapWriteRequest request);

    DeleteResponse delete(ServerIdentity identity, DeleteRequest request);

    BootstrapDeleteResponse delete(ServerIdentity identity, BootstrapDeleteRequest request);

    ExecuteResponse execute(ServerIdentity identity, ExecuteRequest request);

    WriteAttributesResponse writeAttributes(ServerIdentity identity, WriteAttributesRequest request);

    DiscoverResponse discover(ServerIdentity identity, DiscoverRequest request);

    BootstrapDiscoverResponse discover(ServerIdentity identity, BootstrapDiscoverRequest request);

    ObserveResponse observe(ServerIdentity identity, ObserveRequest request);

    void addListener(ObjectListener listener);

    void removeListener(ObjectListener listener);

    void setLwM2mClient(LwM2mClient client);

    void beginTransaction(byte level);

    void endTransaction(byte level);

    ContentFormat getDefaultEncodingFormat(DownlinkRequest<?> request);
}
