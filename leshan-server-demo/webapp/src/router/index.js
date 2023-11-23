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
 *******************************************************************************/

import Vue from "vue";
import VueRouter from "vue-router";

const Clients = () => import("../views/Clients.vue");
const Client = () => import("../views/Client.vue");
const CompositeOperationView = () =>
  import("../views/CompositeOperationView.vue");
const CompositeObjectView = () => import("../views/CompositeObjectView.vue");
const ObjectView = () => import("../views/ObjectView.vue");

const Security = () => import("../views/Security.vue");
const Server = () => import("@leshan-server-core-demo/views/Server.vue");
const About = () => import("@leshan-server-core-demo/views/About.vue");

Vue.use(VueRouter);

const routes = [
  {
    path: "/",
    redirect: "/clients",
  },
  {
    path: "/clients/:endpoint",
    component: Client,
    children: [
      {
        path: "composite",
        component: CompositeOperationView,
        children: [
          {
            path: ":compositeObjectName",
            component: CompositeObjectView,
          },
        ],
      },
      {
        path: ":objectid",
        component: ObjectView,
      },
    ],
  },
  {
    path: "/clients",
    name: "Clients",
    component: Clients,
  },
  {
    path: "/security",
    name: "Security",
    component: Security,
  },
  {
    path: "/server",
    name: "Server",
    component: Server,
    props: {
      pubkeyFileName: "serverPubKey.der",
      certFileName: "serverCertificate.der",
    },
  },
  {
    path: "/about",
    name: "About",
    component: About,
    props: {
      appName: "LWM2M Server Demo",
    },
  },
];

const router = new VueRouter({
  routes,
});

export default router;
