<template>
  <v-dialog
    v-model="show"
    hide-overlay
    fullscreen
    transition="dialog-bottom-transition"
  >
    <v-card>
      <v-card-title class="headline grey lighten-2">
        Create new Instance for Object {{ objectdef.name }} ({{ objectdef.id }})
      </v-card-title>
      <v-card-text>
        <v-form ref="form" @submit.prevent="write">
          <v-text-field
            v-model="instance.id"
            label="InstanceId"
            hint="Let this field empty to let the client choose the instance ID"
          ></v-text-field>
          <v-text-field
            v-for="resourcedef in writableResourceDef"
            :key="resourcedef.id"
            v-model="instance.resources[resourcedef.id]"
            :label="resourcedef.name"
          ></v-text-field>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn text @click="create">
          Create
        </v-btn>
        <v-btn text @click="show=false">
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
  },
  data() {
    return {
      instance: { id: null, resources: {} },
    };
  },
  watch: {
    value(v) {
      // reset local state when dialog is open
      if (v) {
        (this.instanceValue = { id: null, resources: {} }), (this.id = null);
      }
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
    create() {
      this.show = false;
      this.$emit("create", this.instance);
    },
  },
};
</script>
