<!--

  This file is part of the Meeds project (https://meeds.io/).

  Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io

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
  <v-app>
    <main v-if="widget" class="ma-n5">
      <v-row class="mx-n2 mb-5" no-gutters>
        <v-col class="mx-2">
          <widget-code-editor
            v-model="widget.html"
            mode="html"
            widget-icon="fa-code"
            widget-title="HTML"
            class="application-body" />
        </v-col>
        <v-col class="mx-2">
          <widget-code-editor
            v-model="widget.css"
            mode="css"
            widget-icon="fa-file-code"
            widget-title="CSS"
            class="application-body" />
        </v-col>
        <v-col class="mx-2">
          <widget-code-editor
            v-model="widget.js"
            mode="javascript"
            widget-icon="fa-file-code"
            widget-title="JS"
            class="application-body" />
        </v-col>
      </v-row>
      <widget-code-viewer :widget="widget" />
    </main>
  </v-app>
</template>
<script>
export default {
  data: () => ({
    widget: null,
    modified: false,
    initialized: false,
  }),
  computed: {
    html() {
      return this.widget?.html;
    },
    js() {
      return this.widget?.js;
    },
    css() {
      return this.widget?.css;
    },
  },
  watch: {
    widget() {
      window.editingWidget = this.widget;
    },
    html() {
      if (this.initialized) {
        this.modified = true;
      }
    },
    js() {
      if (this.initialized) {
        this.modified = true;
      }
    },
    css() {
      if (this.initialized) {
        this.modified = true;
      }
    },
    modified() {
      if (this.modified) {
        document.dispatchEvent(new CustomEvent('widget-editor-modified'));
      }
    },
  },
  created() {
    document.addEventListener('widget-editor-saved', this.setAsNotModified);
    this.init();
  },
  beforeDestroy() {
    document.removeEventListener('widget-editor-saved', this.setAsNotModified);
  },
  methods: {
    init() {
      this.$widgetService.getWidget(this.$root.widgetId)
        .then(widget => this.widget = widget)
        .then(() => this.$nextTick())
        .finally(() => this.initialized = true);
    },
    setAsNotModified() {
      this.modified = false;
    },
  },
};
</script>
