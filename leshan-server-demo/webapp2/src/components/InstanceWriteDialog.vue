<template>
  <v-dialog
    v-model="show"
    hide-overlay
    fullscreen
    transition="dialog-bottom-transition"
  >
    <v-card>
      <v-card-title class="headline grey lighten-2">
        Write Instance "{{ id }}" for Object {{ objectdef.name }} ({{
          objectdef.id
        }})
      </v-card-title>
      <v-card-text>
        <v-form ref="form" @submit.prevent="write">
          <v-text-field
            v-for="resourcedef in writableResourceDef"
            :key="resourcedef.id"
            v-model="instanceValue[resourcedef.id]"
            :label="resourcedef.name"
            required
          ></v-text-field>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn text @click="update">
          Update
        </v-btn>
        <v-btn text @click="replace">
          Replace
        </v-btn>
        <v-btn text @click="show = false">
          Cancel
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
export default {
  props: {
    value: Boolean,
    objectdef: Object,
    path: String,
    id: String,
  },
  data() {
    return {
      instanceValue: {},
    };
  },
  watch: {
    value(v) {
      // reset local state when dialog is open
      if (v) this.instanceValue = {};
    },
  },
  computed: {
    show: {
      get() {
        return this.value;
      },
      set(value) {
        this.$emit("input", value);
      },
    },
    writableResourceDef() {
      return this.objectdef.resourcedefs.filter((def) =>
        def.operations.includes("W")
      );
    },
  },
  methods: {
    replace() {
      this.show = false;
      this.$emit("replace", this.instanceValue);
    },
    update() {
      this.show = false;
      this.$emit("update", this.instanceValue);
    },
  },
};
</script>
