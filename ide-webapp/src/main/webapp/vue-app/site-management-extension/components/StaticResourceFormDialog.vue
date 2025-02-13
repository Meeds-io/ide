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
  <v-dialog
    v-model="dialog"
    content-class="uiPopup full-width full-height overflow-x-hidden"
    persistent
    max-width="100vw">
    <v-card class="full-height full-width" flat>
      <v-flex class="ms-5 me-0 drawerHeader flex-grow-0">
        <v-list-item class="pe-0 ps-1">
          <v-list-item-content class="drawerTitle align-start text-title text-truncate">
            {{ title }}
          </v-list-item-content>
          <v-list-item-action class="drawerIcons align-end d-flex flex-row">
            <v-btn
              :title="$t('label.close')"
              icon>
              <v-icon @click="close()">fas fa-times</v-icon>
            </v-btn>
          </v-list-item-action>
        </v-list-item>
      </v-flex>
      <v-divider class="my-0" />
      <v-card
        v-if="dialog"
        data-mode="EDIT"
        max-width="100%"
        class="ma-4"
        flat>
        <main class="ma-n5">
          <v-row class="mx-n2 mb-5" no-gutters>
            <v-col class="mx-2">
              <widget-code-editor
                v-model="resource.html"
                mode="html"
                widget-icon="fa-code"
                widget-title="HTML"
                class="application-body" />
            </v-col>
            <v-col class="mx-2">
              <widget-code-editor
                v-model="resource.css"
                mode="css"
                widget-icon="fa-file-code"
                widget-title="CSS"
                class="application-body" />
            </v-col>
            <v-col class="mx-2">
              <widget-code-editor
                v-model="resource.js"
                mode="javascript"
                widget-icon="fa-file-code"
                widget-title="JS"
                class="application-body" />
            </v-col>
          </v-row>
          <v-card
            :class="{
              'justify-center': !codeExecuted,
            }"
            min-width="300px"
            min-height="300px"
            class="d-flex flex-column application-body"
            flat>
            <div
              v-if="!codeExecuted"
              class="d-flex align-center justify-center ma-4">
              <v-btn
                :disabled="!modified"
                color="primary"
                class="ignore-vuetify-classes align-self-center"
                elevation="0"
                outlined
                @click="runCode">
                {{ $t('siteManagement.staticResource.label.run') }}
              </v-btn>
              <v-btn
                :loading="loading"
                :aria-label="$t('siteManagement.staticResource.label.save')"
                class="btn btn-primary d-flex align-center ms-2"
                elevation="0"
                @click="save">
                <span class="text-none">{{ saveButtonLabel }}</span>
              </v-btn>
            </div>
            <div
              v-else
              class="d-flex align-center justify-end ma-4">
              <v-btn
                :title="$t('siteManagement.staticResource.label.switchDisplayMode')"
                icon
                @click="switchMode">
                <v-icon size="20" class="icon-default-color">{{ mobileDisplayMode ? 'fa-desktop' :'fa-mobile-alt' }}</v-icon>
              </v-btn>
              <v-btn
                :disabled="!modified"
                color="primary"
                class="ignore-vuetify-classes align-self-center ms-2"
                elevation="0"
                outlined
                @click="runCode">
                {{ $t('siteManagement.staticResource.label.run') }}
              </v-btn>
              <v-btn
                :loading="loading"
                :aria-label="$t('siteManagement.staticResource.label.save')"
                class="btn btn-primary d-flex align-center ms-2"
                elevation="0"
                @click="save">
                <span class="text-none">{{ saveButtonLabel }}</span>
              </v-btn>
            </div>
            <v-card
              :max-width="mobileDisplayMode && '375px' || '100%'"
              width="100%"
              class="align-self-center"
              flat>
              <div
                ref="code"
                id="codeViewer"
                class="ma-4"
                v-show="codeExecuted"
                :data-up-to-date="viewerUpToDate"></div>
            </v-card>
          </v-card>
        </main>
      </v-card>
    </v-card>
  </v-dialog>
</template>
<script>

export default {
  data: () => ({
    dialog: false,
    resource: null,
    htmlContent: null,
    jsContent: null,
    cssContent: null,
    codeExecuted: false,
    modified: true,
    mobileDisplayMode: false,
    loading: false
  }),
  computed: {
    resourceId() {
      return this.resource?.id;
    },
    title() {
      return this.resourceId ? this.$t('siteManagement.staticResource.label.updateCustomApp') : this.$t('siteManagement.staticResource.label.addCustomApp');
    },
    html() {
      return this.resource?.html;
    },
    js() {
      return this.resource?.js;
    },
    css() {
      return this.resource?.css;
    },
    viewerUpToDate() {
      return !this.modified && this.codeExecuted;
    },
    saveButtonLabel() {
      return this.resourceId ? this.$t('siteManagement.staticResource.label.update') : this.$t('siteManagement.staticResource.label.save');
    },
  },
  watch: {
    html() {
      this.modified = true;
    },
    js() {
      this.modified = true;
    },
    css() {
      this.modified = true;
    },
    modified() {
      this.$root.$emit('close-alert-message');
    },
    viewerUpToDate() {
      this.$root.viewerUpToDate = this.viewerUpToDate;
    },
  },
  created() {
    this.$root.$on('open-site-static-resource-form-dialog', (resource) => {
      this.resource = resource;
      this.dialog = true;
    });
  },
  beforeDestroy() {
    this.$root.$off('open-site-static-resource-form-dialog', () => {
      this.dialog = true;
    });
  },
  methods: {
    close() {
      this.dialog = false;
    },
    save() {
      this.loading = true;
      if (this.resourceId) {
        this.$widgetService.updateWidget(this.resource).then(() => {
          this.$root.$emit('alert-message', this.$t('siteManagement.staticResource.ResourceUpdateSuccess'), 'success');
          this.$root.$emit('refresh-site-static-resources');
        }).finally(() => {
          this.loading = false;
          this.close();
        });
      } else {
        this.$staticResourceApplicationService.createStaticResourceApplication(this.resource).then(() => {
          this.$root.$emit('alert-message', this.$t('siteManagement.staticResource.ResourceAddSuccess'), 'success');
          this.$root.$emit('refresh-site-static-resources');
        }).finally(() => {
          this.loading = false;
          this.close();
        });
      }
    },
    runCode() {
      this.codeExecuted = true;
      this.$root.$emit('close-alert-message');
      this.$refs.code.innerHTML = '';
      if (this.css) {
        const styleElement = document.createElement('style');
        styleElement.innerText = this.css;
        this.$refs.code.append(styleElement);
      }
      if (this.html) {
        const htmlElement = document.createElement('div');
        htmlElement.innerHTML = this.html;
        this.$refs.code.append(htmlElement);
      }
      if (this.js) {
        const scriptElement = document.createElement('script');
        window.ideJsCodeExecuted = false;
        scriptElement.innerText = `
          function() {
            ${this.js}
          })();
          window.ideJsCodeExecuted = true;
        `;
        this.$refs.code.append(scriptElement);
        if (!window.ideJsCodeExecuted) {
          console.error('An error occurred in JS code execution, please make sure adding semicolons in JS');
          this.$root.$emit('alert-message', 'warning', this.$t('codeEditor.jsExecutionError'));
        }
      }
      this.modified = false;
    },
    switchMode() {
      this.mobileDisplayMode = !this.mobileDisplayMode;
    },
  }
};
</script>
