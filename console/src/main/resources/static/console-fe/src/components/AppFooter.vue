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
  <footer class="public-footer">
    <span class="footer-text">Apache Seata (Incubating) Version: {{ version }}</span>
  </footer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const version = ref(import.meta.env.DEV ? 'dev' : '')

onMounted(async () => {
  try {
    const res = await fetch('/version.json')
    const data = await res.json()
    if (data.version) {
      version.value = data.version
    }
  } catch {
    // dev: 保持 'dev'；prod: version.json 必然存在，无需处理
  }
})
</script>

<style lang="scss" scoped>
.public-footer {
  background: #fff;
  box-shadow: 0 -1px 4px rgba(0, 21, 41, 0.08);
  text-align: center;
  padding: 24px 0;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
