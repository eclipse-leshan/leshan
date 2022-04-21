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
  <div>
    <v-data-table
      dense
      v-if="!loading"
      :headers="headers"
      :items="registrations"
      item-key="endpoint"
      :items-per-page="10"
      class="elevation-0 fill-height ma-3"
      @click:row="openLink"
      :search="search"
      sort-by="registrationDate"
      sort-desc
    >
      <template v-slot:top>
        <v-toolbar flat>
          <v-toolbar-title v-if="$vuetify.breakpoint.smAndUp"
            >Registered Clients</v-toolbar-title
          >
          <v-divider
            v-if="$vuetify.breakpoint.smAndUp"
            class="mx-4"
            inset
            vertical
          ></v-divider>
          <v-text-field
            v-model="search"
            :append-icon="$icons.mdiMagnify"
            label="Search"
            single-line
            hide-details
            class="pa-2"
            clearable
          ></v-text-field>
        </v-toolbar>
      </template>
      <!-- custom display for date column -->
      <template v-slot:item.registrationDate="{ item }">
        {{ new Date(item.registrationDate) | moment("MMM D, h:mm:ss A") }}
      </template>
      <template v-slot:item.lastUpdate="{ item }">
        {{ new Date(item.lastUpdate) | moment("MMM D, h:mm:ss A") }}
      </template>
      <template v-slot:item.infos="{ item }">
        <client-info :registration="item" tooltipleft />
      </template>
    </v-data-table>
  </div>
</template>

<script>
import ClientInfo from "../components/ClientInfo.vue";

export default {
  components: { ClientInfo },
  useSSE: true,
  name: "Clients",
  data: () => ({
    loading: true,
    registrations: [],
    headers: [
      { text: "Client Endpoint", value: "endpoint" },
      { text: "Registration ID", value: "registrationId" },
      { text: "Registration Date", value: "registrationDate" },
      { text: "Last Update", value: "lastUpdate" },
      { text: "", value: "infos", sortable: false, align: "end" },
    ],
    search: "",
  }),
  methods: {
    openLink(reg) {
      this.$router.push(`/clients/${reg.endpoint}/3`);
    },
  },
  mounted() {
    // listen events to update registration.
    this.sse = this.$sse
      .create({ url: "api/event" })
      .on("REGISTRATION", (reg) => {
        this.registrations = this.registrations
          .filter((r) => reg.endpoint !== r.endpoint)
          .concat(reg);
      })
      .on("UPDATED", (msg) => {
        let reg = msg.registration;
        this.registrations = this.registrations
          .filter((r) => reg.registrationId !== r.registrationId)
          .concat(reg);
      })
      .on("DEREGISTRATION", (reg) => {
        this.registrations = this.registrations.filter(
          (r) => reg.registrationId !== r.registrationId
        );
      })
      .on("SLEEPING", (reg) => {
        for (var i = 0; i < this.registrations.length; i++) {
          if (this.registrations[i].endpoint === reg.ep) {
            this.registrations[i].sleeping = true;
          }
        }
      })
      .on("error", (err) => {
        console.error("sse unexpected error", err);
      });
    this.sse.connect().catch((err) => {
      console.error("Failed to connect to server", err);
    });

    // get all registrations
    this.axios
      .get("api/clients")
      .then(
        (response) => (
          (this.loading = false), (this.registrations = response.data)
        )
      );
  },
  beforeDestroy() {
    // close eventsource on destroy
    this.sse.disconnect();
  },
};
</script>
