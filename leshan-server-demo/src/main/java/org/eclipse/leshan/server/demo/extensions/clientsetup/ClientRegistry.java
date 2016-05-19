/*******************************************************************************
 * Copyright (c) 2016 Bosch Software Innovations GmbH and others.
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
 *    Achim Kraus (Bosch Software Innovations GmbH) - Initial contribution
 ******************************************************************************/
package org.eclipse.leshan.server.demo.extensions.clientsetup;

import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import org.eclipse.leshan.server.client.ClientUpdate;
import org.eclipse.leshan.server.demo.extensions.ExtensionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client registry to setup clients.
 */
public class ClientRegistry implements ClientRegistryListener {

    private static final Logger LOG = LoggerFactory.getLogger(ClientRegistry.class);

    /**
     * Map of client setups.
     */
    private final ConcurrentHashMap<String, BaseClientSetup> clients = new ConcurrentHashMap<String, BaseClientSetup>();
    /**
     * Client setup configurations. Read from {@link ClientSetupExtension#CONFIG_FILE}.
     */
    private final ClientSetupConfig[] configurations;

    private volatile boolean enabled;

    /**
     * Create client registry to setup client.
     * 
     * @param configurations Client setup configurations
     */
    public ClientRegistry(ClientSetupConfig[] configurations) {
        this.configurations = configurations;
    }

    /**
     * Create a client setup instance. Search the list of {@link ClientSetupConfig} for a configuration, which
     * {@link ClientSetupConfig#endpointName} matches or a {@link ClientSetupConfig#instanceUri} is contained in the
     * registration.
     * 
     * @param client client to create a client setup.
     * @return client setup, if configuration could be found, null, otherwise.
     */
    private BaseClientSetup createClientSetup(final Client client) {
        ClientSetupConfig config = null;
        for (ClientSetupConfig configuration : configurations) {
            if (null != configuration.stateClass) {
                if (ExtensionConfig.isDefined(configuration.endpointName)) {
                    if (configuration.endpointName.equals(client.getEndpoint())) {
                        config = configuration;
                        break;
                    }
                } else if (ExtensionConfig.isDefined(configuration.instanceUri)) {
                    if (BaseClientSetup.hasAvailableInstances(client, configuration.instanceUri)) {
                        config = configuration;
                        break;
                    }
                }
            }
        }
        if (null != config) {
            try {
                BaseClientSetup state = (BaseClientSetup) config.stateClass.newInstance();
                state.setConfiguration(client, config);
                LOG.info("create client initializer {} for {}", config.name, state.getEndpoint());
                return state;
            } catch (InstantiationException e) {
            } catch (IllegalAccessException e) {
            }
        }
        return null;
    }

    @Override
    public void registered(final Client client) {
        if (enabled) {
            BaseClientSetup state = createClientSetup(client);
            if (null != state) {
                String enpoint = state.getEndpoint();
                LOG.info("register client {}", enpoint);
                clients.put(enpoint, state);
                synchronized (this) {
                    notify();
                }
            }
        }
    }

    @Override
    public void updated(ClientUpdate update, Client clientUpdated) {
        // TODO: when the address/port is changed, it may be required to reestablish the observes.
    }

    @Override
    public void unregistered(Client client) {
        String endpoint = client.getEndpoint();
        if (null != clients.remove(endpoint)) {
            LOG.info("unregister client {}", endpoint);
        }
    }

    /**
     * Frequently check, if a client is in a state to execute some setup jobs.
     * 
     * @param server LWM2M server to be sued for setup requests.
     */
    public void loop(final LeshanServer server) {
        while (true) {
            int sz = clients.size();
            if (0 < sz) {
                LOG.info("process XDKs " + sz);
                for (BaseClientSetup state : clients.values()) {
                    String endpoint = state.getEndpoint();
                    Client client = server.getClientRegistry().get(endpoint);
                    if (null == client || state.process(server, client))
                        clients.remove(endpoint, state);
                }
            }
            synchronized (this) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void start() {
        enabled = true;
    }

    public void stop() {
        enabled = false;
    }

}
