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
  <div class="page-container group-manager-view">
    <el-card class="page-card">
      <div class="page-top">
        <el-breadcrumb separator="/" class="card-gap">
          <el-breadcrumb-item>{{ t('groupManager.title') }}</el-breadcrumb-item>
          <el-breadcrumb-item>{{ t('groupManager.subTitle') }}</el-breadcrumb-item>
        </el-breadcrumb>
        <h2 class="page-title">{{ t('groupManager.title') }}</h2>
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

        <el-form-item v-if="filterUnitOptions.length > 0" label="Unit">
          <el-select v-model="formState.unit" clearable>
            <el-option v-for="u in filterUnitOptions" :key="u" :label="u" :value="u" />
          </el-select>
        </el-form-item>

        <el-form-item class="filter-actions">
          <el-button type="primary" :loading="loading" @click="handleSearch">
            <el-icon><Search /></el-icon>
            {{ t('common.search') }}
          </el-button>
          <el-button type="success" @click="showCreateDialog">
            <el-icon><Plus /></el-icon>
            {{ t('groupManager.create.button') }}
          </el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="filteredEntries" border style="width: 100%">
        <el-table-column :label="t('groupManager.table.namespace')" prop="namespace" min-width="180" />
        <el-table-column :label="t('groupManager.table.cluster')" prop="cluster" min-width="180" />
        <el-table-column :label="t('groupManager.table.vgroup')" prop="vgroup" min-width="220" />
        <el-table-column :label="t('groupManager.table.clusterType')" min-width="140">
          <template #default="{ row }">
            {{ row.clusterType }}
          </template>
        </el-table-column>
        <el-table-column :label="t('groupManager.table.units')" min-width="200">
          <template #default="{ row }">
            {{ row.units.length > 0 ? row.units.join(', ') : '-' }}
          </template>
        </el-table-column>
        <TableActionColumn
          :actions="changeActions"
          :label="t('groupManager.table.actions')"
          @action="handleRowAction"
        />
      </el-table>
    </el-card>

    <!-- Create VGroup dialog -->
    <el-dialog
      v-model="createDialogVisible"
      :title="t('groupManager.create.dialogTitle')"
      width="560px"
      destroy-on-close
    >
      <el-form :model="createForm" label-width="120px">
        <el-form-item :label="t('groupManager.table.namespace')">
          <el-select v-model="createForm.namespace" class="full-width" @change="onCreateNamespaceChange">
            <el-option v-for="ns in namespaceOptions" :key="ns" :label="ns" :value="ns" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('groupManager.table.cluster')">
          <el-select v-model="createForm.cluster" class="full-width" @change="onCreateClusterChange">
            <el-option v-for="c in createClusterOpts" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="createShowUnit" label="Unit">
          <el-select v-model="createForm.unit" class="full-width">
            <el-option v-for="u in createUnitOpts" :key="u" :label="u" :value="u" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('groupManager.create.vgroupName')">
          <el-input
            v-model="createForm.vgroupName"
            :placeholder="t('groupManager.create.vgroupNamePlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">
          {{ t('groupManager.create.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Change VGroup dialog -->
    <el-dialog
      v-model="changeDialogVisible"
      :title="t('groupManager.change.dialogTitle')"
      width="600px"
      destroy-on-close
    >
      <el-form :model="changeForm" label-width="140px">
        <el-divider content-position="left">{{ t('groupManager.change.selectVGroup') }}</el-divider>
        <el-form-item :label="t('groupManager.change.originalNamespace')">
          <el-select v-model="changeForm.originalNamespace" class="full-width" @change="onChangeOriginalNsChange">
            <el-option v-for="ns in namespaceOptions" :key="ns" :label="ns" :value="ns" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('groupManager.change.originalCluster')">
          <el-select v-model="changeForm.originalCluster" class="full-width" @change="onChangeOriginalClusterChange">
            <el-option v-for="c in changeOriginalClusterOpts" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('groupManager.change.selectVGroup')">
          <el-select v-model="changeForm.selectedVGroup" class="full-width">
            <el-option v-for="vg in changeOriginalVGroupOpts" :key="vg" :label="vg" :value="vg" />
          </el-select>
        </el-form-item>

        <el-divider content-position="left">{{ t('groupManager.change.targetSection') }}</el-divider>
        <el-form-item :label="t('groupManager.change.targetNamespace')">
          <el-select v-model="changeForm.targetNamespace" class="full-width" @change="onChangeTargetNsChange">
            <el-option v-for="ns in namespaceOptions" :key="ns" :label="ns" :value="ns" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('groupManager.change.targetCluster')">
          <el-select v-model="changeForm.targetCluster" class="full-width" @change="onChangeTargetClusterChange">
            <el-option v-for="c in changeTargetClusterOpts" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="changeShowTargetUnit" :label="t('groupManager.change.targetUnit')">
          <el-select v-model="changeForm.targetUnit" class="full-width">
            <el-option v-for="u in changeTargetUnitOpts" :key="u" :label="u" :value="u" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changeDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="changeLoading" :disabled="changeBtnDisabled" @click="handleChange">
          {{ t('groupManager.change.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, watch, onMounted } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import TableActionColumn, { type RowAction } from '@/components/TableActionColumn.vue'
import { addGroup, changeGroup } from '@/api/groupManager'
import { useNamespaceOptions, type GroupEntry } from '@/composables/useNamespace'

const VGROUP_REFRESH_DELAY_MS = 6000

const { t } = useI18n()

const {
  namespaceOptions,
  namespaceData,
  clusters: clusterOptions,
  groupEntries,
  loadNamespaces,
  onNamespaceChange,
  getClusterUnits,
  getClusterType,
} = useNamespaceOptions()

/** Loading state for table */
const loading = ref(false)

/** ---- Filter ---- */
interface FormState {
  namespace: string
  cluster: string
  unit: string
}

const createDefaultFormState = (): FormState => ({
  namespace: '',
  cluster: '',
  unit: '',
})

const formState = ref<FormState>(createDefaultFormState())

const filterUnitOptions = computed<string[]>(() => {
  if (!formState.value.namespace || !formState.value.cluster) return []
  return getClusterUnits(formState.value.namespace, formState.value.cluster)
})

const filteredEntries = computed<GroupEntry[]>(() => {
  let list = groupEntries.value
  if (formState.value.namespace) {
    list = list.filter((e) => e.namespace === formState.value.namespace)
  }
  if (formState.value.cluster) {
    list = list.filter((e) => e.cluster === formState.value.cluster)
  }
  if (formState.value.unit) {
    list = list.filter((e) => e.units.includes(formState.value.unit))
  }
  return list
})

/** Watch namespace change: cascade to clusters and unit */
watch(
  () => formState.value.namespace,
  (newNamespace) => {
    const firstCluster = onNamespaceChange(newNamespace || undefined)
    formState.value.cluster = firstCluster || ''
    formState.value.unit = ''
  },
)

/** Watch cluster change: cascade to unit */
watch(
  () => formState.value.cluster,
  () => {
    formState.value.unit = ''
  },
)

/** After namespaces loaded, auto-select first namespace */
watch(
  namespaceOptions,
  (options) => {
    if (options.length > 0 && !formState.value.namespace) {
      formState.value.namespace = options[0]
    }
  },
  { once: true },
)

const handleSearch = () => {
  loading.value = true
  setTimeout(() => {
    loading.value = false
  }, 300)
}

/** ---- Table actions ---- */
const changeActions: RowAction[] = [
  { key: 'change', labelKey: 'groupManager.change.button' },
]

const handleRowAction = (key: string, row: unknown) => {
  if (key === 'change') {
    showChangeDialog(row as GroupEntry)
  }
}

/** ---- Create VGroup dialog ---- */
const createDialogVisible = ref(false)
const createLoading = ref(false)

const createForm = reactive<{
  namespace: string
  cluster: string
  unit: string
  vgroupName: string
}>({
  namespace: '',
  cluster: '',
  unit: '',
  vgroupName: '',
})

const createClusterOpts = computed(() => {
  if (!createForm.namespace || !namespaceData.value[createForm.namespace]) return []
  return namespaceData.value[createForm.namespace].clusters
})

const createUnitOpts = computed(() => {
  if (!createForm.namespace || !createForm.cluster) return []
  return getClusterUnits(createForm.namespace, createForm.cluster)
})

const createShowUnit = computed(() => {
  if (!createForm.namespace || !createForm.cluster) return false
  return getClusterType(createForm.namespace, createForm.cluster) !== 'default'
})

const onCreateNamespaceChange = () => {
  createForm.cluster = ''
  createForm.unit = ''
}

const onCreateClusterChange = () => {
  createForm.unit = createUnitOpts.value.length > 0 ? createUnitOpts.value[0] : ''
}

const resetCreateForm = () => {
  createForm.namespace = ''
  createForm.cluster = ''
  createForm.unit = ''
  createForm.vgroupName = ''
}

const showCreateDialog = () => {
  resetCreateForm()
  // Pre-fill from current filter selection
  if (formState.value.namespace) {
    createForm.namespace = formState.value.namespace
    if (formState.value.cluster) {
      createForm.cluster = formState.value.cluster
      onCreateClusterChange()
    }
  }
  createDialogVisible.value = true
}

const handleCreate = async () => {
  if (!createForm.namespace || !createForm.cluster || !createForm.vgroupName.trim()) {
    ElMessage.error(t('groupManager.create.errorMessage'))
    return
  }
  createLoading.value = true
  try {
    const createClusterType = getClusterType(createForm.namespace, createForm.cluster)
    const createUnitName = createClusterType !== 'default' ? createForm.unit : ''
    await addGroup(createForm.namespace, createForm.cluster, createForm.vgroupName.trim(), createUnitName)
    ElMessage.success(t('groupManager.create.success'))
    createDialogVisible.value = false
    setTimeout(() => {
      loadNamespaces()
    }, VGROUP_REFRESH_DELAY_MS)
  } catch (err: any) {
    const msg = err?.data?.message || ''
    ElMessage.error(msg ? `${t('groupManager.create.fail')}: ${msg}` : t('groupManager.create.fail'))
  } finally {
    createLoading.value = false
  }
}

/** ---- Change VGroup dialog ---- */
const changeDialogVisible = ref(false)
const changeLoading = ref(false)

const changeForm = reactive<{
  originalNamespace: string
  originalCluster: string
  selectedVGroup: string
  targetNamespace: string
  targetCluster: string
  targetUnit: string
}>({
  originalNamespace: '',
  originalCluster: '',
  selectedVGroup: '',
  targetNamespace: '',
  targetCluster: '',
  targetUnit: '',
})

// Source cluster options for change dialog
const changeOriginalClusterOpts = computed(() => {
  if (!changeForm.originalNamespace || !namespaceData.value[changeForm.originalNamespace]) return []
  return namespaceData.value[changeForm.originalNamespace].clusters
})

const changeOriginalVGroupOpts = computed(() => {
  if (!changeForm.originalNamespace || !changeForm.originalCluster) return []
  return namespaceData.value[changeForm.originalNamespace]?.clusterVgroups[changeForm.originalCluster] ?? []
})

// Target cluster options for change dialog
const changeTargetClusterOpts = computed(() => {
  if (!changeForm.targetNamespace || !namespaceData.value[changeForm.targetNamespace]) return []
  return namespaceData.value[changeForm.targetNamespace].clusters
})

const changeTargetUnitOpts = computed(() => {
  if (!changeForm.targetNamespace || !changeForm.targetCluster) return []
  return getClusterUnits(changeForm.targetNamespace, changeForm.targetCluster)
})

const changeShowTargetUnit = computed(() => {
  if (!changeForm.targetNamespace || !changeForm.targetCluster) return false
  return getClusterType(changeForm.targetNamespace, changeForm.targetCluster) !== 'default'
})

const changeBtnDisabled = computed(() => {
  if (!changeForm.selectedVGroup || !changeForm.targetNamespace || !changeForm.targetCluster) return true
  if (changeShowTargetUnit.value && !changeForm.targetUnit) return true
  return false
})

const onChangeOriginalNsChange = () => {
  changeForm.originalCluster = ''
  changeForm.selectedVGroup = ''
}

const onChangeOriginalClusterChange = () => {
  changeForm.selectedVGroup = ''
}

const onChangeTargetNsChange = () => {
  changeForm.targetCluster = ''
  changeForm.targetUnit = ''
}

const onChangeTargetClusterChange = () => {
  changeForm.targetUnit = changeTargetUnitOpts.value.length > 0 ? changeTargetUnitOpts.value[0] : ''
}

const resetChangeForm = () => {
  changeForm.originalNamespace = ''
  changeForm.originalCluster = ''
  changeForm.selectedVGroup = ''
  changeForm.targetNamespace = ''
  changeForm.targetCluster = ''
  changeForm.targetUnit = ''
}

const showChangeDialog = (row?: GroupEntry) => {
  resetChangeForm()
  if (row) {
    changeForm.originalNamespace = row.namespace
    changeForm.originalCluster = row.cluster
    changeForm.selectedVGroup = row.vgroup
  }
  changeDialogVisible.value = true
}

const handleChange = async () => {
  if (changeBtnDisabled.value) return
  try {
    await ElMessageBox.confirm(t('groupManager.change.confirmMessage'), t('common.confirm'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
    })
  } catch {
    return
  }
  changeLoading.value = true
  try {
    const targetClusterType = getClusterType(changeForm.targetNamespace, changeForm.targetCluster)
    const changeUnitName = targetClusterType !== 'default' ? changeForm.targetUnit : ''
    await changeGroup(changeForm.targetNamespace, changeForm.targetCluster, changeForm.selectedVGroup, changeUnitName)
    ElMessage.success(t('groupManager.change.success'))
    changeDialogVisible.value = false
    setTimeout(() => {
      loadNamespaces()
    }, VGROUP_REFRESH_DELAY_MS)
  } catch (err: any) {
    const msg = err?.data?.message || ''
    ElMessage.error(msg ? `${t('groupManager.change.fail')}: ${msg}` : t('groupManager.change.fail'))
  } finally {
    changeLoading.value = false
  }
}

onMounted(async () => {
  await loadNamespaces()
  if (formState.value.namespace && formState.value.cluster) {
    handleSearch()
  }
})
</script>

<style lang="scss" scoped>
.group-manager-view {
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

  .full-width {
    width: 100%;
  }
}
</style>
