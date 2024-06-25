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
  <v-row>
    <v-col>
      <v-text-field
        single-line
        label="Object Id"
        :rules="[(v) => !v || isValidId(v) || 'Invalid object id']"
        :value="objectId"
        @input="$emit('input', objectIdChanged($event))"
        clearable
      />
    </v-col>
    <v-col>
      <v-text-field
        single-line
        label="Object Instance Id"
        :rules="[(v) => !v || isValidId(v) || 'Invalid object instance id']"
        :value="objectInstanceId"
        @input="$emit('input', objectInstanceIdChanged($event))"
        clearable
      />
    </v-col>
  </v-row>
</template>
<script>
import { isInteger } from "../../../js/utils.js";

/**
 * An input for OBJLNK single value LWM2M node ("Single Instance Resource" or "Resource Instance")
 */
export default {
  props: {
    value: null, // the input value for this LWM2M Node (v-model)
    resourcedef: Object, // the model of the resource
  },
  data() {
    return {
      objectId: "",
      objectInstanceId: "",
    };
  },
  watch: {
    value(v) {
      this.objectId = v.objectId;
      this.objectInstanceId = v.objectInstanceId;
    },
  },

  methods: {
    convertToNumber(v){
      if (isInteger(v)){
        return Number(v)
      }else{
        return v;
      }
    },
    isValidId(v) {
      return isInteger(v);
    },
    objectIdChanged(v) {
      if (v === "" && this.objectInstanceId === "") {
        return null;
      } else {
        return {
          objectId: this.convertToNumber(v),
          objectInstanceId: this.objectInstanceId,
        };
      }
    },
    objectInstanceIdChanged(v) {
      if (this.objectId === "" && v === "") {
        return null;
      } else {
        return {
          objectId: this.objectId,
          objectInstanceId: this.convertToNumber(v)
        };
      }
    },
  },
};
</script>
