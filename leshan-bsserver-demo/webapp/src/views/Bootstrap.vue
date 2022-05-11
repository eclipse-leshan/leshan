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
      :headers="headers"
      :items="clientConfigs"
      item-key="endpoint"
      sort-by="endpoint"
      class="elevation-0"
      @click:row="openLink"
      dense
      :search="search"
    >
      <template v-slot:top>
        <v-toolbar flat>
          <v-toolbar-title v-if="$vuetify.breakpoint.smAndUp"
            >Clients Configuration</v-toolbar-title
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

          <!-- add clients configuration button-->
          <v-btn dark class="mb-2" @click.stop="dialogOpened = true">
            {{
              $vuetify.breakpoint.smAndDown ? "" : "Add Clients Configuration"
            }}
            <v-icon :right="!$vuetify.breakpoint.smAndDown" dark>
              {{ $icons.mdiKeyPlus }}
            </v-icon>
          </v-btn>

          <!-- add client configuration dialog -->
          <client-config-dialog v-model="dialogOpened" @add="onAdd($event)" />
        </v-toolbar>
      </template>
      <!--custom display for "security" column-->
      <template v-slot:item.security="{ item }">
        <security-info-chip :securityInfo="item.security" />
      </template>
      <!--custom display for "config" column-->
      <template v-slot:item.config="{ item }">
        <div class="pa-2">
          <!-- Path to delete -->
          <span v-if="item.toDelete && item.toDelete.length > 0">
            Delete :
            <span v-for="path in item.toDelete" :key="path">
              <code>{{ path }}</code
              >,
            </span>
            <br />
          </span>
          <span v-if="item.autoIdForSecurityObject"
            >Use Auto ID For Security Object<br
          /></span>
          <!-- LWM2M Server to add -->
          <span v-for="server in item.dm" :key="server.shortid">
            Add Server: <code>{{ server.security.uri }}</code>
            <span v-if="server.security.securityMode.toLowerCase() != 'no_sec'">
              using
              <v-chip small>
                <v-icon left small>
                  {{ modeIcon(server.security.securityMode.toLowerCase()) }}
                </v-icon>
                {{ server.security.securityMode.toLowerCase() }}
              </v-chip>
            </span>
            <br />
          </span>
          <!-- LWM2M Bootstrap Server to add -->
          <span v-for="server in item.bs" :key="server.security.uri">
            Add Bootstrap Server: <code>{{ server.security.uri }}</code>
            <span v-if="server.security.securityMode.toLowerCase() != 'no_sec'">
              using
              <v-chip small>
                <v-icon left small>
                  {{ modeIcon(server.security.securityMode.toLowerCase()) }}
                </v-icon>
                {{ server.security.securityMode.toLowerCase() }}
              </v-chip>
            </span>
          </span>
        </div>
      </template>
      <!--custom display for "actions" column-->
      <template v-slot:item.actions="{ item }">
        <v-icon small @click.stop="onDelete(item)">
          {{ $icons.mdiDelete }}
        </v-icon>
      </template>
    </v-data-table>
  </div>
</template>
<script>
import { configsFromRestToUI, configFromUIToRest } from "../js/bsconfigutil.js";
import { fromHex, fromAscii } from "@leshan-server-core-demo/js/byteutils.js";
import SecurityInfoChip from "@leshan-server-core-demo/components/security/SecurityInfoChip.vue";
import ClientConfigDialog from "../components/wizard/ClientConfigDialog.vue";
import { getModeIcon } from "@leshan-server-core-demo/js/securityutils.js";

export default {
  components: { ClientConfigDialog, SecurityInfoChip },
  data: () => ({
    dialogOpened: false,
    headers: [
      { text: "Endpoint", value: "endpoint", width: "15%" },
      { text: "Credentials", value: "security", width: "15%", sortable: false },
      {
        text: "Bootstrap Config",
        value: "config",
        width: "70%",
        sortable: false,
      },
      { text: "Actions", value: "actions", sortable: false },
    ],
    search: "",
    clientConfigs: [],
  }),

  beforeMount() {
    var newConfigs = [];
    this.axios
      .get("api/bootstrap")
      .then((response) => {
        // on success
        newConfigs = configsFromRestToUI(response.data);
      })
      .then(() => {
        // in any case
        this.axios
          .get("api/security/clients")
          .then((response) =>
            // on success
            // merge config
            response.data.forEach((sec) => {
              let existingConfig = newConfigs.find(
                (c) => c.endpoint === sec.endpoint
              );
              if (existingConfig) {
                existingConfig.security = sec;
              } else {
                newConfigs.push({
                  endpoint: sec.endpoint,
                  security: sec,
                });
              }
            })
          )
          .then(() => {
            // in any case
            this.clientConfigs = newConfigs;
          });
      });
  },

  methods: {
    openLink(bs) {
      this.$router.push(`/bootstrap/${bs.endpoint}`);
    },
    modeIcon(securitymode) {
      return getModeIcon(securitymode);
    },

    formatData(c) {
      let s = {};
      s.securityMode = c.security.mode.toUpperCase();
      s.uri = c.url;
      switch (c.security.mode) {
        case "psk":
          s.publicKeyOrId = fromAscii(c.security.details.identity);
          s.secretKey = fromHex(c.security.details.key);
          break;
        case "rpk":
          s.publicKeyOrId = fromHex(c.security.details.client_pub_key);
          s.secretKey = fromHex(c.security.details.client_pri_key);
          s.serverPublicKey = fromHex(c.security.details.server_pub_key);
          break;
        case "x509":
          s.publicKeyOrId = fromHex(c.security.details.client_certificate);
          s.secretKey = fromHex(c.security.details.client_pri_key);
          s.serverPublicKey = fromHex(c.security.details.server_certificate);
          s.certificateUsage = c.security.details.certificate_usage;
          break;
      }
      return s;
    },

    onAdd(config) {
      if (config.security && (config.security.tls || config.security.oscore)) {
        // if we have security we try to add security first
        this.axios.put("api/security/clients/", config.security).then(() => {
          this.addConfig(config);
        });
      } else {
        // if we don't have security, we remove existing one first
        this.axios
          .delete("api/security/clients/" + encodeURIComponent(config.endpoint))
          .then(() => {
            this.addConfig(config);
          });
      }
    },

    addConfig(config) {
      let c = {
        endpoint: config.endpoint,
        dm: [],
        bs: [],
      };

      if (config.dm) {
        let dmServer = this.formatData(config.dm);
        c.dm = [
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
        ];
      }
      if (config.bs) {
        let bsServer = this.formatData(config.bs);
        c.bs = [
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
        ];
      }

      if (config.security) {
        c.security = config.security;
      }
      if (config.toDelete) {
        c.toDelete = config.toDelete;
      }
      if (config.autoIdForSecurityObject) {
        c.autoIdForSecurityObject = config.autoIdForSecurityObject;
      }

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
            this.$set(this.clientConfigs, index, c);
          } else {
            this.clientConfigs.push(c);
          }
          this.dialogOpened = false;
        });
    },
    onDelete(config) {
      // if try to remove security info first
      this.axios
        .delete("api/security/clients/" + encodeURIComponent(config.endpoint))
        .then(() => {
          this.deleteConfig(config);
        });
    },
    deleteConfig(config) {
      this.indexToRemove = this.clientConfigs.indexOf(config);
      this.axios
        .delete("api/bootstrap/" + encodeURIComponent(config.endpoint))
        .then(() => {
          this.clientConfigs.splice(this.indexToRemove, 1);
        });
    },
  },
};
</script>
