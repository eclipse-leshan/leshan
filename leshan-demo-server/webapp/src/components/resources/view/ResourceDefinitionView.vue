<!-----------------------------------------------------------------------------
 * Copyright (c) 2021 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
  ----------------------------------------------------------------------------->
<template>
  <v-card elevation="0" class="grey lighten-4">
    <!-- Model -->
    <v-card-subtitle class="pb-0 text-decoration-underline">
      Model :
    </v-card-subtitle>
    <v-card-text>
      <v-simple-table dense>
        <template v-slot:default>
          <thead>
            <tr>
              <th class="text-left">
                Operations
              </th>
              <th class="text-left">
                Instances
              </th>
              <th class="text-left">
                Mandatory
              </th>
              <th class="text-left">
                Type
              </th>
              <th class="text-left">
                Range or enumeration
              </th>
              <th class="text-left">
                Units
              </th>
            </tr>
          </thead>
          <tbody>
            <tr style="pointer-events: none" class="text-capitalize">
              <td>{{ resourcedef.operations }}</td>
              <td>{{ resourcedef.instancetype }}</td>
              <td>{{ resourcedef.mandatory }}</td>
              <td>{{ resourcedef.type }}</td>
              <td>{{ resourcedef.range }}</td>
              <td>{{ resourcedef.units }}</td>
            </tr>
          </tbody>
        </template>
      </v-simple-table>
    </v-card-text>
    <!-- description -->
    <v-card-subtitle class="pb-0 text-decoration-underline">
      Description :
    </v-card-subtitle>
    <v-card-text style="white-space: pre-wrap">
      {{ resourcedef.description }}<br v-if="resourcedef.description2" />{{
        resourcedef.description2
      }}
    </v-card-text>
  </v-card>
</template>
<script>
export default {
    props:{
        resourcedef:Object // resource definition to display
    }
};
</script>
