<template>
  <v-data-table
    dense
    :loading="loading"
    :headers="headers"
    :items="registrations"
    item-key="endpoint"
    :items-per-page="10"
    class="elevation-0 fill-height ma-3"
    @click:row="openLink"
  >
    <!-- custom display for date column -->
    <template v-slot:item.registrationDate="{ item }">
      {{ new Date(item.registrationDate) | moment("MMM D, h:mm:ss A") }}
    </template>
    <template v-slot:item.lastUpdate="{ item }">
      {{ new Date(item.lastUpdate) | moment("MMM D, h:mm:ss A") }}
    </template>
    <template v-slot:item.infos="{ item }">
      <client-info :registration="item" tooltipleft/>
    </template>
  </v-data-table>
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
  }),
  methods: {
    openLink(reg) {
      this.$router.replace(`/clients/${reg.endpoint}/3`);
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
