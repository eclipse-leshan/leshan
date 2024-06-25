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
  <!-- object info -->
  <v-sheet color="grey lighten-5" class="pa-4" width="100%" v-if="objectdef">
    <div>
      <h3>
        <object-icon :objectId="objectdef.id" /> {{ objectdef.name }}-v{{
          objectdef.version
        }}
        <object-control
          :key="objectdef.id"
          id="object.id"
          :objectdef="objectdef"
          :endpoint="$route.params.endpoint"
        />
      </h3>

      <p style="white-space: pre-wrap">{{ objectdef.description }}</p>
    </div>
    <v-divider></v-divider>
    <div v-for="instanceId in instances" :key="objectdef.id + '/' + instanceId">
      <instance-view
        :objectdef="objectdef"
        :instanceId="instanceId"
        :endpoint="$route.params.endpoint"
      />
    </div>
  </v-sheet>
</template>
<script>
import InstanceView from "../components/instance/InstanceView.vue";
import ObjectControl from "../components/object/ObjectControl.vue";
import ObjectIcon from "../components/object/ObjectIcon.vue";

export default {
  components: { InstanceView, ObjectIcon, ObjectControl },
  props: { objectdef: Object, instances: Array },
};
</script>
