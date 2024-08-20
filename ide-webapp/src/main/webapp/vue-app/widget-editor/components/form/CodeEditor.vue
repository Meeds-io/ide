<template>
  <v-card
    ref="parent"
    min-width="300px"
    min-height="302px"
    class="d-flex flex-column"
    flat>
    <div
      ref="content"
      :class="{
        'position-absolute t-0 b-0 r-0 l-0 z-index-modal white': expand,
        'flex-grow-1': !expand,
      }"
      class="d-flex flex-column pa-4 border-box-sizing">
      <div class="d-flex align-center mb-2">
        <v-icon size="20">{{ widgetIcon }}</v-icon>
        <span class="ms-4">{{ widgetTitle }}</span>
        <v-btn
          :title="$t('codeEditor.expandTooltip')"
          class="ms-auto"
          icon
          @click="toogleExpand">
          <v-icon size="20">{{ expandIcon }}</v-icon>
        </v-btn>
      </div>
      <iframe
        :src="editorHref"
        ref="iframe"
        width="100%"
        title="Code Editor"
        class="border-color flex-grow-1"
        @load="init"></iframe>
    </div>
  </v-card>
</template>
<script>
export default {
  props: {
    value: {
      type: String,
      default: null,
    },
    mode: {
      type: String,
      default: null,
    },
    widgetIcon: {
      type: String,
      default: null,
    },
    widgetTitle: {
      type: String,
      default: null,
    },
  },
  data: () => ({
    editor: null,
    content: null,
    expand: false,
    resizeObserver: null,
  }),
  computed: {
    editorHref() {
      return `/ide/codemirror/html/${this.mode}.html`;
    },
    expandIcon() {
      return this.expand && 'fa-compress-alt' ||  'fa-expand-alt';
    },
  },
  watch: {
    content() {
      this.$emit('input', this.content);
    },
  },
  created() {
    this.content = this.value;
    document.addEventListener('keydown', this.collapseWithShortcut);
  },
  beforeDestroy() {
    document.removeEventListener('keydown', this.collapseWithShortcut);
    this.resizeObserver?.disconnect?.();
  },
  mounted() {
    this.resizeObserver = new ResizeObserver(this.resize).observe(this.$refs.content);
  },
  methods: {
    init() {
      this.$refs.iframe.contentDocument.addEventListener('keydown', this.saveWithShortcut);
      this.$refs.iframe.contentDocument.addEventListener('keydown', this.collapseWithShortcut);
      this.resize();
      this.editor = this.$refs.iframe.contentWindow.init(this.content);
      this.editor.on('change', () => this.content = this.editor.getValue());
    },
    collapseWithShortcut(e) {
      if (this.expand && e.key === 'Escape') {
        this.toogleExpand();
      }
    },
    saveWithShortcut(e) {
      if (e.ctrlKey && e.key === 's') {
        // Prevent the Save dialog to open
        e.preventDefault();
        document.dispatchEvent(new CustomEvent('widget-editor-save'));
      }
    },
    resize() {
      this.$refs.iframe?.contentDocument?.body?.style?.setProperty?.('--codeMirrorHeight', `${this.$refs.iframe?.offsetHeight - 2}px`);
    },
    toogleExpand() {
      this.expand = !this.expand;
      if (this.expand) {
        document.querySelector('#vuetify-apps').appendChild(this.$refs.content);
      } else {
        this.$refs.parent.$el.appendChild(this.$refs.content);
      }
    },
  },
};
</script>