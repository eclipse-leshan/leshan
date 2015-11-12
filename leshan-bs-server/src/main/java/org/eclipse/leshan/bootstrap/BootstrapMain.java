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

package org.eclipse.leshan.bootstrap;

import java.net.InetSocketAddress;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.leshan.bootstrap.servlet.BootstrapServlet;
import org.eclipse.leshan.server.californium.impl.LwM2mBootstrapServerImpl;
import org.eclipse.leshan.server.security.SecurityStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BootstrapMain {

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapMain.class);

    private static String parseHost(String iface) {
        return iface.substring(0, iface.lastIndexOf(':'));
    }

    private static int parsePort(String iface) {
        return Integer.parseInt(iface.substring(iface.lastIndexOf(':') + 1, iface.length()));
    }

    public static void main(String[] args) {

        BootstrapStoreImpl bsStore = new BootstrapStoreImpl();
        SecurityStore securityStore = new BootstrapSecurityStore(bsStore);

        // use those ENV variables for specifying the interface to be bound for coap and coaps
        String iface = System.getenv("COAPIFACE");
        String ifaces = System.getenv("COAPSIFACE");

        LwM2mBootstrapServerImpl bsServer;

        if (iface == null || iface.isEmpty() || ifaces == null || ifaces.isEmpty()) {
            bsServer = new LwM2mBootstrapServerImpl(bsStore, securityStore);
        } else {
            // user specified the iface to be bound
            bsServer = new LwM2mBootstrapServerImpl(new InetSocketAddress(parseHost(iface), parsePort(iface)),
                    new InetSocketAddress(parseHost(ifaces), parsePort(ifaces)), bsStore, securityStore);
        }

        bsServer.start();
        // now prepare and start jetty

        String webPort = System.getenv("PORT");

        if (webPort == null || webPort.isEmpty()) {
            webPort = System.getProperty("PORT");
        }

        if (webPort == null || webPort.isEmpty()) {
            webPort = "8080";
        }

        Server server = new Server(Integer.valueOf(webPort));
        WebAppContext root = new WebAppContext();

        root.setContextPath("/");
        // root.setDescriptor(webappDirLocation + "/WEB-INF/web.xml");
        root.setResourceBase(BootstrapMain.class.getClassLoader().getResource("webapp").toExternalForm());

        // root.setResourceBase(webappDirLocation);
        root.setParentLoaderPriority(true);

        ServletHolder bsServletHolder = new ServletHolder(new BootstrapServlet(bsStore));
        root.addServlet(bsServletHolder, "/api/bootstrap/*");

        server.setHandler(root);

        try {
            server.start();
        } catch (Exception e) {
            LOG.error("jetty error", e);
        }

    }
}
