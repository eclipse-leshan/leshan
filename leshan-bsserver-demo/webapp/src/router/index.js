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
import Bootstrap from "../views/Bootstrap.vue";
import About from "../views/About.vue";
import Client from "../views/Client.vue";
import Server from "@leshan-server-core-demo/views/Server.vue";

Vue.use(VueRouter);

const routes = [
  {
    path: "/",
    redirect: "/bootstrap",
  },
  {
    path: "/bootstrap",
    name: "Bootstrap",
    component: Bootstrap,
  },
  {
    path: "/bootstrap/:endpoint",
    component: Client,
  },
  {
    path: "/server",
    name: "Server",
    component: Server,
    props: { pubkeyFileName: "bsServerPubKey.der", certFileName: "bsServerCertificate.der" },
  },
  {
    path: "/about",
    name: "About",
    component: About,
  },
];

const router = new VueRouter({
  routes,
});

export default router;
