<template>
  <v-data-table
    :headers="headers"
    :items="clientConfigs"
    item-key="endpoint"
    sort-by="endpoint"
    class="elevation-0"
    dense
  >
    <template v-slot:top>
      <v-toolbar flat>
        <v-toolbar-title>Clients Configuration</v-toolbar-title>
        <v-divider class="mx-4" inset vertical></v-divider>
        <v-spacer></v-spacer>
        <!-- add clients configuration button-->
        <v-btn dark class="mb-2" @click.stop="dialogOpened = true">
          Add Clients Configuration
          <v-icon right dark>
            mdi-key-plus
          </v-icon>
        </v-btn>

        <!-- add client configuration dialog -->
        <client-config-dialog
          v-model="dialogOpened"
          @add="addConfig($event)"
          :initialValue="editedSecurityInfo"
        />
      </v-toolbar>
    </template>
    <!--custom display for "dm" column-->
    <template v-slot:item.dm="{ item }">
      <div v-for="server in item.dm" :key="server.shortid">
        <p>
          <strong>{{server.security.uri }}</strong><br />
          security mode : {{server.security.securityMode}}<br />
          <span v-if="server.security.certificateUsage">
            certificate usage : {{server.security.certificateUsage}}<br />
          </span>
        </p>
      </div>
    </template>
    <!--custom display for "bs" column-->
    <template v-slot:item.bs="{ item }">
        <div v-for="server in item.bs" :key="server.security.uri">
        <p>
          <strong>{{server.security.uri }}</strong><br />
          security mode : {{server.security.securityMode}}<br />
          <span v-if="server.security.certificateUsage">
            certificate usage : {{server.security.certificateUsage}}<br />
          </span>
        </p>
        </div>
    </template>
    <!--custom display for "actions" column-->
    <template v-slot:item.actions="{ item }">
      <v-icon small @click="deleteConfig(item)">
        mdi-delete
      </v-icon>
    </template>
  </v-data-table>
</template>
<script>
import { configsFromRestToUI, configFromUIToRest } from "../js/bsconfigutil.js";
import ClientConfigDialog from "../components/ClientConfigDialog.vue";

export default {
  components: { ClientConfigDialog },
  data: () => ({
    dialogOpened: false,
    headers: [
      { text: "Endpoint", value: "endpoint", width: "20%" },
      { text: "LWM2M Server", value: "dm", width: "40%" },
      { text: "LWM2M Bootstrap Server", value: "bs", width: "40%" },
      { text: "Actions", value: "actions", sortable: false },
    ],
    clientConfigs: [],
    editedSecurityInfo: {}, // initial value for Security Information dialog
  }),

  beforeMount() {
    this.axios
      .get("api/bootstrap")
      .then(
        (response) => (this.clientConfigs = configsFromRestToUI(response.data))
      )
      .catch((error) => console.log(error));
  },

  methods: {
    fromAscii(ascii) {
      var bytearray = [];
      for (var i in ascii) {
        bytearray[i] = ascii.charCodeAt(i);
      }
      return bytearray;
    },
    fromHex(hex) {
      var bytes = [];
      for (var i = 0; i < hex.length - 1; i += 2) {
        bytes.push(parseInt(hex.substr(i, 2), 16));
      }
      return bytes;
    },
    formatData(c) {
      let s = {};
      s.securityMode = c.mode.toUpperCase();
      s.uri = c.url;
      switch (c.mode) {
        case "psk":
          s.publicKeyOrId = this.fromAscii(c.details.identity);
          s.secretKey = this.fromHex(c.details.key);
          break;
        case "rpk":
          s.publicKeyOrId = this.fromHex(c.details.client_pub_key);
          s.secretKey = this.fromHex(c.details.client_pri_key);
          s.serverPublicKey = this.fromHex(c.details.server_pri_key);
          break;
        case "x509":
          s.publicKeyOrId = this.fromHex(c.details.client_certificate);
          s.secretKey = this.fromHex(c.details.client_pri_key);
          s.serverPublicKey = this.fromHex(c.details.server_certificate);
          s.certificateUsage = c.certificate_usage;
          break;
      }
      return s;
    },
    addConfig(config) {
      let dmServer = this.formatData(config.dm);
      let bsServer = this.formatData(config.bs);
      let c = {
        endpoint: config.endpoint,
        dm: [
          {
            binding: "U",
            defaultMinPeriod: 1,
            lifetime: 300,
            notifIfDisabled: true,
            shortId: 123,
            security: {
              bootstrapServer: false,
              certificateUsage: dmServer.certificateUsage,
              clientOldOffTime: 1,
              publicKeyOrId: dmServer.publicKeyOrId,
              secretKey: dmServer.secretKey,
              securityMode: dmServer.securityMode,
              serverId: 123,
              serverPublicKey: dmServer.serverPublicKey,
              serverSmsNumber: "",
              smsBindingKeyParam: [],
              smsBindingKeySecret: [],
              smsSecurityMode: "NO_SEC",
              uri: dmServer.uri,
            },
          },
        ],
        bs: [
          {
            security: {
              bootstrapServer: true,
              certificateUsage: bsServer.certificateUsage,
              clientOldOffTime: 1,
              publicKeyOrId: bsServer.publicKeyOrId,
              secretKey: bsServer.secretKey,
              securityMode: bsServer.securityMode,
              serverPublicKey: bsServer.serverPublicKey,
              serverSmsNumber: "",
              smsBindingKeyParam: [],
              smsBindingKeySecret: [],
              smsSecurityMode: "NO_SEC",
              uri: bsServer.uri,
            },
          },
        ],
      };
      this.axios
        .post(
          "api/bootstrap/" + encodeURIComponent(config.endpoint),
          configFromUIToRest(c)
        )
        .then(() => {
          let index = this.clientConfigs.findIndex(
            (c) => c.endpoint == config.endpoint
          );
          if (index != -1) {
            this.clientConfigs[index] = c;
          } else {
            this.clientConfigs.push(c);
          }
          this.dialogOpened = false;
        })
        .catch((error) => {
          console.log(error);
          this.$dialog.message.error(
            error.response ? error.response.data : error.toString(),
            {
              position: "bottom",
              timeout: 5000,
            }
          );
        });
    },
    deleteConfig(config) {
      this.indexToRemove = this.clientConfigs.indexOf(config);
      this.axios
        .delete("api/bootstrap/" + encodeURIComponent(config.endpoint))
        .then(() => {
          this.clientConfigs.splice(this.indexToRemove, 1);
        })
        .catch((error) => {
          console.log(error);
          this.$dialog.message.error(
            error.response ? error.response.data : error.toString(),
            {
              position: "bottom",
              timeout: 5000,
            }
          );
        });
    },
  },
};
</script>
