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
<script lang="ts">
export interface RowAction {
  key: string
  labelKey: string
  buttonType?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
}
</script>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'

const props = withDefaults(defineProps<{
  actions: RowAction[]
  maxPrimary?: number
  label: string
}>(), {
  maxPrimary: 2,
})

const emit = defineEmits<{
  action: [key: string, row: unknown]
}>()

const { t } = useI18n()

const primaryActions = computed(() => props.actions.slice(0, props.maxPrimary))
const foldedActions = computed(() => props.actions.slice(props.maxPrimary))

// Estimate rendered text width: CJK chars ~12.5px, Latin chars ~6px at el-button small (12px font)
const estimateTextW = (text: string): number => {
  let w = 0
  for (const char of text) {
    w += /[\u4e00-\u9fff\u3000-\u303f\uff00-\uffef]/.test(char) ? 12.5 : 6
  }
  return Math.ceil(w)
}

// Compute column width to fit all primary buttons + optional "More" dropdown
const columnWidth = computed(() => {
  const BTN_OVERHEAD = 24   // el-button small: 11px padding x2 + 1px border x2
  const BTN_GAP = 4         // flex gap between action items (.action-cell gap)
  const DROPDOWN_ML = 10    // .more-dropdown additional margin-left
  const ICON_W = 22         // ArrowDown icon (16px) + internal margin (6px)
  const CELL_PADDING = 28   // el-table cell padding (12px x2) + 4px breathing room

  const btnW = (labelKey: string) => estimateTextW(t(labelKey)) + BTN_OVERHEAD
  const primaryW = primaryActions.value.reduce((sum, a) => sum + btnW(a.labelKey), 0)
  const hasDropdown = foldedActions.value.length > 0
  const moreW = hasDropdown ? btnW('common.more') + ICON_W : 0
  const primaryGapsW = (primaryActions.value.length - 1) * BTN_GAP
  const dropdownGapW = hasDropdown ? BTN_GAP + DROPDOWN_ML : 0
  return primaryW + moreW + primaryGapsW + dropdownGapW + CELL_PADDING
})
</script>

<template>
  <el-table-column :label="label" fixed="right" :width="columnWidth">
    <template #default="{ row }">
      <div class="action-cell">
        <el-button
          v-for="action in primaryActions"
          :key="action.key"
          :type="action.buttonType ?? 'primary'"
          size="small"
          @click="emit('action', action.key, row)"
        >
          {{ t(action.labelKey) }}
        </el-button>

        <el-dropdown
          v-if="foldedActions.length"
          class="more-dropdown"
          trigger="click"
          @command="(key: string) => emit('action', key, row)"
        >
          <el-button size="small">
            {{ t('common.more') }}
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-for="action in foldedActions"
                :key="action.key"
                :command="action.key"
              >
                {{ t(action.labelKey) }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </template>
  </el-table-column>
</template>

<style scoped>
.action-cell {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 4px;
}

.action-cell > .more-dropdown {
  margin-left: 10px;
}
</style>
