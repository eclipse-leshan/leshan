<template>
  <span>
    <request-button @on-click="read">R</request-button>
    <request-button @on-click="openWriteDialog" ref="W">W</request-button>
    <request-button @on-click="del">Delete</request-button>
    <instance-write-dialog
      v-model="showDialog"
      :objectdef="objectdef"
      :path="path"
      :id="id"
      @update="write($event, false)"
      @replace="write($event, true)"
    />
  </span>
</template>
<script>
import RequestButton from "./RequestButton.vue";
import InstanceWriteDialog from "./InstanceWriteDialog.vue";
import { preference } from "vue-preferences";

const timeout = preference("timeout", { defaultValue: 5 });
const format = preference("multiformat", { defaultValue: "TLV" });

export default {
  components: { RequestButton, InstanceWriteDialog },
  props: { objectdef: Object, path: String, endpoint: String, id: String },
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
          if (response.data.success) {
            this.$store.newInstanceValue(
              this.endpoint,
              this.path,
              response.data.content.resources
            );
          }
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
    openWriteDialog() {
      this.dialog = true;
    },
    write(value, replace) {
      let requestButton = this.$refs.W;
      let data = { id: this.id, resources: [] };
      for (let id in value) {
        data.resources.push({ id: id, value: value[id] });
      }

      this.axios
        .put(this.requestPath() + "&replace=" + replace, data)
        .then((response) => {
          this.updateState(response.data, requestButton);
          if (response.data.success) {
            this.$store.newInstanceValue(
              this.endpoint,
              this.path,
              data.resources,
              true
            );
          }
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
    del(requestButton) {
      this.axios
        .delete(this.requestPath())
        .then((response) => {
          this.updateState(response.data, requestButton);
          if (response.data.success) {
            this.$store.removeInstanceValue(this.endpoint, this.path);
          }
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
  },
};
</script>
