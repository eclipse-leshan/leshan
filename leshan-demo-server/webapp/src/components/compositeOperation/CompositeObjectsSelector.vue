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
  <v-sheet color="grey-lighten-5" class="pa-4" width="100%" height="100%">
    <v-card>
      <v-card-actions class="justify-center">
        <v-btn size="small" @click="openNewCompositeObject()">
          Add Composite Object
        </v-btn>
      </v-card-actions>
      <v-list density="compact">
        <v-list-item
          v-for="(compositeObject, index) in $pref.compositeObjects"
          :key="index"
          :to="
            '/clients/' +
            $route.params.endpoint +
            '/composite/' +
            compositeObject.name
          "
        >
          <v-list-item>
            <composite-object-icons :compositeObject="compositeObject" />
          </v-list-item>

          <v-list-item-title> {{ compositeObject.name }}</v-list-item-title>
          <v-list-item-subtitle>
            {{ pathsAsString(compositeObject) }}</v-list-item-subtitle
          >
          <template v-slot:append>
            <v-btn
              @click.prevent="openEditCompositeObject(compositeObject)"
              :icon="$icons.mdiPencil"
              size="x-small"
            >
            </v-btn>
            <v-btn
              @click.prevent="removeCompositeObject(index)"
              :icon="$icons.mdiDelete"
              size="x-small"
            >
            </v-btn>
          </template>
        </v-list-item>
      </v-list>

      <!-- composite object dialog -->
      <composite-object-dialog
        v-model="showDialog"
        :initialValue="editedCompositeObject"
        :alreadyUsedName="$pref.compositeObjects.map((o) => o.name)"
        @new="addNewCompositeObject($event)"
        @edit="editCompositeObject($event)"
      />
    </v-card>
  </v-sheet>
</template>
<script>
import CompositeObjectIcons from "./CompositeObjectIcons.vue";
import CompositeObjectDialog from "./CompositeObjectDialog.vue";

export default {
  components: {
    CompositeObjectDialog,
    CompositeObjectIcons,
  },
  data() {
    return {
      dialog: false,
      editedCompositeObject: null,
    };
  },
  computed: {
    showDialog: {
      get() {
        return this.dialog;
      },
      set(value) {
        this.dialog = value;
      },
    },
  },
  methods: {
    pathsAsString(compositObject) {
      return compositObject.paths.join(" , ");
    },
    openNewCompositeObject() {
      this.editedCompositeObject = null;
      this.showDialog = true;
    },
    openEditCompositeObject(compositeObjectToEdit) {
      this.editedCompositeObject = compositeObjectToEdit;
      this.showDialog = true;
    },
    addNewCompositeObject(compositeObject) {
      this.$pref.compositeObjects = [
        ...this.$pref.compositeObjects,
        compositeObject,
      ];
      this.showDialog = false;
    },
    editCompositeObject(compositeObject) {
      this.$pref.compositeObjects = this.$pref.compositeObjects.map((co) =>
        co.name == compositeObject.name ? compositeObject : co
      );
      this.showDialog = false;
    },
    removeCompositeObject(index) {
      this.$pref.compositeObjects.splice(index, 1);
    },
  },
};
</script>
