<script setup>
import { onMounted, onBeforeUnmount, ref } from 'vue'
import Panzoom from '@panzoom/panzoom'

const viewport = ref(null)
const content = ref(null)

let pz
let wheelHandler

onMounted(() => {
  pz = Panzoom(content.value, {
    maxScale: 6,
    minScale: 0.4,
    contain: false,
    panOnlyWhenZoomed: false,
    startScale: 1.2,
  })

  wheelHandler = (e) => {
    e.preventDefault()
    pz.zoomWithWheel(e)
  }

  viewport.value.addEventListener('wheel', wheelHandler, { passive: false })
})

onBeforeUnmount(() => {
  try { viewport.value?.removeEventListener('wheel', wheelHandler) } catch {}
  try { pz?.destroy?.() } catch {}
})
</script>

<template>
  <div ref="viewport" class="pz-viewport">
    <div ref="content" class="pz-content">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.pz-viewport {
  width: 100%;
  height: 460px;
  overflow: hidden;
  border-radius: 14px;
  border: 1px solid rgba(0,0,0,.12);
  cursor: grab;
  touch-action: none;
}
.pz-viewport:active { cursor: grabbing; }

.pz-content {
  width: 100%;
  height: 100%;
}

.pz-viewport :deep(svg) {
  user-select: none;
  -webkit-user-select: none;
  pointer-events: none;
}
</style>
