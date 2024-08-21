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
  <v-btn
    :disabled="!modified"
    :loading="loading"
    :aria-label="$t('codeEditor.save')"
    class="btn btn-primary d-flex align-center"
    elevation="0"
    @click="save">
    <span class="text-none">{{ $t('codeEditor.save') }}</span>
  </v-btn>
</template>
<script>
export default {
  data: () => ({
    loading: false,
    modified: false,
  }),
  created() {
    document.addEventListener('keydown', this.saveWithShortcut);
    document.addEventListener('widget-editor-save', this.save);
    document.addEventListener('widget-editor-modified', this.setAsModified);
  },
  methods: {
    setAsModified() {
      this.modified = true;
    },
    saveWithShortcut(e) {
      if (e.ctrlKey && e.key === 's') {
        // Prevent the Save dialog to open
        e.preventDefault();
        this.save();
      }
    },
    async save() {
      this.loading = true;
      try {
        await this.$widgetService.updateWidget(window.editingWidget);
        if (this.$root.isPortletEditor) {
          const instance = await this.$portletInstanceService.getPortletInstance(this.$root.portletInstanceId);
          instance.preferences = await this.$portletInstanceService.getPortletInstancePreferences(this.$root.portletInstanceId);
  
          await this.$portletInstanceService.updatePortletInstance(instance);
          await this.savePreview();
        }

        this.modified = false;
        this.$root.$emit('alert-message', this.$t('codeEditor.savedSuccessfully'), 'success');
        document.dispatchEvent(new CustomEvent('widget-editor-saved', {detail: window.editingWidget}));
      } catch (e) {
        console.debug('Error saving widget', e); // eslint-disable-line no-console
        this.$root.$emit('alert-message', this.$t('codeEditor.saveError'), 'error');
      } finally {
        window.setTimeout(() => this.loading = false, 50);
      }
    },
    async savePreview() {
      const codeViewerElement = document.querySelector('#codeViewer');
      if (!codeViewerElement?.innerHTML || !this.$root.viewerUpToDate) {
        return;
      }
      const previewCanvas = await window.html2canvas(codeViewerElement);
      const previewImage = previewCanvas.toDataURL('image/png');
      const previewBlob = this.convertPreviewToFile(previewImage);
      const uploadId =  await this.$uploadService.upload(previewBlob);
      await new Promise((resolve, reject) => {
        const interval = window.setInterval(() => {
          this.$uploadService.getUploadProgress(uploadId)
            .then(percent => {
              if (Number(percent) === 100) {
                window.clearInterval(interval);
                resolve();
              }
            })
            .catch(e => reject(e));
        }, 200);
      });
      return await this.$fileAttachmentService.saveAttachments({
        objectType: 'portletInstance',
        objectId: this.$root.portletInstanceId,
        uploadedFiles: [{uploadId}],
        attachedFiles: [],
      });
    },
    convertPreviewToFile(previewImage) {
      const imgString = window.atob(previewImage.replace(/^data:image\/\w+;base64,/, ''));
      const bytes = new Uint8Array(imgString.length);
      for (let i = 0; i < imgString.length; i++) {
        bytes[i] = imgString.charCodeAt(i);
      }
      return new Blob([bytes], {type: 'image/png'});
    },
  },
};
</script>