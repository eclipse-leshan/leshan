<template>
  <div>
    <request-button @on-click="read" v-if="readable(resourcedef)"
      >R</request-button
    >
    <request-button
      @on-click="openWriteDialog"
      v-if="writable(resourcedef)"
      ref="W"
      >W</request-button
    >
    <request-button @on-click="exec" v-if="executable(resourcedef)"
      >E</request-button
    >
    <resource-write-dialog
      v-model="showDialog"
      :resourcedef="resourcedef"
      :path="path"
      @write="write($event)"
    />
  </div>
</template>
<script>
import RequestButton from "./RequestButton.vue";
import ResourceWriteDialog from "./ResourceWriteDialog.vue";
import { preference } from "vue-preferences";

const timeout = preference("timeout", { defaultValue: 5 });
const format = preference("singleformat", { defaultValue: "TLV" });

export default {
  components: { RequestButton, ResourceWriteDialog },
  props: { resourcedef: Object, path: String, endpoint: String, value: null },
  data() {
    return {
      dialog: false,
    };
  },
  computed: {
    showDialog: {
      get() {
        return this.dialog;
      },
      set(value) {
        this.dialog = value;
        this.$refs.W.resetState();
      },
    },
  },
  methods: {
    requestPath() {
      return `api/clients/${encodeURIComponent(this.endpoint)}${
        this.path
      }?timeout=${timeout.get()}&format=${format.get()}`;
    },
    readable(resourcedef) {
      return resourcedef.operations.includes("R");
    },
    writable(resourcedef) {
      return resourcedef.operations.includes("W");
    },
    executable(resourcedef) {
      return resourcedef.operations.includes("E");
    },
    updateState(content, requestButton) {
      let state = !content.valid
        ? "warning"
        : content.success
        ? "success"
        : "error";
      requestButton.changeState(state, content.status);
    },
    read(requestButton) {
      this.axios
        .get(this.requestPath())
        .then((response) => {
          this.updateState(response.data, requestButton);
          if (response.data.success)
            this.$emit("input", {
              val: response.data.content.value,
              supposed: false,
            });
        })
        .catch((error) => {
          console.log(error);
          requestButton.resetState();
          alert(error);
        });
    },
    openWriteDialog() {
      this.dialog = true;
    },
    write(value) {
      let requestButton = this.$refs.W;
      this.axios
        .put(this.requestPath(), { id: this.resourcedef.id, value: value })
        .then((response) => {
          this.updateState(response.data, requestButton);
          if (response.data.success)
            this.$emit("input", { val: value, supposed: true });
        })
        .catch((error) => {
          console.log(error);
          requestButton.resetState();
          alert(error);
        });
    },
    exec(requestButton) {
      this.axios
        .post(this.requestPath())
        .then((response) => {
          this.updateState(response.data, requestButton);
        })
        .catch((error) => {
          console.log(error);
          requestButton.resetState();
          alert(error);
        });
    },
  },
};
</script>
