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
  <el-dialog
    :model-value="visible"
    :title="t('branch.title') + ' - ' + session.xid"
    width="80%"
    class="branch-dialog"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-table v-loading="loading" :data="branchList" border style="width: 100%">
      <el-table-column prop="transactionId" :label="t('branch.table.transactionId')" min-width="170" show-overflow-tooltip />
      <el-table-column prop="branchId" :label="t('branch.table.branchId')" min-width="170" show-overflow-tooltip />
      <el-table-column prop="resourceGroupId" :label="t('branch.table.resourceGroupId')" min-width="140" show-overflow-tooltip />
      <el-table-column prop="branchType" :label="t('branch.table.branchType')" min-width="100" />
      <el-table-column :label="t('branch.table.status')" min-width="140">
        <template #default="{ row }">
          <el-button :type="branchStatuses[row.status]?.type ?? 'info'" size="small" plain>
            {{ branchStatuses[row.status]?.label ?? row.status }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column prop="resourceId" :label="t('branch.table.resourceId')" min-width="250" show-overflow-tooltip />
      <el-table-column prop="clientId" :label="t('branch.table.clientId')" min-width="220" show-overflow-tooltip />
      <el-table-column prop="applicationData" :label="t('branch.table.applicationData')" min-width="140" show-overflow-tooltip />
      <TableActionColumn
        :actions="branchActions"
        :label="t('branch.table.actions')"
        @action="handleAction"
      />
    </el-table>

    <div v-if="!branchList.length" class="empty-hint">
      {{ t('common.noData') }}
    </div>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">
        {{ t('common.close') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { ButtonType } from 'element-plus'
import type { GlobalSessionOperationParams } from '@/api/globalSession'
import TableActionColumn, { type RowAction } from '@/components/TableActionColumn.vue'
import {
  deleteBranchSession,
  forceDeleteBranchSession,
  stopBranchRetry,
  startBranchRetry,
} from '@/api/branchSession'

const { t } = useI18n()

const props = defineProps<{
  visible: boolean
  session: GlobalSessionOperationParams
  branchList: unknown[]
  loading: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  refresh: []
}>()

// Codes and names are defined by org.apache.seata.core.model.BranchStatus.
const branchStatuses: Record<number, { label: string; type: ButtonType }> = {
  0: { label: 'Unknown', type: 'info' },
  1: { label: 'Registered', type: 'primary' },
  2: { label: 'PhaseOne_Done', type: 'success' },
  3: { label: 'PhaseOne_Failed', type: 'danger' },
  4: { label: 'PhaseOne_Timeout', type: 'danger' },
  5: { label: 'PhaseTwo_Committed', type: 'success' },
  6: { label: 'PhaseTwo_CommitFailed_Retryable', type: 'warning' },
  7: { label: 'PhaseTwo_CommitFailed_Unretryable', type: 'danger' },
  8: { label: 'PhaseTwo_Rollbacked', type: 'warning' },
  9: { label: 'PhaseTwo_RollbackFailed_Retryable', type: 'warning' },
  10: { label: 'PhaseTwo_RollbackFailed_Unretryable', type: 'danger' },
  11: { label: 'PhaseTwo_CommitFailed_XAER_NOTA_Retryable', type: 'warning' },
  12: { label: 'PhaseTwo_RollbackFailed_XAER_NOTA_Retryable', type: 'warning' },
  13: { label: 'PhaseOne_RDONLY', type: 'success' },
  14: { label: 'STOP_RETRY', type: 'info' },
  15: { label: 'PhaseOne_PrepareFailed', type: 'danger' },
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

const branchActions: RowAction[] = [
  { key: 'delete', labelKey: 'branch.actions.delete', buttonType: 'danger' },
  { key: 'forceDelete', labelKey: 'branch.actions.forceDelete', buttonType: 'danger' },
  { key: 'stopRetry', labelKey: 'branch.actions.stopRetry' },
  { key: 'startRetry', labelKey: 'branch.actions.startRetry' },
]

const handleAction = async (key: string, row: unknown) => {
  const params = { ...props.session, branchId: (row as { branchId: string }).branchId }
  const labelMap: Record<string, string> = {
    delete: 'branch.actions.delete',
    forceDelete: 'branch.actions.forceDelete',
    stopRetry: 'branch.actions.stopRetry',
    startRetry: 'branch.actions.startRetry',
  }
  const labelKey = labelMap[key]
  if (!labelKey || !await confirmAction(labelKey)) return
  if (key === 'delete') await deleteBranchSession(params)
  else if (key === 'forceDelete') await forceDeleteBranchSession(params)
  else if (key === 'stopRetry') await stopBranchRetry(params)
  else if (key === 'startRetry') await startBranchRetry(params)
  ElMessage.success(t('common.success'))
  emit('refresh')
}
</script>

<style lang="scss" scoped>
.empty-hint {
  text-align: center;
  padding: 32px 0;
  color: #909399;
  font-size: 14px;
}

</style>
