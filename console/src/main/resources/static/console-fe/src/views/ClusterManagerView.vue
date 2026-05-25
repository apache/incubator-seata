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
  <div class="page-container cluster-manager-view">
    <el-card class="page-card">
      <div class="page-top">
        <el-breadcrumb separator="/" class="card-gap">
          <el-breadcrumb-item>{{ t('clusterManager.title') }}</el-breadcrumb-item>
          <el-breadcrumb-item>{{ t('clusterManager.subTitle') }}</el-breadcrumb-item>
        </el-breadcrumb>
        <h2 class="page-title">{{ t('clusterManager.title') }}</h2>
      </div>

      <el-form :inline="true" :model="formState" class="filter-form card-gap">
        <el-form-item label="namespace">
          <el-select v-model="formState.namespace" clearable>
            <el-option v-for="ns in namespaceOptions" :key="ns" :label="ns" :value="ns" />
          </el-select>
        </el-form-item>

        <el-form-item label="cluster">
          <el-select v-model="formState.cluster" clearable>
            <el-option v-for="c in clusterOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>

        <el-form-item class="filter-actions">
          <el-button type="primary" :loading="loading" @click="handleSearch">
            <el-icon><Search /></el-icon>
            {{ t('common.search') }}
          </el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="unitEntries" border style="width: 100%">
        <el-table-column :label="t('clusterManager.unitName')" min-width="150">
          <template #default="{ row }">
            {{ row.unitName }}
          </template>
        </el-table-column>
        <el-table-column :label="t('clusterManager.members')" min-width="120">
          <template #default="{ row }">
            {{ row.unit?.namingInstanceList?.length ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column :label="t('clusterManager.clusterType')" min-width="150">
          <template #default>
            {{ clusterData?.clusterType ?? '' }}
          </template>
        </el-table-column>
        <el-table-column :label="t('clusterManager.operations')" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="showUnitDialog(row.unitName, row.unit)">
              {{ t('clusterManager.view') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Unit detail dialog -->
    <el-dialog
      v-model="unitDialogVisible"
      :title="`${t('clusterManager.unitDialogTitle')}: ${selectedUnitName}`"
      width="80vw"
      destroy-on-close
    >
      <el-table :data="selectedUnit?.namingInstanceList ?? []" border max-height="60vh">
        <el-table-column :label="t('clusterManager.control')" min-width="220">
          <template #default="{ row }">
            {{ row.control ? `${t('clusterManager.controlEndpoint')}: ${row.control.host}:${row.control.port}` : '' }}
          </template>
        </el-table-column>
        <el-table-column :label="t('clusterManager.transaction')" min-width="220">
          <template #default="{ row }">
            {{ row.transaction ? `${t('clusterManager.transactionEndpoint')}: ${row.transaction.host}:${row.transaction.port}` : '' }}
          </template>
        </el-table-column>
        <el-table-column :label="t('clusterManager.internal')" min-width="160">
          <template #default="{ row }">
            {{ row.internal ? `${row.internal.host}:${row.internal.port}` : '' }}
          </template>
        </el-table-column>
        <el-table-column :label="t('clusterManager.weight')" prop="weight" min-width="80" />
        <el-table-column :label="t('clusterManager.healthy')" min-width="100">
          <template #default="{ row }">
            {{ row.healthy ? 'Yes' : 'No' }}
          </template>
        </el-table-column>
        <el-table-column :label="t('clusterManager.term')" prop="term" min-width="80" />
        <el-table-column :label="t('clusterManager.role')" prop="role" min-width="100" />
        <el-table-column :label="t('clusterManager.unit')" prop="unit" min-width="120" />
        <el-table-column :label="t('clusterManager.version')" prop="version" min-width="120" />
        <TableActionColumn
          :actions="metadataActions"
          :label="t('clusterManager.metadata')"
          @action="handleMetadataAction"
        />
      </el-table>
    </el-dialog>

    <!-- Metadata JSON preview dialog -->
    <el-dialog
      v-model="metadataDialogVisible"
      :title="t('clusterManager.metadataDialogTitle')"
      width="80vw"
      destroy-on-close
    >
      <pre class="metadata-json">{{ JSON.stringify(selectedMetadata, null, 2) }}</pre>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import TableActionColumn, { type RowAction } from '@/components/TableActionColumn.vue'
import { fetchClusterData } from '@/api/clusterManager'
import { useNamespaceOptions } from '@/composables/useNamespace'

const { t } = useI18n()

const {
  namespaceOptions,
  clusters: clusterOptions,
  loadNamespaces,
  onNamespaceChange: onNsChange,
} = useNamespaceOptions()

/** Loading state */
const loading = ref(false)
/** Full cluster data from backend */
const clusterData = ref<any>(null)
/** Unit entries with named keys, computed from clusterData */
const unitEntries = computed(() =>
  Object.entries(clusterData.value?.unitData ?? {}).map(([unitName, unit]) => ({ unitName, unit }))
)

interface FormState {
  namespace: string
  cluster: string
}

const createDefaultFormState = (): FormState => ({
  namespace: '',
  cluster: '',
})

const formState = ref<FormState>(createDefaultFormState())

/** Unit dialog state */
const unitDialogVisible = ref(false)
const selectedUnitName = ref('')
const selectedUnit = ref<any>(null)

/** Metadata dialog state */
const metadataDialogVisible = ref(false)
const selectedMetadata = ref<any>(null)

/** TableActionColumn actions for metadata column */
const metadataActions: RowAction[] = [
  { key: 'viewMetadata', labelKey: 'clusterManager.view' },
]

const handleMetadataAction = (key: string, row: any) => {
  if (key === 'viewMetadata' && row.metadata) {
    selectedMetadata.value = row.metadata
    metadataDialogVisible.value = true
  }
}

/** Search cluster data */
const handleSearch = async () => {
  const { namespace, cluster } = formState.value
  if (!namespace || !cluster) {
    ElMessage.error('Please select namespace and cluster')
    return
  }
  loading.value = true
  try {
    const res = await fetchClusterData(namespace, cluster)
    // Backend response: { code: '200', data: { unitData, clusterType }, success: true }
    if (res?.success) {
      clusterData.value = res.data
    } else {
      ElMessage.error(res?.message || 'Failed to fetch cluster data')
      clusterData.value = null
    }
  } catch {
    ElMessage.error('Failed to fetch cluster data')
    clusterData.value = null
  } finally {
    loading.value = false
  }
}

/** Show unit detail dialog */
const showUnitDialog = (unitName: string, unit: any) => {
  selectedUnitName.value = unitName
  selectedUnit.value = unit
  unitDialogVisible.value = true
}

/** After namespaces loaded, auto-select first and search */
watch(
  namespaceOptions,
  (options) => {
    if (options.length > 0 && !formState.value.namespace) {
      formState.value.namespace = options[0]
    }
  },
  { once: true },
)

/** Watch namespace change: cascade to clusters */
watch(
  () => formState.value.namespace,
  (newNamespace) => {
    const firstCluster = onNsChange(newNamespace || undefined)
    formState.value.cluster = firstCluster || ''
  },
)

onMounted(async () => {
  await loadNamespaces()
  // After loadNamespaces, if namespace was set by the watch above, trigger search
  if (formState.value.namespace && formState.value.cluster) {
    handleSearch()
  }
})
</script>

<style lang="scss" scoped>
.cluster-manager-view {
  .page-top {
    .page-title {
      margin: 0;
      font-size: 38px;
      font-weight: 600;
      line-height: 1.15;
    }
  }

  .metadata-json {
  background-color: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 16px;
  overflow: auto;
  max-height: 60vh;
  font-family: ui-monospace, Consolas, monospace;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}

.page-card {
    border-radius: $border-radius-lg;

    :deep(.el-card__body) {
      padding: 32px;
    }
  }

  .filter-form {
    display: grid;
    margin: 32px 0;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    column-gap: 16px;
    row-gap: 12px;
    box-sizing: border-box;
    align-items: end;

    :deep(.el-form-item) {
      margin: 0;
    }

    :deep(.el-input),
    :deep(.el-select) {
      width: 100%;
      min-width: 0;
    }

    .filter-actions {
      grid-column: 1 / -1;
      justify-self: end;
      align-self: end;

      :deep(.el-form-item__content) {
        display: inline-flex;
        justify-content: flex-end;
        flex-wrap: wrap;
        gap: 8px;
        margin-left: 0 !important;
      }
    }
  }

  @media (max-width: 768px) {
    .filter-form {
      grid-template-columns: 1fr;

      .filter-actions {
        justify-self: end;

        :deep(.el-form-item__content) {
          justify-content: flex-end;
        }
      }
    }
  }
}


</style>
