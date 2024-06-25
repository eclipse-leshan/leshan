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
  <v-card class="mb-12" elevation="0">
    <v-card-text class="pb-0">
      <p>
        A LWM2M Bootstrap Session generally starts by deleting part of the
        existing configuration on the <strong>LWM2M client</strong>.
      </p>
      <p>
        By default, objects <code>/0</code>, <code>/1</code> and <code>/21</code> are deleted,
        then you will be able to define LWM2M Server and LWM2M Bootstrap Server
        to add.
      </p>
      <p>
        Note that Security <code>/0</code> Object Instance storing LWM2M
        Bootstrap server data can not be deleted using Delete Request. <br />
        Adding Security instance without knowing the Instance ID of Bootstrap
        Server could lead to some unexpected behavior (see
        <a
          href="https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/522"
          >OMA#522</a
        >,
        <a
          href="https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/523"
          >OMA#523</a
        >). <br />
        By default, this wizard use the convention that LWM2M Bootstrap Server
        Security instance ID is 0. <br />If your client don't use this
        convention, you can use <code>autoIdForSecurityObject=true </code> and
        so Leshan will start the Bootstrap session with a Discover request to
        know which Security instance ID is used for the Bootstrap Server.
      </p>
    </v-card-text>
    <v-form ref="form" :value="valid" @input="$emit('update:valid', $event)">
      <v-switch
        class="pl-5"
        label="Auto ID For Security Object"
        :value="autoId"
        @change="$emit('update:autoId', $event)"
      />
      <paths-input
        :value="pathToDelete"
        @input="$emit('update:pathToDelete', $event)"
        addButtonText="Add Path to Delete"
      />
    </v-form>
  </v-card>
</template>
<script>
import PathsInput from "@leshan-server-core-demo/components/path/PathsInput.vue";

export default {
  components: { PathsInput },
  props: {
    pathToDelete: Array, // path to delete
    autoId: Boolean, // auto id for security Object
    valid: Boolean, // validation state of the form
  },
  methods: {
    resetValidation() {
      this.$refs.form.resetValidation();
    },
  },
};
</script>
