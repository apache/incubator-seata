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
  <div class="page-container global-lock-list-view">
    <el-card class="page-card">
      <div class="page-top">
        <el-breadcrumb separator="/" class="card-gap">
          <el-breadcrumb-item>{{ t('globalLock.title') }}</el-breadcrumb-item>
          <el-breadcrumb-item>{{ t('globalLock.subTitle') }}</el-breadcrumb-item>
        </el-breadcrumb>
        <h2 class="page-title">{{ t('globalLock.title') }}</h2>
      </div>

      <el-form :inline="true" :model="formState" class="filter-form card-gap">
        <el-form-item :label="t('globalLock.filters.createTime')" class="filter-item-span-2">
          <el-date-picker
            v-model="formState.createTime"
            type="daterange"
            unlink-panels
            value-format="YYYY-MM-DD"
            :range-separator="t('globalLock.filters.to')"
            :start-placeholder="t('globalLock.filters.startDate')"
            :end-placeholder="t('globalLock.filters.endDate')"
          />
        </el-form-item>

        <el-form-item label="xid">
          <el-input v-model="formState.xid" :placeholder="t('globalLock.filters.inputPlaceholder')" clearable />
        </el-form-item>

        <el-form-item label="tableName">
          <el-input v-model="formState.tableName" :placeholder="t('globalLock.filters.inputPlaceholder')" clearable />
        </el-form-item>

        <el-form-item label="transactionId">
          <el-input v-model="formState.transactionId" :placeholder="t('globalLock.filters.inputPlaceholder')" clearable />
        </el-form-item>

        <el-form-item label="branchId">
          <el-input v-model="formState.branchId" :placeholder="t('globalLock.filters.inputPlaceholder')" clearable />
        </el-form-item>

        <el-form-item label="namespace">
          <el-select v-model="formState.namespace" clearable>
            <el-option v-for="namespace in namespaceOptions" :key="namespace" :label="namespace" :value="namespace" />
          </el-select>
        </el-form-item>

        <el-form-item label="cluster">
          <el-select v-model="formState.cluster" clearable>
            <el-option v-for="cluster in clusterOptions" :key="cluster" :label="cluster" :value="cluster" />
          </el-select>
        </el-form-item>

        <el-form-item label="vgroup">
          <el-select v-model="formState.vgroup" :placeholder="t('globalLock.filters.selectTransactionGroup')" clearable>
            <el-option v-for="vgroup in vgroupOptions" :key="vgroup" :label="vgroup" :value="vgroup" />
          </el-select>
        </el-form-item>

        <el-form-item class="filter-actions">
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            {{ t('common.reset') }}
          </el-button>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            {{ t('common.search') }}
          </el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" border style="width: 100%">
        <el-table-column prop="xid" label="xid" min-width="250" show-overflow-tooltip />
        <el-table-column prop="transactionId" label="transactionId" min-width="180" show-overflow-tooltip />
        <el-table-column prop="branchId" label="branchId" min-width="180" show-overflow-tooltip />
        <el-table-column prop="resourceId" label="resourceId" min-width="240" show-overflow-tooltip />
        <el-table-column prop="tableName" label="tableName" min-width="130" show-overflow-tooltip />
        <el-table-column prop="pk" label="pk" min-width="80" show-overflow-tooltip />
        <el-table-column prop="rowKey" label="rowKey" min-width="120" show-overflow-tooltip />
        <el-table-column label="gmtCreate" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatTime(row.gmtCreate) }}
          </template>
        </el-table-column>
        <el-table-column label="gmtModified" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatTime(row.gmtModified) }}
          </template>
        </el-table-column>
        <TableActionColumn
          :actions="lockActions"
          :label="t('globalLock.table.actions')"
          @action="handleLockAction"
        />
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="totalRecords"
          layout="sizes, prev, pager, next"
          @size-change="currentPage = 1; handleSearch()"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import {
  queryGlobalLocks,
  deleteGlobalLock,
  checkGlobalLock,
  type GlobalLockRecord,
} from '@/api/globalLock'
import { useNamespaceOptions } from '@/composables/useNamespace'
import TableActionColumn, { type RowAction } from '@/components/TableActionColumn.vue'

interface QueryState {
  createTime: [string, string] | []
  xid: string
  tableName: string
  transactionId: string
  branchId: string
  namespace: string
  cluster: string
  vgroup: string
}

const { t } = useI18n()

const {
  namespaceOptions,
  clusters: clusterOptions,
  vgroups: vgroupOptions,
  loadNamespaces,
  onNamespaceChange,
  onClusterChange,
} = useNamespaceOptions()

/** Current page data (returned by API) */
const tableData = ref<GlobalLockRecord[]>([])
/** Total record count */
const totalRecords = ref(0)
/** Loading state */
const loading = ref(false)

const createDefaultState = (): QueryState => ({
  createTime: [],
  xid: '',
  tableName: '',
  transactionId: '',
  branchId: '',
  namespace: '',
  cluster: '',
  vgroup: '',
})

const formState = ref<QueryState>(createDefaultState())
const currentPage = ref(1)
const pageSize = ref(10)

/** Watch namespace change: cascade to clusters and vgroups */
watch(
  () => formState.value.namespace,
  (newNamespace) => {
    const firstCluster = onNamespaceChange(newNamespace || undefined)
    formState.value.cluster = firstCluster || ''
    formState.value.vgroup = ''
  },
)

/** Watch cluster change: cascade to vgroups */
watch(
  () => formState.value.cluster,
  (newCluster) => {
    const namespace = formState.value.namespace
    if (!namespace || !newCluster) {
      vgroupOptions.value = []
      formState.value.vgroup = ''
      return
    }
    onClusterChange(namespace, newCluster)
    formState.value.vgroup = ''
  },
)

/** After loading namespaces, set defaults if none selected */
watch(
  namespaceOptions,
  (options) => {
    if (options.length > 0 && !formState.value.namespace) {
      formState.value.namespace = options[0]
    }
  },
  { once: true },
)

/** Format millisecond timestamp to YYYY-MM-DD HH:mm:ss */
const formatTime = (ts: number): string => {
  if (!ts) return ''
  return dayjs(ts).format('YYYY-MM-DD HH:mm:ss')
}

const confirmAction = async (labelKey: string): Promise<boolean> => {
  try {
    await ElMessageBox.confirm(t(labelKey), t('common.confirm'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
    })
    return true
  } catch {
    return false
  }
}

const lockActions: RowAction[] = [
  { key: 'delete', labelKey: 'globalLock.actions.delete', buttonType: 'danger' },
]

const handleLockAction = async (key: string, row: unknown) => {
  if (key === 'delete') await handleDelete(row as GlobalLockRecord)
}

const handleSearch = async () => {
  const f = formState.value
  let timeStart: number | null = null
  let timeEnd: number | null = null
  if (f.createTime && f.createTime.length === 2) {
    timeStart = new Date(f.createTime[0]).getTime()
    timeEnd = new Date(f.createTime[1]).getTime() + 86400000 - 1
  }

  loading.value = true
  try {
    const res = await queryGlobalLocks({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      timeStart,
      timeEnd,
      xid: f.xid.trim() || undefined,
      tableName: f.tableName.trim() || undefined,
      transactionId: f.transactionId.trim() || undefined,
      branchId: f.branchId.trim() || undefined,
      namespace: f.namespace || undefined,
      cluster: f.cluster || undefined,
      vgroup: f.vgroup || undefined,
    })
    tableData.value = res.data
    totalRecords.value = res.total
  } catch {
    tableData.value = []
    totalRecords.value = 0
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  const prevNamespace = formState.value.namespace
  const prevCluster = formState.value.cluster
  formState.value = createDefaultState()
  formState.value.namespace = prevNamespace
  formState.value.cluster = prevCluster
  currentPage.value = 1
  handleSearch()
}

const handlePageChange = () => {
  handleSearch()
}

const handleDelete = async (row: GlobalLockRecord) => {
  if (!await confirmAction('globalLock.actions.delete')) return

  const operationParams = {
    xid: row.xid,
    branchId: row.branchId,
    namespace: formState.value.namespace || undefined,
    cluster: formState.value.cluster || undefined,
    vgroup: formState.value.vgroup || undefined,
  }

  loading.value = true
  try {
    // Step 1: Check if the lock exists and whether it may affect branch transactions
    const checkRes = await checkGlobalLock(operationParams)

    if (checkRes.data) {
      // Branch transactions may be affected, show warning
      try {
        await ElMessageBox.confirm(
          t('globalLock.warning.deleteWarning'),
          t('common.confirm'),
          {
            confirmButtonText: t('common.confirm'),
            cancelButtonText: t('common.cancel'),
            type: 'warning',
          },
        )
      } catch {
        return
      }
    }

    await deleteGlobalLock(operationParams)
    ElMessage.success(t('common.success'))
    await handleSearch()
  } catch {
    // Errors are handled in the request interceptor
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadNamespaces()
  if (namespaceOptions.value.length > 0 && !formState.value.namespace) {
    formState.value.namespace = namespaceOptions.value[0]
  }
  handleSearch()
})
</script>

<style lang="scss" scoped>
.global-lock-list-view {
  .page-top {
    .page-title {
      margin: 0;
      font-size: 38px;
      font-weight: 600;
      line-height: 1.15;
    }
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
    :deep(.el-select),
    :deep(.el-date-editor.el-input__wrapper),
    :deep(.el-date-editor.el-range-editor) {
      width: 100%;
      min-width: 0;
    }

    :deep(.el-date-editor.el-range-editor) {
      min-width: 240px;
      max-width: 100%;
    }

    .filter-item-span-2 {
      grid-column: span 2;
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

  @media (max-width: 1200px) {
    .filter-form {
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));

      .filter-item-span-2 {
        grid-column: span 1;
      }

      .filter-actions {
        grid-column: 1 / -1;
      }

      :deep(.el-date-editor.el-range-editor) {
        min-width: 100%;
      }
    }
  }

  @media (max-width: 768px) {
    .filter-form {
      grid-template-columns: 1fr;

      .filter-actions {
        justify-self: end;

        :deep(.el-form-item__content) {
          width: auto;
          justify-content: flex-end;
        }
      }
    }
  }

  .pagination-wrap {
    margin-top: 12px;
    display: flex;
    justify-content: flex-start;
  }
}
</style>
