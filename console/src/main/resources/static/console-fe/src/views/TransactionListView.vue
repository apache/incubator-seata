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
  <div class="page-container transaction-list-view">
    <el-card class="page-card">
      <div class="page-top">
        <el-breadcrumb separator="/" class="card-gap">
          <el-breadcrumb-item>{{ t('transaction.title') }}</el-breadcrumb-item>
          <el-breadcrumb-item>{{ t('transaction.subTitle') }}</el-breadcrumb-item>
        </el-breadcrumb>
        <h2 class="page-title">{{ t('transaction.title') }}</h2>
      </div>

      <el-form :inline="true" :model="formState" class="filter-form card-gap">
        <el-form-item :label="t('transaction.filters.createTime')" class="filter-item-span-2">
          <el-date-picker
            v-model="formState.createTime"
            type="daterange"
            unlink-panels
            value-format="YYYY-MM-DD"
            :range-separator="t('transaction.filters.to')"
            :start-placeholder="t('transaction.filters.startDate')"
            :end-placeholder="t('transaction.filters.endDate')"
          />
        </el-form-item>

        <el-form-item label="xid">
          <el-input v-model="formState.xid" :placeholder="t('transaction.filters.inputPlaceholder')" clearable />
        </el-form-item>

        <el-form-item label="applicationId">
          <el-input v-model="formState.applicationId" :placeholder="t('transaction.filters.inputPlaceholder')" clearable />
        </el-form-item>

        <el-form-item label="status">
          <el-select v-model="formState.status" :placeholder="t('transaction.filters.selectPlaceholder')" clearable>
            <el-option v-for="status in statusOptions" :key="status" :label="status" :value="status" />
          </el-select>
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
          <el-select v-model="formState.vgroup" :placeholder="t('transaction.filters.selectTransactionGroup')" clearable>
            <el-option v-for="vgroup in vgroupOptions" :key="vgroup" :label="vgroup" :value="vgroup" />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('transaction.filters.includeBranch')">
          <el-switch v-model="formState.includeBranch" />
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
        <el-table-column prop="transactionId" label="transactionId" min-width="170" show-overflow-tooltip />
        <el-table-column prop="applicationId" label="applicationId" min-width="180" show-overflow-tooltip />
        <el-table-column prop="transactionServiceGroup" label="transactionServiceGroup" min-width="170" show-overflow-tooltip />
        <el-table-column prop="transactionName" label="transactionName" min-width="140" show-overflow-tooltip />
        <el-table-column label="status" min-width="180">
          <template #default="{ row }">
            <el-button :type="getStatusButtonType(row.status)" size="small" plain>
              {{ GlobalStatusMap[row.status] ?? row.status }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="timeout" label="timeout" min-width="90" />
        <el-table-column label="beginTime" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row.beginTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="applicationData" label="applicationData" min-width="140" show-overflow-tooltip />
        <TableActionColumn
          :actions="visibleActions"
          :label="t('transaction.table.actions')"
          @action="handleAction"
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

    <BranchSessionDialog
      v-model:visible="branchDialogVisible"
      :xid="currentBranchXid"
      :branch-list="currentBranchList"
      @refresh="handleSearch"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import TableActionColumn, { type RowAction } from '@/components/TableActionColumn.vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import {
  queryGlobalSessions,
  deleteGlobalSession,
  forceDeleteGlobalSession,
  stopGlobalRetry,
  startGlobalRetry,
  sendCommitOrRollback,
  changeGlobalStatus,
  GlobalStatusMap,
  type GlobalSessionRecord,
  type GlobalSessionOperationParams,
} from '@/api/globalSession'
import { useNamespaceOptions } from '@/composables/useNamespace'
import BranchSessionDialog from '@/components/branch/BranchSessionDialog.vue'

interface QueryState {
  createTime: [string, string] | []
  xid: string
  applicationId: string
  status: string
  namespace: string
  cluster: string
  vgroup: string
  includeBranch: boolean
}

const { t } = useI18n()

const statusOptions = Object.values(GlobalStatusMap)

const {
  namespaceOptions,
  clusters: clusterOptions,
  vgroups: vgroupOptions,
  loadNamespaces,
  onNamespaceChange,
  onClusterChange,
} = useNamespaceOptions()

/** Current page data (returned by API) */
const tableData = ref<GlobalSessionRecord[]>([])
/** Total record count */
const totalRecords = ref(0)
/** Loading state */
const loading = ref(false)

const createDefaultState = (): QueryState => ({
  createTime: [],
  xid: '',
  applicationId: '',
  status: '',
  namespace: '',
  cluster: '',
  vgroup: '',
  includeBranch: false,
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

/** Branch session dialog state */
const branchDialogVisible = ref(false)
const currentBranchXid = ref('')
const currentBranchList = ref<unknown[]>([])

const rowActions: RowAction[] = [
  { key: 'detail', labelKey: 'transaction.actions.detail' },
  { key: 'viewBranch', labelKey: 'transaction.actions.viewBranch' },
  { key: 'deleteGlobal', labelKey: 'transaction.actions.deleteGlobal', buttonType: 'danger' },
  { key: 'forceDeleteGlobal', labelKey: 'transaction.actions.forceDeleteGlobal', buttonType: 'danger' },
  { key: 'stopRetry', labelKey: 'transaction.actions.stopRetry' },
  { key: 'startRetry', labelKey: 'transaction.actions.startRetry' },
  { key: 'submitRetryGlobal', labelKey: 'transaction.actions.submitRetryGlobal' },
  { key: 'updateStatus', labelKey: 'transaction.actions.updateStatus' },
]

/** All visible actions, filtered by includeBranch state */
const visibleActions = computed(() =>
  rowActions.filter((action) => {
    if (action.key === 'viewBranch' && !formState.value.includeBranch) return false
    return true
  }),
)

type StatusButtonType = '' | 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'text'

const statusButtonTypeMap: Record<string, StatusButtonType> = {
  // Phase 1
  Begin: 'primary',
  // Phase 2 - running
  Committing: 'primary',
  CommitRetrying: 'warning',
  Rollbacking: 'warning',
  RollbackRetrying: 'warning',
  TimeoutRollbacking: 'danger',
  TimeoutRollbackRetrying: 'danger',
  AsyncCommitting: 'primary',
  // Phase 2 - final: committed
  Committed: 'success',
  // Phase 2 - final: rollbacked
  Rollbacked: 'warning',
  TimeoutRollbacked: 'warning',
  // Phase 2 - final: failed
  CommitFailed: 'danger',
  RollbackFailed: 'danger',
  TimeoutRollbackFailed: 'danger',
  CommitRetryTimeout: 'danger',
  RollbackRetryTimeout: 'danger',
  // Others
  UnKnown: 'info',
  Finished: 'info',
  Deleting: 'info',
  StopCommitOrCommitRetry: 'info',
  StopRollbackOrRollbackRetry: 'info',
}

const getStatusButtonType = (status: number): StatusButtonType => {
  const name = GlobalStatusMap[status] ?? ''
  return statusButtonTypeMap[name] || 'info'
}

/** Format millisecond timestamp to YYYY-MM-DD HH:mm:ss */
const formatTime = (ts: number): string => {
  if (!ts) return ''
  return dayjs(ts).format('YYYY-MM-DD HH:mm:ss')
}

/** Convert status name to backend status code */
const statusNameToCode = (name: string): number | undefined => {
  const entry = Object.entries(GlobalStatusMap).find(([, v]) => v === name)
  return entry ? Number(entry[0]) : undefined
}

/** Build GlobalSessionOperationParams from form state and row */
const getOperationParams = (row: GlobalSessionRecord): GlobalSessionOperationParams => ({
  xid: row.xid,
  namespace: formState.value.namespace || undefined,
  cluster: formState.value.cluster || undefined,
  vgroup: formState.value.vgroup || undefined,
})

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
    const res = await queryGlobalSessions({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      timeStart,
      timeEnd,
      xid: f.xid.trim() || undefined,
      applicationId: f.applicationId.trim() || undefined,
      status: statusNameToCode(f.status),
      namespace: f.namespace || undefined,
      cluster: f.cluster || undefined,
      vgroup: f.vgroup || undefined,
      withBranch: f.includeBranch || undefined,
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

const actionHandlerMap: Record<string, (row: GlobalSessionRecord) => Promise<void>> = {
  detail: async (row) => {
    ElMessage.info(`${t('transaction.actions.detail')}: ${row.xid}`)
  },
  viewBranch: async (row) => {
    currentBranchXid.value = row.xid
    currentBranchList.value = (row as unknown as Record<string, unknown>).branchSessionVOs as unknown[] ?? []
    branchDialogVisible.value = true
  },
  deleteGlobal: async (row) => {
    if (!await confirmAction('transaction.actions.deleteGlobal')) return
    await deleteGlobalSession(getOperationParams(row))
    ElMessage.success(t('common.success'))
    await handleSearch()
  },
  forceDeleteGlobal: async (row) => {
    if (!await confirmAction('transaction.actions.forceDeleteGlobal')) return
    await forceDeleteGlobalSession(getOperationParams(row))
    ElMessage.success(t('common.success'))
    await handleSearch()
  },
  stopRetry: async (row) => {
    if (!await confirmAction('transaction.actions.stopRetry')) return
    await stopGlobalRetry(getOperationParams(row))
    ElMessage.success(t('common.success'))
    await handleSearch()
  },
  startRetry: async (row) => {
    if (!await confirmAction('transaction.actions.startRetry')) return
    await startGlobalRetry(getOperationParams(row))
    ElMessage.success(t('common.success'))
    await handleSearch()
  },
  submitRetryGlobal: async (row) => {
    if (!await confirmAction('transaction.actions.submitRetryGlobal')) return
    await sendCommitOrRollback(getOperationParams(row))
    ElMessage.success(t('common.success'))
    await handleSearch()
  },
  updateStatus: async (row) => {
    if (!await confirmAction('transaction.actions.updateStatus')) return
    await changeGlobalStatus(getOperationParams(row))
    ElMessage.success(t('common.success'))
    await handleSearch()
  },
}

const handleAction = async (actionKey: string, row: unknown) => {
  try {
    await actionHandlerMap[actionKey]?.(row as GlobalSessionRecord)
  } catch {
    // Errors are handled in the request interceptor
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
.transaction-list-view {
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
