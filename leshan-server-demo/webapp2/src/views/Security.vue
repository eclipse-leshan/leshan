<template>
  <v-data-table
    :headers="headers"
    :items="securityInfos"
    item-key="endpoint"
    sort-by="endpoint"
    class="elevation-0"
    dense
  >
    <template v-slot:top>
      <v-toolbar flat>
        <v-toolbar-title>Security Information</v-toolbar-title>
        <v-divider class="mx-4" inset vertical></v-divider>
        <v-spacer></v-spacer>
        <!-- add security info button-->
        <v-btn dark class="mb-2" @click.stop="openNewSec()">
          Add Security Information
          <v-icon right dark>
            mdi-key-plus
          </v-icon>
        </v-btn>

        <!-- add/edit security info dialog -->
        <security-info-dialog
          v-model="dialogOpened"
          @new="newSec($event)"
          @edit="editSec($event)"
          :initialValue="editedSecurityInfo"
        />
      </v-toolbar>
    </template>
    <!--custom display for "mode" column-->
    <template v-slot:item.mode="{ item }">
      <v-chip small>
        <v-icon left small>
          {{ getModeIcon(item.mode) }}
        </v-icon>
        {{ item.mode }}
      </v-chip>
    </template>
    <!--custom display for "details" column-->
    <template v-slot:item.details="{ item }">
      <div v-if="item.mode == 'psk'" style="word-break:break-all;" class="pa-1">
        <strong>Identity:</strong> <code>{{ item.details.identity }}</code>
        <br />
        <strong>Key:</strong><code class="text-uppercase">{{ item.details.key }}</code>
      </div>
      <div v-if="item.mode == 'rpk'" style="word-break:break-all;" class="pa-1">
        <strong>Public Key:</strong> <code class="text-uppercase">{{ item.details.key }}</code>
      </div>
      <div
        v-if="item.mode == 'x509'"
        style="word-break:break-all;"
        class="pa-1"
      >
        <strong>X509 certificate with CN equals :</strong>
        <code>{{ item.endpoint }}</code>
      </div>
    </template>
    <!--custom display for "actions" column-->
    <template v-slot:item.actions="{ item }">
      <v-icon
        small
        class="mr-2"
        @click.stop="openEditSec(item)"
        :disabled="item.mode == 'unsupported'"
      >
        mdi-pencil
      </v-icon>
      <v-icon small @click="deleteSec(item)">
        mdi-delete
      </v-icon>
    </template>
  </v-data-table>
</template>
<script>
import SecurityInfoDialog from "../components/security/SecurityInfoDialog.vue";

export default {
  components: { SecurityInfoDialog },
  data: () => ({
    dialogOpened: false,
    headers: [
      { text: "Endpoint", value: "endpoint" },
      { text: "Security mode", value: "mode" },
      { text: "Details", value: "details", sortable: false, width: "60%" },
      { text: "Actions", value: "actions", sortable: false },
    ],
    securityInfos: [],
    editedSecurityInfo: {}, // initial value for Security Information dialog
  }),

  beforeMount() {
    this.axios
      .get("api/security/clients")
      .then(
        (response) =>
          (this.securityInfos = response.data.map((c) => {
            return this.adaptToUI(c);
          }))
      )
      .catch((error) => console.log(error));
  },

  methods: {
    adaptToUI(sec) {
      // TODO this is a bit tricky, probably better to adapt the REST API
      // But do not want to change it while we have 2 demo UI (old & new)
      let s = {};
      s.endpoint = sec.endpoint;
      s.mode = this.getMode(sec);
      if (s.mode != "unsupported" && s.mode != "x509")
        s.details = sec[s.mode];
      return s;
    },
    adaptToAPI(sec) {
      // TODO this is a bit tricky, probably better to adapt the REST API
      // But do not want to change it while we have 2 demo UI (old & new)
      let s = {};
      s.endpoint = sec.endpoint;
      if (sec.mode == "x509") {
        s[sec.mode] = true;
      } else if (sec.mode != "unsupported") {
        s[sec.mode] = sec.details;
      }
      return s;
    },

    getMode(sec) {
      if (sec.x509) return "x509";
      else if (sec.psk) return "psk";
      else if (sec.rpk) return "rpk";
      else return "unsupported";
    },

    getModeIcon(mode) {
      switch (mode) {
        case "x509":
          return "mdi-certificate";
        case "psk":
          return "mdi-lock";
        case "rpk":
          return "mdi-key-change";
        default:
          return "mdi-help-rhombus-outline";
      }
    },

    openNewSec() {
      this.editedSecurityInfo = null;
      this.dialogOpened = true;
    },

    newSec(cred) {
      this.axios
        .put("api/security/clients/", this.adaptToAPI(cred))
        .then(() => {
          this.securityInfos.push(cred);
          this.dialogOpened = false;
        })
        .catch((error) => {
          console.log(error);
          this.$dialog.message.error(error.response ? error.response.data : error.toString(), {
            position: "bottom",
            timeout: 5000,
          });
        });
    },

    openEditSec(sec) {
      this.editedSecurityInfo = sec;
      this.dialogOpened = true;
    },

    editSec(sec) {
      this.axios
        .put("api/security/clients/", this.adaptToAPI(sec))
        .then(() => {
          this.securityInfos = this.securityInfos.map((s) =>
            s.endpoint == sec.endpoint ? sec : s
          );
          this.dialogOpened = false;
        })
        .catch((error) => {
          console.log(error);
          this.$dialog.message.error(error.response ? error.response.data : error.toString(), {
            position: "bottom",
            timeout: 5000,
          });
        });
    },

    deleteSec(sec) {
      this.indexToRemove = this.securityInfos.indexOf(sec);
      this.axios
        .delete(
          "api/security/clients/" + encodeURIComponent(sec.endpoint)
        )
        .then(() => {
          this.securityInfos.splice(this.indexToRemove, 1);
        })
        .catch((error) => {
          console.log(error);
          this.$dialog.message.error(error.response ? error.response.data : error.toString(), {
            position: "bottom",
            timeout: 5000,
          });
        });
    },
  },
};
</script>
