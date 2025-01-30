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
    id="StaticResourceFormDrawer"
    ref="drawer"
    v-model="drawer"
    :right="!$vuetify.rtl"
    eager
    go-back-button
    allow-expand
    @closed="close">
    <template #title>
      <span>{{ drawerTitle }}</span>
    </template>
    <template v-if="drawer" #content>
      <v-card height="calc(100% - 32px)" class="ma-4 elevation-0">
        <div class="text-header pt-4">
          {{ $t('siteManagement.staticResource.label.name') }}
        </div>
        <v-card-text class="ps-0 pt-2">
          <input
            ref="resourceName"
            v-model="StaticResourceName"
            :aria-label="StaticResourceName"
            :placeholder="$t('siteManagement.staticResource.label.fileName.placeholder', {0: resourceType})"
            type="text"
            class="ignore-vuetify-classes full-width"
            required>
        </v-card-text>
        <div class="d-flex pb-2 align-center">
          <div class="text-header">
            {{ $t('siteManagement.staticResource.label.customizePosition') }}
          </div>
          <v-spacer />
          <v-switch
            v-model="customizePosition"
            :ripple="false"
            color="primary"
            class="my-auto" />
        </div>
        <v-radio-group
          v-if="customizePosition"
          v-model="position"
          class="mt-0">
          <v-radio value="START_OF_HEAD" :label="$t('siteManagement.staticResource.label.customizePosition.startOfHead')" />
          <v-radio value="END_OF_HEAD" :label="$t('siteManagement.staticResource.label.customizePosition.endOfHead')" />
          <v-radio value="START_OF_BODY" :label="$t('siteManagement.staticResource.label.customizePosition.startOfBody')" />
          <v-radio value="END_OF_BODY" :label="$t('siteManagement.staticResource.label.customizePosition.endOfBody')" />
        </v-radio-group>
        <div class="text-header pt-4">
          Code
        </div>
        <widget-code-editor
          v-model="content"
          :mode="resourceType === 'js' ? 'javascript' : resourceType"
          :expanded="false"
          :min-height="editorHeight"
          :multiple="false"
          class="application-body" />
      </v-card>
    </template>
    <template #footer>
      <div class="d-flex">
        <v-spacer />
        <v-btn
          class="btn ms-2"
          @click="close">
          {{ $t('siteManagement.staticResource.label.cancel') }}
        </v-btn>
        <v-btn
          :disabled="disableSaveButton"
          :loading="loading"
          class="btn btn-primary ms-2"
          @click="save">
          {{ saveButtonLabel }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>

export default {
  data: () => ({
    drawer: false,
    StaticResourceName: '',
    content: null,
    setting: null,
    customizePosition: false,
    position: 'END_OF_BODY',
    loading: false,
    resource: null
  }),
  computed: {
    resourceId() {
      return this.resource?.id;
    },
    resourceContent() {
      return this.resource !== null && this.resource[this.resourceType];
    },
    resourceName() {
      return this.resource?.properties?.resourceName;
    },
    resourcePosition() {
      return this.resource?.properties?.position;
    },
    resourceType() {
      return this.setting?.resourceType;
    },
    siteName() {
      return this.setting?.siteName;
    },
    drawerTitle() {
      return this.resourceType === 'css' ? this.$t('siteManagement.staticResource.label.addCss') : this.$t('siteManagement.staticResource.label.addJs');
    },
    saveButtonLabel() {
      return this.resourceId ? this.$t('siteManagement.staticResource.label.update') : this.$t('siteManagement.staticResource.label.save');
    },
    disableSaveButton() {
      return !this.resourceChanged || !this.content || !this.StaticResourceName;
    },
    resourceChanged() {
      return this.resourceContent !== this.content || this.resourceName !== this.StaticResourceName || this.customizePosition && (this.resourcePosition !== this.position) || !!this.resourcePosition !== this.customizePosition;
    },
    editorHeight() {
      return `calc(100% - ${this.customizePosition ? '332' : '200'}px)`;
    }
  },
  created() {
    this.$root.$on('open-site-static-resource-form-drawer', this.open);
  },
  beforeDestroy() {
    this.$root.$off('open-site-static-resource-form-drawer', this.open);
  },
  methods: {
    open(setting) {
      if (setting?.id) {
        this.resource = setting;
        this.setting = { ...{siteName: setting?.properties?.siteName,
          resourceType: setting?.type.toLowerCase()} };
        this.StaticResourceName = setting?.properties?.resourceName;
        this.customizePosition = !!setting?.properties?.position;
        this.position = setting?.properties?.position || 'END_OF_BODY';
        this.content = setting[setting?.type.toLowerCase()];
      } else {
        this.setting = { ...setting };
      }
      this.$refs.drawer.open();
    },
    close() {
      this.setting = null;
      this.content = null;
      this.resource = null;
      this.StaticResourceName = '';
      this.position = 'END_OF_BODY';
      this.customizePosition = false;
      this.$refs.drawer.close();
    },
    save() {
      this.loading = true;
      const properties = {
        siteName: this.siteName,
        resourceName: this.StaticResourceName,
        position: this.customizePosition ? this.position : '',
        enabled: true
      };
      const resource = {
        css: this.resourceType === 'css' ? this.content : null,
        js: this.resourceType === 'js' ? this.content : null,
        type: this.resourceType.toUpperCase(),
        properties: properties
      };
      if (this.resourceId) {
        resource.id = this.resourceId;
        this.$widgetService.updateWidget(resource).then(() => {
          this.$root.$emit('alert-message', this.$t('siteManagement.staticResource.ResourceUpdateSuccess'), 'success');
          this.$root.$emit('refresh-site-static-resources');
        }).finally(() => {
          this.loading = false;
          this.close();
        });
      } else {
        this.$staticResourceApplicationService.createStaticResourceApplication(resource).then(() => {
          this.$root.$emit('alert-message', this.$t('siteManagement.staticResource.ResourceAddSuccess'), 'success');
          this.$root.$emit('refresh-site-static-resources');
        }).finally(() => {
          this.loading = false;
          this.close();
        });
      }
    }
  }
};
</script>
