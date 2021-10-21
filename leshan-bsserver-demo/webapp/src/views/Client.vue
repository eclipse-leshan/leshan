<!-----------------------------------------------------------------------------
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
  ----------------------------------------------------------------------------->
<template>
  <v-card elevation="0" >
    <v-card-text>
      <v-card-title class="justify-center"> Client "{{ $route.params.endpoint }}" </v-card-title>

      <v-card-subtitle class="text-center" >
        Watch your client bootstrap session.

        <v-btn icon title="Clear timeline" @click="events = []">
          <v-icon>mdi-delete-sweep-outline</v-icon>
        </v-btn>
      </v-card-subtitle>

      <v-timeline align-top>
        <v-timeline-item
          v-for="event in events"
          :key="event.id"
          :color="event.color"
          small
          :large="event.large"
          right
          :icon="event.icon"
        >
          <template v-slot:opposite>
            <span>{{
              new Date(event.time) | moment("MMM D, h:mm:ss.SSS A")
            }}</span>
          </template>
          <div>
            <div class="font-weight-normal">
              <strong>{{ event.name }}</strong>
            </div>
            <div style="white-space: pre">{{ event.message }}</div>
          </div>
        </v-timeline-item>
      </v-timeline>
      <div ref="end"></div>
    </v-card-text>
  </v-card>
</template>
<script>
export default {
  data: () => ({
    events: [
      // name : string
      // time : timestamp
      // message : string
      // color : color
      // left: boolean
      // icon: string (mid-*)
    ],
    nonce: 0,
  }),

  mounted() {
    this.sse = this.$sse
      .create({
        url: "api/event",
      })
      .on("BSSESSION", (event) => {
        event.id = this.nonce++;
        event.large = this.islarge(event);
        event.icon = this.getIcon(event);
        event.color = this.getColor(event);
        this.events.push(event);
        this.$nextTick(function() {
          this.$vuetify.goTo(this.$refs.end);
        });
      })
      .on("error", (err) => {
        console.error("sse unexpected error", err);
      });

    this.sse.connect().catch((err) => {
      console.error("Failed to connect to server", err);
    });
  },

  methods: {
    islarge(event) {
      let large = ["new session", "finished", "failed", "unauthorized"];
      return !!large.find((e) => event.name.startsWith(e));
    },

    getIcon(event) {
      let icons = {
        "new session": "mdi-play",
        unauthorized: "mdi-account-cancel-outline",
        "no config": "mdi-database-remove",
        authorized: "mdi-account-check-outline",
        "send write": "mdi-lead-pencil",
        "send delete": "mdi-delete-outline",
        "send discover": "mdi-magnify",
        "receive success response": "mdi-check",
        "receive error response": "mdi-alert-outline",
        finished: "mdi-check-bold",
        "request failure": "mdi-close-circle-outline",
        failed: "mdi-exclamation-thick",
      };
      return icons[event.name];
    },

    getColor(event) {
      let colors = {
        "no config": "red",
        unauthorized: "red",
        authorized: "green",
        "receive success response": "green",
        "receive error response": "orange",
        "request failure": "red",
        finished: "green",
        failed: "red",
      };
      return colors[event.name];
    },
  },
};
</script>
