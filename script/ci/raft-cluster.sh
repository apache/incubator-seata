#!/usr/bin/env bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -euo pipefail

usage() {
  cat <<USAGE
Usage:
  $0 start --distribution <archive|url|version> --workspace <dir> [--env-file <path>] [--group <name>]
  $0 stop --workspace <dir>
USAGE
}

command=${1:-}
if [[ -z "$command" ]]; then
  usage
  exit 1
fi
shift || true

distribution=""
workspace=""
env_file=""
group="default"
readiness_mode="metadata"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --distribution)
      distribution="$2"
      shift 2
      ;;
    --workspace)
      workspace="$2"
      shift 2
      ;;
    --env-file)
      env_file="$2"
      shift 2
      ;;
    --group)
      group="$2"
      shift 2
      ;;
    --readiness-mode)
      readiness_mode="$2"
      shift 2
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

require_workspace() {
  if [[ -z "$workspace" ]]; then
    echo "--workspace is required" >&2
    exit 1
  fi
}

resolve_distribution_url() {
  local version="$1"
  local candidates=(
    "https://github.com/apache/incubator-seata/releases/download/v${version}/apache-seata-${version}-bin.tar.gz"
    "https://github.com/apache/incubator-seata/releases/download/v${version}/apache-seata-${version}-incubating-bin.tar.gz"
    "https://archive.apache.org/dist/incubator/seata/${version}/apache-seata-${version}-bin.tar.gz"
    "https://archive.apache.org/dist/incubator/seata/${version}/apache-seata-${version}-incubating-bin.tar.gz"
  )
  local candidate
  for candidate in "${candidates[@]}"; do
    if curl -fsIL "$candidate" >/dev/null 2>&1; then
      echo "$candidate"
      return 0
    fi
  done
  echo "Unable to resolve distribution archive for version ${version}" >&2
  return 1
}

download_distribution() {
  local input="$1"
  local downloads_dir="$workspace/downloads"
  mkdir -p "$downloads_dir"

  if [[ -f "$input" ]]; then
    echo "$input"
    return 0
  fi

  local source="$input"
  if [[ ! "$input" =~ ^https?:// ]]; then
    if ! source="$(resolve_distribution_url "$input")"; then
      exit 1
    fi
  fi

  local filename
  filename="$(basename "${source%%\?*}")"
  local target="$downloads_dir/$filename"
  curl -fsSL "$source" -o "$target"
  echo "$target"
}

extract_distribution() {
  local archive="$1"
  local extract_dir="$workspace/distribution"
  rm -rf "$extract_dir"
  mkdir -p "$extract_dir"
  case "$archive" in
    *.tar.gz|*.tgz)
      tar -xzf "$archive" -C "$extract_dir"
      ;;
    *.zip)
      unzip -q "$archive" -d "$extract_dir"
      ;;
    *)
      echo "Unsupported distribution format: $archive" >&2
      exit 1
      ;;
  esac
}

locate_server_home() {
  local server_home
  server_home="$(find "$workspace/distribution" -maxdepth 4 -type d -name seata-server | head -n 1)"
  if [[ -z "$server_home" ]]; then
    echo "Unable to locate seata-server home under $workspace/distribution" >&2
    exit 1
  fi
  if [[ ! -f "$server_home/target/seata-server.jar" ]]; then
    echo "Unable to locate seata-server.jar under $server_home/target" >&2
    exit 1
  fi
  echo "$server_home"
}

detect_host_ip() {
  local host_ip
  host_ip="$(hostname -I | awk '{print $1}')"
  if [[ -z "$host_ip" ]]; then
    echo "Unable to determine non-loopback host IP" >&2
    exit 1
  fi
  echo "$host_ip"
}

write_cluster_env() {
  local target_file="$1"
  local control_csv="$2"
  local metadata_csv="$3"
  local tx_csv="$4"
  local leader_control="$5"
  local leader_tx="$6"
  local leader_term="$7"
  cat >> "$target_file" <<EOF_ENV
SEATA_RAFT_WORKSPACE=$workspace
SEATA_RAFT_GROUP=$group
SEATA_RAFT_CONTROL_ADDRS=$control_csv
SEATA_RAFT_METADATA_ADDRS=$metadata_csv
SEATA_RAFT_TX_ADDRS=$tx_csv
SEATA_RAFT_LEADER_CONTROL_ADDR=$leader_control
SEATA_RAFT_LEADER_ADDR=$leader_tx
SEATA_RAFT_TERM=$leader_term
EOF_ENV
}

wait_for_cluster() {
  local metadata_csv="$1"
  local output_json="$workspace/cluster-metadata.json"
  python3 - "$metadata_csv" "$group" "$output_json" <<'PY'
import json
import sys
import time
import urllib.error
import urllib.request

metadata_addresses = sys.argv[1].split(',')
group = sys.argv[2]
outfile = sys.argv[3]
deadline = time.time() + 180
last_error = None
while time.time() < deadline:
    for metadata_address in metadata_addresses:
        url = f"http://{metadata_address}/metadata/v1/cluster?group={group}"
        try:
            with urllib.request.urlopen(url, timeout=5) as response:
                if response.status != 200:
                    continue
                payload = json.loads(response.read().decode('utf-8'))
                nodes = payload.get('nodes') or []
                leader = next((node for node in nodes if str(node.get('role', '')).upper() == 'LEADER'), None)
                if leader and leader.get('transaction') and leader.get('control'):
                    with open(outfile, 'w', encoding='utf-8') as handle:
                        json.dump(payload, handle)
                    sys.exit(0)
        except Exception as exc:
            last_error = exc
    time.sleep(2)
message = f"Timed out waiting for raft cluster readiness. Last error: {last_error}"
raise SystemExit(message)
PY
}

wait_for_tx_ports() {
  local tx_csv="$1"
  python3 - "$tx_csv" <<'PY'
import socket
import sys
import time

addresses = [entry.strip() for entry in sys.argv[1].split(',') if entry.strip()]
deadline = time.time() + 180
remaining = set(addresses)
last_error = None
while time.time() < deadline and remaining:
    for address in list(remaining):
        host, port = address.rsplit(':', 1)
        try:
            with socket.create_connection((host, int(port)), timeout=2):
                remaining.discard(address)
        except Exception as exc:
            last_error = exc
    if remaining:
        time.sleep(2)
if remaining:
    raise SystemExit(
        f"Timed out waiting for raft transaction ports: {','.join(sorted(remaining))}. Last error: {last_error}"
    )
PY
}

start_cluster() {
  if [[ -z "$distribution" ]]; then
    echo "--distribution is required for start" >&2
    exit 1
  fi
  require_workspace

  rm -rf "$workspace"
  mkdir -p "$workspace"

  local archive
  archive="$(download_distribution "$distribution")"
  extract_distribution "$archive"

  local server_home
  server_home="$(locate_server_home)"
  local pids_file="$workspace/pids"
  : > "$pids_file"
  local host_ip
  host_ip="$(detect_host_ip)"

  local -a controls tx_ports internal_ports
  controls=(7091 7092 7093)
  tx_ports=(8091 8092 8093)
  internal_ports=(9091 9092 9093)

  local internal_csv control_csv metadata_csv tx_csv
  internal_csv="${host_ip}:${internal_ports[0]},${host_ip}:${internal_ports[1]},${host_ip}:${internal_ports[2]}"
  control_csv="${host_ip}:${controls[0]},${host_ip}:${controls[1]},${host_ip}:${controls[2]}"
  tx_csv="${host_ip}:${tx_ports[0]},${host_ip}:${tx_ports[1]},${host_ip}:${tx_ports[2]}"
  metadata_csv="$control_csv"

  local i
  for i in 1 2 3; do
    local index=$((i - 1))
    local node_dir="$workspace/node-$i"
    mkdir -p "$node_dir/conf" "$node_dir/data" "$node_dir/logs"
    cat > "$node_dir/conf/application.yml" <<EOF_NODE
server:
  port: ${controls[$index]}
spring:
  application:
    name: seata-server
  main:
    web-application-type: servlet
logging:
  file:
    path: $node_dir/logs
seata:
  config:
    type: file
  registry:
    type: file
  server:
    service-port: ${tx_ports[$index]}
    enable-check-auth: false
    raft:
      group: $group
      server-addr: $internal_csv
      snapshot-interval: 30
      reporter-enabled: false
      sync: true
  store:
    mode: raft
    file:
      dir: $node_dir/data/sessionStore
  metrics:
    enabled: false
EOF_NODE

    nohup java \
      -Dloader.path="$server_home/lib" \
      -Dspring.config.location="file:$node_dir/conf/application.yml" \
      -Dserver.raftPort="${internal_ports[$index]}" \
      -jar "$server_home/target/seata-server.jar" \
      --host "$host_ip" \
      --serverNode "$i" \
      > "$node_dir/logs/server.out" 2>&1 &
    echo "$!" >> "$pids_file"
    echo "$!" > "$node_dir/pid"
  done

  local leader_control="" leader_tx="" leader_term="0"
  case "$readiness_mode" in
    metadata)
      wait_for_cluster "$metadata_csv"
      leader_control="$(python3 - "$workspace/cluster-metadata.json" <<'PY'
import json
import sys
with open(sys.argv[1], 'r', encoding='utf-8') as handle:
    payload = json.load(handle)
leader = next(node for node in payload['nodes'] if str(node.get('role', '')).upper() == 'LEADER')
print(f"{leader['control']['host']}:{leader['control']['port']}")
PY
      )"
      leader_tx="$(python3 - "$workspace/cluster-metadata.json" <<'PY'
import json
import sys
with open(sys.argv[1], 'r', encoding='utf-8') as handle:
    payload = json.load(handle)
leader = next(node for node in payload['nodes'] if str(node.get('role', '')).upper() == 'LEADER')
print(f"{leader['transaction']['host']}:{leader['transaction']['port']}")
PY
      )"
      leader_term="$(python3 - "$workspace/cluster-metadata.json" <<'PY'
import json
import sys
with open(sys.argv[1], 'r', encoding='utf-8') as handle:
    payload = json.load(handle)
print(payload.get('term', 0))
PY
      )"
      ;;
    tcp)
      wait_for_tx_ports "$tx_csv"
      ;;
    *)
      echo "Unsupported readiness mode: $readiness_mode" >&2
      exit 1
      ;;
  esac

  if [[ -n "$env_file" ]]; then
    write_cluster_env "$env_file" "$control_csv" "$metadata_csv" "$tx_csv" "$leader_control" "$leader_tx" "$leader_term"
  else
    write_cluster_env /dev/stdout "$control_csv" "$metadata_csv" "$tx_csv" "$leader_control" "$leader_tx" "$leader_term"
  fi
}

stop_cluster() {
  require_workspace
  local pids_file="$workspace/pids"
  if [[ -f "$pids_file" ]]; then
    while read -r pid; do
      if [[ -n "$pid" ]] && kill -0 "$pid" >/dev/null 2>&1; then
        kill "$pid" >/dev/null 2>&1 || true
        sleep 1
        if kill -0 "$pid" >/dev/null 2>&1; then
          kill -9 "$pid" >/dev/null 2>&1 || true
        fi
      fi
    done < <(tac "$pids_file")
  fi
}

case "$command" in
  start)
    start_cluster
    ;;
  stop)
    stop_cluster
    ;;
  *)
    usage
    exit 1
    ;;
esac
