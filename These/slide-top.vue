<template>
  <div v-if="$nav.currentLayout !== 'cover'" class="absolute bottom-0 right-5 p-2 text-s opacity-60">
    <SlideCurrentNo />
  </div>
  <div class="absolute top-0 left-0 p-2">
    <img src="/images/EPFL_logo.png" style="max-height: 20px; width: auto;" />
  </div>
</template>