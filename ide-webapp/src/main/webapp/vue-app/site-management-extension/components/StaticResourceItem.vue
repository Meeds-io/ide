<!--
 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 3 of the License, or (at your option) any later version.
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->
<template>
  <diV>
    <v-list-item
      class="px-0"
      dense>
      <v-list-item-title>
        {{ resourceName }}
      </v-list-item-title>
      <v-list-item-action class="ma-0 flex-row align-center ml-auto flex-shrink-0 border-box-sizing">
        <v-switch
          v-model="enabled"
          :ripple="false"
          class="px-3"
          color="primary mx-3"
          @change="updateResourceStatus" />
        <v-btn
          icon
          @click="updateResource">
          <v-icon size="20">fas fa-edit</v-icon>
        </v-btn>
        <v-btn
          icon
          @click="deleteConfirmDialog">
          <v-icon class="error-color" size="20">fa-solid fa-trash</v-icon>
        </v-btn>
      </v-list-item-action>
    </v-list-item>
    <exo-confirm-dialog
      ref="deleteConfirmDialog"
      :message="$t('siteManagement.staticResource.message.confirmDeleteResource')"
      :title="$t('siteManagement.staticResource.title.confirmDeleteResource')"
      :ok-label="$t('siteManagement.label.confirm')"
      :cancel-label="$t('siteManagement.label.cancel')"
      @ok="deleteResource" />
  </diV>
</template>

<script>

export default {
  props: {
    resource: {
      type: Object,
      default: null
    },
  },
  computed: {
    resourceId() {
      return this.resource?.id;
    },
    resourceName() {
      return this.resource?.properties?.resourceName;
    },
    enabled() {
      return this.resource?.properties?.enabled === 'true';
    },
  },
  methods: {
    deleteConfirmDialog() {
      this.$refs.deleteConfirmDialog.open();
    },
    deleteResource() {
      return this.$widgetService.deleteWidget(this.resourceId).then(() => {
        this.$root.$emit('alert-message', this.$t('siteManagement.staticResource.ResourceDeleteSuccess'), 'success');
        this.$root.$emit('refresh-site-static-resources');
      });
    },
    updateResourceStatus() {
      const resource = this.resource;
      resource.properties.enabled = this.enabled ? 'false' : 'true';
      return this.$widgetService.updateWidget(resource).then(() => {
        this.$root.$emit('alert-message', this.$t('siteManagement.staticResource.ResourceUpdateSuccess'), 'success');
        this.$root.$emit('refresh-site-static-resources');
      }).finally(() => {
        this.loading = false;
        this.close();
      });
    },
    updateResource() {
      this.$root.$emit('open-site-static-resource-form-drawer', this.resource);
    },
  }
};
</script>
