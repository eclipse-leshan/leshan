<template>
  <div>
    <v-layout justify-center>
      <v-alert
        border="left"
        outlined
        type="info"
        elevation="2"
        icon="mdi-alert-decagram"
        dismissible
        prominent
        class="ma-2"
        v-model="shownews"
      >
        <p>
          This is the new UI for Leshan Server Demo. If needed, you can still
          use the old version which should be available
          <a href="../old/"><strong>here</strong></a> but not for so long as it
          <strong> will be removed very soon !</strong>
        </p>
      </v-alert>
    </v-layout>
    <v-data-table
      dense
      :loading="loading"
      :headers="headers"
      :items="registrations"
      item-key="endpoint"
      :items-per-page="10"
      class="elevation-0 fill-height ma-3"
      @click:row="openLink"
      :search="search"
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
            append-icon="mdi-magnify"
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
import { preference } from "vue-preferences";

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
  computed: {
    shownews: preference("shownews", {
      defaultValue: true,
      ttl: 60 * 60 * 24,
    }),
  },
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
