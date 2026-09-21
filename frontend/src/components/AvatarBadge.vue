<script setup lang="ts">
import { computed } from 'vue'
import { initials } from '../utils/format'
import { resolveAssetUrl } from '../api/client'

const props = withDefaults(defineProps<{ name: string; url?: string; size?: 'sm' | 'md' | 'lg' }>(), {
  size: 'md',
})

const label = computed(() => initials(props.name))
</script>

<template>
  <span class="avatar" :class="`avatar--${size}`" :aria-label="`${name} 的头像`">
    <img v-if="url" :src="resolveAssetUrl(url)" alt="" />
    <span v-else>{{ label }}</span>
  </span>
</template>
