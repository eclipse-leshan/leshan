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
  <v-list density="compact">
    <v-list-item
      v-for="object in objects"
      :key="object.id"
      :to="'/clients/' + $route.params.endpoint + '/' + object.id"
    >
      <template v-slot:append>
        <object-icon :objectId="object.id" class="me-2" />
      </template>

      <v-list-item-title>
        {{ object.name }}
        <v-icon
          v-if="shouldBeHidden(object)"
          size="small"
          color="red"
          :title="`The Object ${object.name} ID:${object.id} MUST NOT be part of Update Objects and Object Instances list in Register Request.`"
          >{{ $mdiAlertCircle }}</v-icon
        ></v-list-item-title
      >
      <v-list-item-subtitle>
        {{ "/" + object.id }}
      </v-list-item-subtitle>
    </v-list-item>
  </v-list>
</template>
<script>
import ObjectIcon from "./ObjectIcon.vue";
export default {
  components: { ObjectIcon },
  props: { objects: Array },
  methods: {
    shouldBeHidden(object) {
      if (object) {
        switch (object.id) {
          case 0:
          case 21:
            return true;
          default:
            return false;
        }
      }
      return false;
    },
  },
};
</script>
