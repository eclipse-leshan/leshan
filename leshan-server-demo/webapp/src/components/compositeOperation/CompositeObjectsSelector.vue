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
  <v-sheet color="grey lighten-5" class="pa-4" width="100%" height="100%">
    <v-card>
      <v-card-actions class="justify-center">
        <v-btn small @click="openNewCompositeObject()">
          Add Composite Object
        </v-btn>
      </v-card-actions>
      <v-list dense>
        <v-list-item-group>
          <v-list-item
            v-for="(compositeObject, index) in compositeObjects"
            :key="index"
            :to="
              '/clients/' +
              $route.params.endpoint +
              '/composite/' +
              compositeObject.name
            "
          >
            <v-list-item-icon>
              <composite-object-icons :compositeObject="compositeObject" />
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title> {{ compositeObject.name }}</v-list-item-title>
              <v-list-item-subtitle>
                {{ pathsAsString(compositeObject) }}</v-list-item-subtitle
              >
            </v-list-item-content>
            <v-list-item-action>
              <v-btn
                icon
                @click.prevent="openEditCompositeObject(compositeObject)"
              >
                <v-icon small>
                  {{ $icons.mdiPencil }}
                </v-icon>
              </v-btn>
            </v-list-item-action>
            <v-list-item-action>
              <v-btn icon @click.prevent="removeCompositeObject(index)">
                <v-icon>{{ $icons.mdiDelete }}</v-icon>
              </v-btn>
            </v-list-item-action>
          </v-list-item>
        </v-list-item-group>
      </v-list>

      <!-- composite object dialog -->
      <composite-object-dialog
        v-model="showDialog"
        :initialValue="editedCompositeObject"
        :alreadyUsedName="compositeObjects.map((o) => o.name)"
        @new="addNewCompositeObject($event)"
        @edit="editCompositeObject($event)"
      />
    </v-card>
  </v-sheet>
</template>
<script>
import { preference } from "vue-preferences";
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
    compositeObjects: preference("compositeObjects", {
      defaultValue: [
        { name: "myCompositeObject", paths: ["/3/0/1", "/3/0/2"] },
      ],
    }),
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
      this.compositeObjects = [...this.compositeObjects, compositeObject];
      this.showDialog = false;
    },
    editCompositeObject(compositeObject) {
      this.compositeObjects = this.compositeObjects.map((co) =>
        co.name == compositeObject.name ? compositeObject : co
      );
      this.showDialog = false;
    },
    removeCompositeObject(index) {
      this.compositeObjects.splice(index, 1);
      // HACK to force reactivity, maybe a bug in vue-preference ?
      this.compositeObjects = [...this.compositeObjects];
    },
  },
};
</script>
