<template>
  <v-dialog
    v-model="show"
    hide-overlay
    fullscreen
    transition="dialog-bottom-transition"
  >
    <v-card>
      <v-card-title class="headline grey lighten-2">
        Write "{{ resourcedef.name }}" Resource
      </v-card-title>
      <v-card-text>
        <div class="subtitle-1">Resource {{ path }}</div>
        <div v-if="resourcedef['type']">
          Type : <span class="text-capitalize">{{ resourcedef["type"] }}</span>
        </div>
        <div v-if="resourcedef.range">Range : {{ resourcedef.range }}</div>
        <div v-if="resourcedef.units">Units : {{ resourcedef.units }}</div>
        <v-divider class="pa-2" />
        <p>{{ resourcedef.description }}</p>
        <v-form ref="form" @submit.prevent="write">
          <v-text-field
            v-model="resourceValue"
            clearable
            :label="resourcedef.name"
            required
            autofocus
          ></v-text-field>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn text @click="write">
          Write
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
export default {
  props: {
    value: Boolean,
    resourcedef: Object,
    path: String,
  },
  data() {
    return {
      resourceValue: null,
    };
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
  },
  methods: {
    write() {
      this.show = false;
      this.$emit("write", this.resourceValue);
    },
  },
};
</script>
