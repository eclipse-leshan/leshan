<template>
  <!-- object info -->
  <v-sheet color="grey lighten-5" class="pa-4" width="100%" v-if="object">
    <div>
      <h3>
        <object-icon :objectId="object.id" /> {{ object.name }}-v{{
          object.version
        }}
        <object-control :key="object.id" id="object.id" :objectdef="object" :endpoint="$route.params.endpoint"/>
      </h3>
      <p>{{ object.description }}</p>
    </div>
    <v-divider></v-divider>
    <div v-for="instance in instances" :key="object.id + '/' + instance.id">
      <instance-view
        :object="object"
        :instanceId="instance.id"
        :showTitle="instances.length > 1"
        :endpoint="$route.params.endpoint"
        v-model="data"
      />
    </div>
  </v-sheet>
</template>
<script>
import InstanceView from "../components/InstanceView.vue";
import ObjectControl from "../components/ObjectControl.vue";
import ObjectIcon from "../components/ObjectIcon.vue";

export default {
  components: { InstanceView, ObjectIcon, ObjectControl },
  props: { object: Object, instances: Array },
  data() {
    return {
      data: {},
    };
  },
};
</script>
