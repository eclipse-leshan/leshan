/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import java.util.List;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.leshan.client.LwM2mClient;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.resource.listener.ObjectListener;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.LwM2mModel;
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

public class TestObjectsInitializer extends ObjectsInitializer {

    public TestObjectsInitializer() {
        super();
    }

    public TestObjectsInitializer(LwM2mModel model) {
        super(model);
    }

    @Override
    protected LwM2mObjectEnabler createNodeEnabler(ObjectModel objectModel) {
        final LwM2mObjectEnabler nodeEnabler = super.createNodeEnabler(objectModel);
        return new LwM2mObjectEnabler() {

            @Override
            public WriteAttributesResponse writeAttributes(ServerIdentity identity, WriteAttributesRequest request) {
                if (!identity.isSystem())
                    assertThat(request.getCoapRequest(), instanceOf(Request.class));
                return nodeEnabler.writeAttributes(identity, request);
            }

            @Override
            public BootstrapWriteResponse write(ServerIdentity identity, BootstrapWriteRequest request) {
                if (!identity.isSystem())
                    assertThat(request.getCoapRequest(), instanceOf(Request.class));
                return nodeEnabler.write(identity, request);
            }

            @Override
            public WriteResponse write(ServerIdentity identity, WriteRequest request) {
                if (!identity.isSystem())
                    assertThat(request.getCoapRequest(), instanceOf(Request.class));
                return nodeEnabler.write(identity, request);
            }

            @Override
            public void setLwM2mClient(LwM2mClient client) {
                nodeEnabler.setLwM2mClient(client);
            }

            @Override
            public void removeListener(ObjectListener listener) {
                nodeEnabler.addListener(listener);
            }

            @Override
            public ReadResponse read(ServerIdentity identity, ReadRequest request) {
                if (!identity.isSystem())
                    assertThat(request.getCoapRequest(), instanceOf(Request.class));
                return nodeEnabler.read(identity, request);
            }

            @Override
            public ObserveResponse observe(ServerIdentity identity, ObserveRequest request) {
                if (!identity.isSystem())
                    assertThat(request.getCoapRequest(), instanceOf(Request.class));
                return nodeEnabler.observe(identity, request);
            }

            @Override
            public ObjectModel getObjectModel() {
                return nodeEnabler.getObjectModel();
            }

            @Override
            public int getId() {
                return nodeEnabler.getId();
            }

            @Override
            public ContentFormat getDefaultEncodingFormat(DownlinkRequest<?> request) {
                return nodeEnabler.getDefaultEncodingFormat(request);
            }

            @Override
            public List<Integer> getAvailableResourceIds(int instanceId) {
                return nodeEnabler.getAvailableResourceIds(instanceId);
            }

            @Override
            public List<Integer> getAvailableInstanceIds() {
                return nodeEnabler.getAvailableInstanceIds();
            }

            @Override
            public ExecuteResponse execute(ServerIdentity identity, ExecuteRequest request) {
                if (!identity.isSystem())
                    assertThat(request.getCoapRequest(), instanceOf(Request.class));
                return nodeEnabler.execute(identity, request);
            }

            @Override
            public BootstrapDiscoverResponse discover(ServerIdentity identity, BootstrapDiscoverRequest request) {
                if (!identity.isSystem())
                    assertThat(request.getCoapRequest(), instanceOf(Request.class));
                return nodeEnabler.discover(identity, request);
            }

            @Override
            public DiscoverResponse discover(ServerIdentity identity, DiscoverRequest request) {
                if (!identity.isSystem())
                    assertThat(request.getCoapRequest(), instanceOf(Request.class));
                return nodeEnabler.discover(identity, request);
            }

            @Override
            public BootstrapDeleteResponse delete(ServerIdentity identity, BootstrapDeleteRequest request) {
                if (!identity.isSystem())
                    assertThat(request.getCoapRequest(), instanceOf(Request.class));
                return nodeEnabler.delete(identity, request);
            }

            @Override
            public DeleteResponse delete(ServerIdentity identity, DeleteRequest request) {
                if (!identity.isSystem())
                    assertThat(request.getCoapRequest(), instanceOf(Request.class));
                return nodeEnabler.delete(identity, request);
            }

            @Override
            public CreateResponse create(ServerIdentity identity, CreateRequest request) {
                if (!identity.isSystem())
                    assertThat(request.getCoapRequest(), instanceOf(Request.class));
                return nodeEnabler.create(identity, request);
            }

            @Override
            public void addListener(ObjectListener listener) {
                nodeEnabler.addListener(listener);
            }

            @Override
            public void beginTransaction(byte level) {
                nodeEnabler.beginTransaction(level);
            }

            @Override
            public void endTransaction(byte level) {
                nodeEnabler.endTransaction(level);
            }
        };
    }
}
