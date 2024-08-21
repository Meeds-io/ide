<template>
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
        {{ $t('codeEditor.run') }}
      </v-btn>
      <widget-editor-save
        v-if="!$root.isPortletEditor"
        class="ms-2" />
    </div>
    <div
      v-else
      class="d-flex align-center justify-end ma-4">
      <v-btn
        :title="$t('codeEditor.switchDisplayMode')"
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
        {{ $t('codeEditor.run') }}
      </v-btn>
      <widget-editor-save
        v-if="!$root.isPortletEditor"
        class="ms-2" />
    </div>
    <v-card
      :max-width="mobileDisplayMode && '375px' || '100%'"
      width="100%"
      class="align-self-center"
      flat>
      <div
        ref="code"
        id="codeViewer"
        v-show="codeExecuted"></div>
    </v-card>
  </v-card>
</template>
<script>
export default {
  props: {
    widget: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    codeExecuted: false,
    modified: true,
    mobileDisplayMode: false,
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
    viewerUpToDate() {
      return !this.modified && this.codeExecuted;
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
  methods: {
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
        scriptElement.innerText = this.js;
        this.$refs.code.append(scriptElement);
      }
      this.modified = false;
    },
    switchMode() {
      this.mobileDisplayMode = !this.mobileDisplayMode;
    },
  },
};
</script>