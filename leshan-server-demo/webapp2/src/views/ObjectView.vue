<template>
  <!-- object info -->
  <v-sheet color="grey lighten-5" class="pa-4" width="100%" v-if="objectdef">
    <div>
      <h3>
        <object-icon :objectId="objectdef.id" /> {{ objectdef.name }}-v{{
          objectdef.version
        }}
        <object-control
          :key="objectdef.id"
          id="object.id"
          :objectdef="objectdef"
          :endpoint="$route.params.endpoint"
        />
      </h3>

      <p style="white-space: pre-wrap">{{ objectdef.description }}</p>
    </div>
    <v-divider></v-divider>
    <div v-for="instance in instances" :key="objectdef.id + '/' + instance.id">
      <instance-view
        :objectdef="objectdef"
        :instanceId="instance.id"
        :endpoint="$route.params.endpoint"
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
  props: { objectdef: Object, instances: Array},
};
</script>
