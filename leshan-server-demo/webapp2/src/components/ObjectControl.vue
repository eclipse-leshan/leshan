<template>
  <span>
    <request-button
      @on-click="openCreateDialog"
      ref="C"
      :title="'Create Instance /' + objectdef.id"
      >Create</request-button
    >
    <instance-create-dialog
      v-model="showDialog"
      :objectdef="objectdef"
      @create="create($event)"
    />
  </span>
</template>
<script>
import RequestButton from "./RequestButton.vue";
import InstanceCreateDialog from "./InstanceCreateDialog.vue";
import { preference } from "vue-preferences";

const timeout = preference("timeout", { defaultValue: 5 });
const format = preference("multiformat", { defaultValue: "TLV" });

export default {
  components: { RequestButton ,InstanceCreateDialog },
  props: { objectdef: Object, endpoint: String},
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
        this.$refs.C.resetState();
      },
    },
  },
  methods: {
    requestPath() {
      return `api/clients/${encodeURIComponent(this.endpoint)}/${this.objectdef.id}?timeout=${timeout.get()}&format=${format.get()}`;
    },
    updateState(content, requestButton) {
      let state = !content.valid
        ? "warning"
        : content.success
        ? "success"
        : "error";
      requestButton.changeState(state, content.status);
    },
    openCreateDialog() {
      this.dialog = true;
    },
    create(value) {
      let requestButton = this.$refs.C;
      let data = {resources:[]};
      for (let id in value.resources){
          data.resources.push({id:id,value:value.resources[id]})
      }
      if (value.id) data.id = value.id;

      this.axios
        .post(this.requestPath(),data)
        .then((response) => {
          this.updateState(response.data, requestButton);
        })
        .catch(() => {
          requestButton.resetState();
        });
    },
  },
};
</script>
