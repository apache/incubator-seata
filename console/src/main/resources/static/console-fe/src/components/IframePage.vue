<!--
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
-->
<template>
  <div class="iframe-container">
    <iframe
      :src="src"
      :title="title"
      class="iframe-content"
      frameborder="0"
      @load="onLoad"
    />
  </div>
</template>

<script setup lang="ts">
interface Props {
  src: string
  title?: string
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'load'): void
}>()

function onLoad() {
  emit('load')
}
</script>

<style scoped lang="scss">
.iframe-container {
  width: 100%;
  height: 100%;
  display: flex;
}

.iframe-content {
  width: 100%;
  height: 100%;
  border: none;
}
</style>
