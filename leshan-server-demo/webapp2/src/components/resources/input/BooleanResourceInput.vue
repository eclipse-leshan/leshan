<template>
  <v-row>
    <v-col cols="1">
      <v-checkbox
        v-model="booleanValue"
        true-value="true"
        false-value="false"
        :indeterminate="state !== ''"
        :key="state"
        :indeterminate-icon="icon"
        off-icon="mdi-close-box-outline"
      ></v-checkbox>
    </v-col>
    <v-col>
      <v-text-field
        single-line
        :label="resourcedef.type"
        :suffix="resourcedef.units"
        :rules="[(v) => isNotSet || !isNotBoolean || 'not a boolean']"
        v-model="booleanValue"
        clearable
      />
    </v-col>
  </v-row>
</template>
<script>
export default {
  props: { value: null, resourcedef: Object },
  data() {
    return {
      localValue: null,
    };
  },
  watch: {
    value(v) {
      this.localValue = v;
    },
  },

  computed: {
    booleanValue: {
      get() {
        return this.localValue;
      },
      set(v) {
        this.localValue = v;
        this.$emit("input", this.localValue);
      },
    },
    state() {
      if (this.isNotSet) {
        return "notset";
      }
      if (this.isNotBoolean) {
        return "notbool";
      } else {
        return "";
      }
    },
    isNotSet() {
      return (
        this.booleanValue === undefined ||
        this.booleanValue === null ||
        this.booleanValue === ""
      );
    },
    isNotBoolean() {
      return this.booleanValue != "true" && this.booleanValue != "false";
    },
    icon() {
      if (this.isNotSet) {
        return "mdi-checkbox-blank-outline";
      } else if (this.isNotBoolean) {
        return "mdi-help-box";
      } else {
        return "";
      }
    },
  },
};
</script>
