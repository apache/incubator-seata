/*
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
 */
import { computed, ref } from 'vue'
import { fetchNamespaceV2 } from '@/api/clusterManager'

export interface NamespaceOption {
  clusters: string[]
  clusterVgroups: Record<string, string[]>
  clusterUnits: Record<string, string[]>
  clusterTypes: Record<string, string>
}

export type NamespaceOptions = Record<string, NamespaceOption>

export interface GroupEntry {
  namespace: string
  cluster: string
  vgroup: string
  clusterType: string
  units: string[]
}

export function useNamespaceOptions() {
  /** All namespace option keys (namespace names) */
  const namespaceOptions = ref<string[]>([])
  /** Full namespace data store for cascading lookups */
  const namespaceData = ref<NamespaceOptions>({})
  /** Available clusters for the selected namespace */
  const clusters = ref<string[]>([])
  /** Available vgroups for the selected namespace and cluster */
  const vgroups = ref<string[]>([])

  /** Flat list of all vgroups across all namespaces and clusters */
  const groupEntries = computed<GroupEntry[]>(() => {
    const entries: GroupEntry[] = []
    Object.keys(namespaceData.value).forEach((ns) => {
      const nsData = namespaceData.value[ns]
      nsData.clusters.forEach((cluster) => {
        const clusterType = nsData.clusterTypes[cluster] || 'default'
        const units = nsData.clusterUnits[cluster] || []
        ;(nsData.clusterVgroups[cluster] || []).forEach((vgroup) => {
          entries.push({ namespace: ns, cluster, vgroup, clusterType, units })
        })
      })
    })
    return entries
  })

  const loadNamespaces = async (): Promise<void> => {
    try {
      const res = await fetchNamespaceV2() as {
        code?: string
        data?: Record<string, {
          clusters?: Record<string, { vgroups?: string[]; units?: string[]; type?: string }>
        }>
      }
      const namespaces = res?.data ?? {}
      const options: NamespaceOptions = {}
      Object.keys(namespaces).forEach((namespaceKey) => {
        const namespaceItem = namespaces[namespaceKey]
        const clustersData = namespaceItem.clusters || {}
        const clusterVgroups: Record<string, string[]> = {}
        const clusterUnits: Record<string, string[]> = {}
        const clusterTypes: Record<string, string> = {}
        Object.keys(clustersData).forEach((clusterName) => {
          const cluster = clustersData[clusterName]
          clusterVgroups[clusterName] = cluster.vgroups || []
          clusterUnits[clusterName] = cluster.units || []
          clusterTypes[clusterName] = cluster.type || 'default'
        })
        options[namespaceKey] = {
          clusters: Object.keys(clustersData),
          clusterVgroups,
          clusterUnits,
          clusterTypes,
        }
      })
      namespaceData.value = options
      namespaceOptions.value = Object.keys(options)
    } catch {
      namespaceData.value = {}
      namespaceOptions.value = []
    }
  }

  /**
   * Handle namespace change: update clusters and reset cluster/vgroup
   * Returns the first cluster if available, or undefined
   */
  const onNamespaceChange = (namespace: string | undefined): string | undefined => {
    if (!namespace || !namespaceData.value[namespace]) {
      clusters.value = []
      vgroups.value = []
      return undefined
    }
    const ns = namespaceData.value[namespace]
    clusters.value = ns.clusters
    const firstCluster = ns.clusters.length > 0 ? ns.clusters[0] : undefined
    if (firstCluster) {
      vgroups.value = ns.clusterVgroups[firstCluster] || []
    } else {
      vgroups.value = []
    }
    return firstCluster
  }

  /**
   * Handle cluster change: update vgroups
   */
  const onClusterChange = (namespace: string | undefined, cluster: string | undefined): void => {
    if (!namespace || !cluster || !namespaceData.value[namespace]) {
      vgroups.value = []
      return
    }
    const ns = namespaceData.value[namespace]
    vgroups.value = ns.clusterVgroups[cluster] || []
  }

  /** Get available units for a given namespace and cluster */
  const getClusterUnits = (namespace: string, cluster: string): string[] => {
    return namespaceData.value[namespace]?.clusterUnits[cluster] ?? []
  }

  /** Get cluster type for a given namespace and cluster */
  const getClusterType = (namespace: string, cluster: string): string => {
    return namespaceData.value[namespace]?.clusterTypes[cluster] ?? 'default'
  }

  return {
    namespaceOptions,
    namespaceData,
    clusters,
    vgroups,
    groupEntries,
    loadNamespaces,
    onNamespaceChange,
    onClusterChange,
    getClusterUnits,
    getClusterType,
  }
}
