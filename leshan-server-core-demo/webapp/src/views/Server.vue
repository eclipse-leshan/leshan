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
  <div class="pa-5">
    <v-card elevation="0">
      <v-card-text>
        <v-card-title class="justify-center"> Server Information </v-card-title>

        <v-card-subtitle class="text-center">
          Here some information about this server.
        </v-card-subtitle>
      </v-card-text>
    </v-card>
    <v-row>
      <v-col md="3" cols="12">
        <v-card outlined>
          <v-list-item three-line>
            <v-list-item-content>
              <div class="text-overline mb-4">CoAP Endpoint</div>
              <v-list-item-title class="text-h5 mb-1">
                Server URL
              </v-list-item-title>
              <v-list-item-subtitle
                >Available endpoints for this server:
              </v-list-item-subtitle>
            </v-list-item-content>
            <v-icon>{{ $icons.mdiAccessPointNetwork }}</v-icon>
          </v-list-item>
          <v-card-text>
            <ul>
              <li>
                <code
                  ><strong>{{ coapurl }}</strong> </code
                >: CoAP over UDP.
              </li>
              <li>
                <code
                  ><strong>{{ coapsurl }}</strong> </code
                >: CoAP over DTLS.
              </li>
            </ul>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col md="4" cols="12" v-if="pubkey">
        <v-card outlined>
          <v-list-item three-line>
            <v-list-item-content>
              <div class="text-overline mb-4">RPK</div>
              <v-list-item-title class="text-h5 mb-1">
                Server Public Key
              </v-list-item-title>
              <v-list-item-subtitle class="font-italic">
                SubjectPublicKeyInfo der encoded
              </v-list-item-subtitle>
            </v-list-item-content>
            <v-icon>{{ $icons.mdiKey }}</v-icon>
            <v-card-actions>
              <v-btn
                icon
                title="Download Public Key"
                @click="saveFile(pubkeyFileName, pubkey.bytesDer)"
              >
                <v-icon>{{ $icons.mdiDownload }}</v-icon>
              </v-btn>
            </v-card-actions>
          </v-list-item>

          <v-card-text>
            <p>
              If you want to connect a client using DTLS with Raw Public
              Key(RPK) mode, your client need to trust this key to accept DTLS
              connection with this server.
            </p>
            <u>Elliptic Curve parameters :</u>
            <div>
              <code>{{ pubkey.params }}</code>
            </div>
            <u>Public x coord :</u>
            <div>
              <code>{{ pubkey.x }}</code>
            </div>
            <u>Public y coord :</u>
            <div>
              <code>{{ pubkey.y }}</code>
            </div>
            <p>
              <u>Hex : </u>
            </p>

            <div class="key">
              {{ pubkey.hexDer }}
            </div>
            <br />
            <u>Base64 : </u>
            <div class="key">
              {{ pubkey.b64Der }}
            </div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col md="5" cols="12" v-if="certificate">
        <v-card outlined>
          <v-list-item three-line>
            <v-list-item-content>
              <div class="text-overline mb-4">x509</div>
              <v-list-item-title class="text-h5 mb-1">
                Server Certificate
              </v-list-item-title>
              <v-list-item-subtitle class="font-italic">
                x509v3 der encoded
              </v-list-item-subtitle>
            </v-list-item-content>
            <v-icon>{{ $icons.mdiCertificate }}</v-icon>
            <v-card-actions>
              <v-btn
                icon
                title="Download Certificate"
                @click="saveFile(certFileName, certificate.bytesDer)"
              >
                <v-icon>{{ $icons.mdiDownload }}</v-icon>
              </v-btn>
            </v-card-actions>
          </v-list-item>
          <v-card-text>
            <p>
              If you want to connect a client using DTLS with certificate(x509)
              mode, your client need to trust this certificate to accept DTLS
              connection with this server.
            </p>
            <u>Hex : </u>
            <div class="key">
              {{ certificate.hexDer }}
            </div>
            <br />
            <u>Base64 : </u>
            <div class="key">
              {{ certificate.b64Der }}
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>
<script>
import { toHex, base64ToBytes } from "../js/byteutils.js";
import { saveAs } from "file-saver";

export default {
  props: { pubkeyFileName: String, certFileName: String },
  data() {
    return {
      coapurl: "",
      coapsurl: "",
      certificate: {},
      pubkey: {},
    };
  },
  methods: {
    saveFile(filename, bytes) {
      var blob = new Blob([bytes], { type: "application/octet-stream" });
      saveAs(blob, filename);
    },
  },
  beforeMount() {
    this.axios.get("api/server/endpoint").then((response) => {
      this.coapurl = `coap://${location.hostname}:${response.data.unsecuredEndpointPort}`;
      this.coapsurl = `coaps://${location.hostname}:${response.data.securedEndpointPort}`;
    });
    this.axios.get("api/server/security").then((response) => {
      if (response.data.certificate) {
        this.certificate = response.data.certificate;
        this.certificate.bytesDer = base64ToBytes(this.certificate.b64Der);
        this.certificate.hexDer = toHex(this.certificate.bytesDer);
        this.pubkey = response.data.certificate.pubkey;
        this.pubkey.bytesDer = base64ToBytes(this.pubkey.b64Der);
        this.pubkey.hexDer = toHex(this.pubkey.bytesDer);
      } else if (response.data.pubkey) {
        this.certificate = null;
        this.pubkey = response.data.pubkey;
        this.pubkey.bytesDer = base64ToBytes(this.pubkey.b64Der);
        this.pubkey.hexDer = toHex(this.pubkey.bytesDer);
      }
    });
  },
};
</script>
<style>
.key {
  word-wrap: break-word;
  word-break: break-all;
  background: rgba(0, 0, 0, 0.05);
  font-family: monospace, monospace;
}
</style>
