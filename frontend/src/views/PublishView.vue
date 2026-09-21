<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { communityApi } from '../api/community'
import type { Category } from '../types'
import { markdownToHtml } from '../utils/markdown'

interface UploadItem {
  key: string
  name: string
  previewUrl: string
  url?: string
  uploading: boolean
  error?: string
}

const router = useRouter()
const categories = ref<Category[]>([])
const categoryId = ref<number>()
const title = ref('')
const content = ref('')
const uploads = ref<UploadItem[]>([])
const loading = ref(false)
const dragging = ref(false)
const error = ref('')
const editorMode = ref<'write' | 'preview'>('write')
const contentInput = ref<HTMLTextAreaElement>()
const hasPendingUpload = computed(() => uploads.value.some((item) => item.uploading))
const hasUploadError = computed(() => uploads.value.some((item) => item.error))
const renderedPreview = computed(() => markdownToHtml(content.value))

onMounted(async () => {
  try {
    categories.value = await communityApi.categories()
    categoryId.value = categories.value[0]?.id
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '分类加载失败'
  }
})

onBeforeUnmount(() => uploads.value.forEach((item) => URL.revokeObjectURL(item.previewUrl)))

async function addFiles(fileList: FileList | File[]) {
  const files = Array.from(fileList)
  error.value = ''
  if (uploads.value.length + files.length > 9) {
    error.value = '每篇帖子最多上传 9 张图片'
    return
  }
  const allowedTypes = new Set(['image/jpeg', 'image/png', 'image/gif'])
  for (const file of files) {
    if (!allowedTypes.has(file.type)) {
      error.value = '仅支持 JPG、PNG 和 GIF 图片'
      continue
    }
    if (file.size > 5 * 1024 * 1024) {
      error.value = `${file.name} 超过 5MB，请压缩后重试`
      continue
    }
    const item: UploadItem = {
      key: `${file.name}-${file.lastModified}-${crypto.randomUUID()}`,
      name: file.name,
      previewUrl: URL.createObjectURL(file),
      uploading: true,
    }
    uploads.value.push(item)
    try {
      const result = await communityApi.uploadImage(file)
      item.url = result.url
    } catch (reason) {
      item.error = reason instanceof Error ? reason.message : '上传失败'
    } finally {
      item.uploading = false
    }
  }
}

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  if (input.files) addFiles(input.files)
  input.value = ''
}

function onDrop(event: DragEvent) {
  dragging.value = false
  if (event.dataTransfer?.files) addFiles(event.dataTransfer.files)
}

function removeImage(item: UploadItem) {
  if (item.uploading) return
  URL.revokeObjectURL(item.previewUrl)
  uploads.value = uploads.value.filter((candidate) => candidate.key !== item.key)
}

function formatSelection(before: string, after: string, placeholder: string) {
  const input = contentInput.value
  const start = input?.selectionStart ?? content.value.length
  const end = input?.selectionEnd ?? start
  const selected = content.value.slice(start, end) || placeholder
  content.value = `${content.value.slice(0, start)}${before}${selected}${after}${content.value.slice(end)}`
  editorMode.value = 'write'
  nextTick(() => {
    contentInput.value?.focus()
    contentInput.value?.setSelectionRange(start + before.length, start + before.length + selected.length)
  })
}

async function submit() {
  if (!categoryId.value || hasPendingUpload.value || hasUploadError.value) return
  loading.value = true
  error.value = ''
  try {
    const post = await communityApi.createPost({
      categoryId: categoryId.value,
      title: title.value,
      content: content.value,
      imageUrls: uploads.value.flatMap((item) => item.url ? [item.url] : []),
    })
    await router.push(`/posts/${post.id}`)
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '发布失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="editor-page page-container">
    <div class="editor-heading">
      <span class="eyebrow"><i></i> SHARE YOUR STORY</span>
      <h1>写下此刻的校园故事</h1>
      <p>支持 Markdown 排版和实时预览，让长内容也清晰好读。</p>
    </div>
    <form class="editor-card surface-card" @submit.prevent="submit">
      <div class="editor-step"><span>01</span><div><b>选择频道</b><small>把内容放到最合适的位置</small></div></div>
      <div class="category-selector">
        <label v-for="category in categories" :key="category.id" :class="{ active: categoryId === category.id }">
          <input v-model="categoryId" type="radio" :value="category.id" />
          <i :style="{ background: category.color }"></i>{{ category.name }}
        </label>
      </div>

      <div class="editor-step"><span>02</span><div><b>组织内容</b><small>可使用标题、列表、引用、链接和代码块</small></div></div>
      <label class="editor-field">标题<input v-model.trim="title" minlength="4" maxlength="120" required placeholder="用一句话说清楚你想交流的事情" /><small>{{ title.length }}/120</small></label>

      <div class="markdown-editor">
        <div class="markdown-editor__bar">
          <div class="markdown-editor__tabs" role="tablist" aria-label="正文编辑模式">
            <button type="button" :class="{ active: editorMode === 'write' }" @click="editorMode = 'write'">编辑</button>
            <button type="button" :class="{ active: editorMode === 'preview' }" @click="editorMode = 'preview'">预览</button>
          </div>
          <div class="markdown-toolbar" aria-label="Markdown 快捷格式">
            <button type="button" title="二级标题" @click="formatSelection('## ', '', '小标题')">H2</button>
            <button type="button" title="加粗" @click="formatSelection('**', '**', '加粗文字')"><b>B</b></button>
            <button type="button" title="斜体" @click="formatSelection('*', '*', '斜体文字')"><i>I</i></button>
            <button type="button" title="引用" @click="formatSelection('> ', '', '引用内容')">“</button>
            <button type="button" title="无序列表" @click="formatSelection('- ', '', '列表项目')">•</button>
            <button type="button" title="链接" @click="formatSelection('[', '](https://)', '链接文字')">↗</button>
            <button type="button" title="行内代码" @click="formatSelection('`', '`', '代码')">&lt;/&gt;</button>
          </div>
          <span class="markdown-badge">Markdown</span>
        </div>
        <label v-show="editorMode === 'write'" class="editor-field editor-field--content">
          <span class="sr-only">正文</span>
          <textarea ref="contentInput" v-model="content" minlength="10" maxlength="10000" required rows="15" placeholder="写下完整内容。你可以使用 Markdown，例如：&#10;&#10;## 小标题&#10;- 第一项&#10;- 第二项&#10;&#10;**这是重点**"></textarea>
          <small>{{ content.length }}/10000</small>
        </label>
        <div v-show="editorMode === 'preview'" class="markdown-preview markdown-body">
          <div v-if="content.trim()" v-html="renderedPreview"></div>
          <p v-else class="markdown-preview__empty">输入正文后，这里会显示最终排版效果。</p>
        </div>
      </div>

      <div class="editor-step"><span>03</span><div><b>补充图片</b><small>图片将上传到平台配置的 MinIO 存储；最多 9 张</small></div></div>
      <label
        class="upload-drop"
        :class="{ dragging }"
        @dragenter.prevent="dragging = true"
        @dragover.prevent
        @dragleave.prevent="dragging = false"
        @drop.prevent="onDrop"
      >
        <input type="file" multiple accept="image/jpeg,image/png,image/gif" @change="onFileChange" />
        <span class="upload-drop__icon" aria-hidden="true">＋</span>
        <span><b>拖动图片到这里，或点击选择</b><small>支持 JPG、PNG、GIF；单张不超过 5MB</small></span>
      </label>
      <div v-if="uploads.length" class="upload-grid">
        <div v-for="item in uploads" :key="item.key" class="upload-item" :class="{ error: item.error }">
          <img :src="item.previewUrl" :alt="item.name" />
          <span v-if="item.uploading" class="upload-item__state"><i></i>上传中</span>
          <span v-else-if="item.error" class="upload-item__state">上传失败</span>
          <span v-else class="upload-item__state success">✓ 已上传</span>
          <button type="button" aria-label="移除图片" :disabled="item.uploading" @click="removeImage(item)">×</button>
        </div>
      </div>

      <p v-if="error" class="form-error">{{ error }}</p>
      <p v-if="hasUploadError" class="form-error">有图片上传失败，请移除后重新选择。</p>
      <div class="editor-actions">
        <RouterLink class="text-button" to="/">取消</RouterLink>
        <button class="button button--primary" type="submit" :disabled="loading || !categoryId || hasPendingUpload || hasUploadError">
          {{ loading ? '正在发布…' : hasPendingUpload ? '图片上传中…' : '发布到社区' }}
        </button>
      </div>
    </form>
  </div>
</template>
