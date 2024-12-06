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

import { createWebHashHistory, createRouter } from 'vue-router'

const Bootstrap = () => import("../views/BootstrapView.vue");
const About = () => import("@leshan-demo-servers-shared/views/About.vue");
const Client = () => import("../views/ClientView.vue");
const Server = () => import("@leshan-demo-servers-shared/views/Server.vue");

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
    props: {
      pubkeyFileName: "bsServerPubKey.der",
      certFileName: "bsServerCertificate.der",
    },
  },
  {
    path: "/about",
    name: "About",
    component: About,
    props: {
      appName: "LWM2M Bootstrap Server Demo",
    },
  },
];


const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

export default router;
