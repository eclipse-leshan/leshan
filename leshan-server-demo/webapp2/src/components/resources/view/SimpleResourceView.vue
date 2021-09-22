<template>
  <div :title="resourceValueAsString">
    {{ resourceValueAsString }}
  </div>
</template>
<script>
import { resourceToString } from "../../../js/valueutils.js";

export default {
  props: {
    resourcedef: Object,
    resource: Object,
  },

  computed: {
    resourceValueAsString: function() {
      if (!this.resource) {
        return "";
      } else {
        return resourceToString(this.resource, this.resourcedef.type);
      }
    },
  },
};
</script>
