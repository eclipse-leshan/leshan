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
  <v-card>
    <v-card-title>{{ title }}</v-card-title>
    <v-card-text>
      <v-text-field
        v-model="inputText"
        :label="text"
        autofocus
        :placeholder="placeholderText"
        variant="outlined"
        @keydown.enter.prevent="submit"
      ></v-text-field>

    </v-card-text>
    <v-card-actions>
      <v-spacer></v-spacer>
      <v-btn variant="text" color="primary" @click="submit">{{confirmationText}}</v-btn>
      <v-btn variant="text" @click="$emit('closeDialog', null)">{{cancelText}}</v-btn>
    </v-card-actions>
  </v-card>
</template>

<script>
export default {
  props: {
    title: { type: String, default: "Prompt"},
    text: { type: String, default: "Enter value"},
    placeholderText:{ type: String, default: ""},
    cancelText: { type: String, default: "Cancel"},
    confirmationText: { type: String, default: "OK"},
  },
  data: () => ({
    inputText: "",
  }),
  emits: [ 'closeDialog'],
  methods: {
    submit(){
      this.$emit('closeDialog', this.inputText)
    }
  }
}
</script>

