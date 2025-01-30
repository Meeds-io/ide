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
  <exo-drawer
    id="staticResourceDrawer"
    ref="drawer"
    v-model="drawer"
    :loading="loading"
    :right="!$vuetify.rtl"
    eager
    allow-expand
    @closed="close">
    <template #title>
      <span>{{ $t('siteManagement.staticResource.label.addResource') }}</span>
    </template>
    <template v-if="drawer" #content>
      <v-card class="ma-4 elevation-0">
        <div>{{ $t('siteManagement.staticResource.label.customizeStyles') }}</div>
        <div class="text-header pt-4">
          {{ $t('siteManagement.staticResource.label.site') }}
        </div>
        <v-list-item
          class="px-0"
          dense>
          <v-list-item-avatar
            tile>
            <v-icon size="30" class="medium-grey-color">
              {{ siteIcon }}
            </v-icon>
          </v-list-item-avatar>
          <v-list-item-content>
            <v-list-item-title>{{ siteDisplayName }}</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <div class="d-flex pb-2 pt-4 align-center">
          <div class="font-weight-bold">
            {{ $t('siteManagement.staticResource.label.cssFiles') }}
          </div>
          <v-spacer />
          <v-btn
            class="justify-end"
            x-large
            icon
            dark
            @click="createStaticResource('css')">
            <v-icon size="20" class="primary">fas fa-plus</v-icon>
          </v-btn>
        </div>
        <div class="d-flex pb-2 align-center">
          <div class="text-header">
            {{ $t('siteManagement.staticResource.label.name') }}
          </div>
          <v-spacer />
          <div class="text-header">
            {{ $t('siteManagement.staticResource.label.actions') }}
          </div>
        </div>
        <site-management-static-resource-item
          initialized="initialized"
          v-for="item in cssResources"
          :key="item"
          :resource="item" />
        <div class="d-flex pb-2 pt-4 align-center">
          <div class="font-weight-bold">
            {{ $t('siteManagement.staticResource.label.jsFiles') }}
          </div>
          <v-spacer />
          <v-btn
            class="justify-end"
            x-large
            icon
            dark
            @click="createStaticResource('js')">
            <v-icon size="20" class="primary">fas fa-plus</v-icon>
          </v-btn>
        </div>
        <div class="d-flex pb-2 align-center">
          <div class="text-header">
            {{ $t('siteManagement.staticResource.label.name') }}
          </div>
          <v-spacer />
          <div class="text-header">
            {{ $t('siteManagement.staticResource.label.actions') }}
          </div>
        </div>
        <site-management-static-resource-item
          initialized="initialized"
          v-for="item in jsResources"
          :key="item"
          :resource="item" />
      </v-card>
    </template>
  </exo-drawer>
</template>
<script>

export default {
  data: () => ({
    drawer: false,
    expanded: false,
    loading: false,
    site: null,
    cssResources: [],
    jsResources: []
  }),
  computed: {
    siteDisplayName() {
      return this.site?.displayName;
    },
    siteName() {
      return this.site?.name;
    },
    siteIcon() {
      return this.site?.icon;
    }
  },
  created() {
    this.$root.$on('open-site-static-resource-drawer', this.open);
    this.$root.$on('refresh-site-static-resources', this.retrieveStaticResources);

  },
  beforeDestroy() {
    this.$root.$off('open-site-static-resource-drawer', this.open);
    this.$root.$off('refresh-site-static-resources', this.retrieveStaticResources);
  },
  methods: {
    open(site) {
      this.site = site;
      this.retrieveStaticResources();
      this.$refs.drawer.open();
    },
    close() {
      this.cssResources = [];
      this.jsResources = [];
      this.$refs.drawer.close();
    },
    createStaticResource(type) {
      this.$root.$emit('open-site-static-resource-form-drawer', {
        siteName: this.siteName,
        resourceType: type
      });
    },
    retrieveStaticResources() {
      this.loading = true;
      return this.$staticResourceApplicationService.getStaticResourceApplications({
        siteName: this.siteName,
      }).then((resources) => {
        this.cssResources = resources?.filter(resource => resource?.type === 'CSS');
        this.jsResources = resources?.filter(resource => resource?.type === 'JS');
      }).finally(() => {
        this.loading = false;
      });
    }

  }
};
</script>
