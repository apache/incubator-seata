#!/bin/bash
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

set -eo pipefail

distribution_dir=$(cd "$(dirname "$0")/../../.." && pwd)
test_dir=$(mktemp -d "${TMPDIR:-/tmp}/seata-server-test.XXXXXX")
java_cmd=$(command -v java)
clean_env=(env -u JAVA_TOOL_OPTIONS -u JDK_JAVA_OPTIONS -u _JAVA_OPTIONS)
launcher_pid=
case_dir=
owned_pids=()

running() {
    kill -0 "$1" 2>/dev/null && [[ $(ps -p "$1" -o stat=) != Z* ]]
}

stopped() {
    ! running "$1"
}

cleanup() {
    if [[ -n "$case_dir" && -f "$case_dir/jvm.pid" ]]; then
        owned_pids+=("$(cat "$case_dir/jvm.pid")")
    fi
    for pid in "${owned_pids[@]}"; do
        kill -TERM "$pid" 2>/dev/null || true
    done
    for ((attempt = 0; attempt < 50; attempt++)); do
        active=false
        for pid in "${owned_pids[@]}"; do
            if running "$pid"; then active=true; fi
        done
        if [[ "$active" == false ]]; then break; fi
        sleep 0.1
    done
    for pid in "${owned_pids[@]}"; do
        if running "$pid"; then kill -KILL "$pid" 2>/dev/null || true; fi
    done
    if [[ -n "$launcher_pid" ]]; then wait "$launcher_pid" 2>/dev/null || true; fi
    rm -rf "$test_dir"
}
trap cleanup EXIT
trap 'exit 1' HUP INT TERM

fail() {
    echo "FAIL: $*" >&2
    if [[ -n "$case_dir" ]]; then
        cat "$case_dir/stdout" "$case_dir/stderr" >&2
    fi
    exit 1
}

wait_for() {
    local description=$1
    shift
    for ((attempt = 0; attempt < 100; attempt++)); do
        if "$@"; then return; fi
        sleep 0.1
    done
    fail "timed out waiting for $description"
}

mkdir -p "$test_dir/classes"
cat > "$test_dir/LaunchFixture.java" <<'JAVA'
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

public class LaunchFixture {
    public static void main(String[] args) throws Exception {
        final Path home = Paths.get(System.getProperty("app.home"));
        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        Files.write(home.resolve("jvm.pid"), Collections.singletonList(pid), StandardCharsets.UTF_8);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.write(home.resolve("shutdown-hook"), Collections.singletonList("ran"), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
        Files.write(home.resolve("args"), Arrays.asList(args), StandardCharsets.UTF_8);
        System.out.println("FIXTURE_STDOUT");
        System.err.println("FIXTURE_STDERR");
        int exitCode = Integer.parseInt(System.getenv("SEATA_TEST_EXIT_CODE"));
        if (exitCode != 0) {
            System.exit(exitCode);
        }
        Files.write(home.resolve("ready"), Collections.singletonList("ready"), StandardCharsets.UTF_8);
        new CountDownLatch(1).await();
    }
}
JAVA
"${clean_env[@]}" javac -source 8 -target 8 -d "$test_dir/classes" "$test_dir/LaunchFixture.java"
"${clean_env[@]}" jar cfe "$test_dir/fixture.jar" LaunchFixture -C "$test_dir/classes" .

launch() {
    local name=$1 foreground=$2 exit_code=$3
    shift 3
    case_dir="$test_dir/$name"
    mkdir -p "$case_dir/bin" "$case_dir/conf" "$case_dir/target" "$case_dir/logs"
    cp "$distribution_dir/bin/seata-server.sh" "$distribution_dir/bin/seata-setup.sh" "$case_dir/bin/"
    cp "$test_dir/fixture.jar" "$case_dir/target/seata-server.jar"
    local foreground_env=()
    if [[ "$foreground" != unset ]]; then foreground_env=("SEATA_FOREGROUND=$foreground"); fi
    "${clean_env[@]}" -u SEATA_FOREGROUND "${foreground_env[@]}" \
        JAVACMD="$java_cmd" JAVA_OPT= JAVA_OPTS= JMX_OPTS= JMX_ENABLE=false SKYWALKING_ENABLE=false \
        JVM_XMX=64m JVM_XMS=32m JVM_XSS=512k JVM_MetaspaceSize=32m JVM_MaxMetaspaceSize=128m \
        JVM_MaxDirectMemorySize=32m LOADER_PATH= LOG_HOME="$case_dir/logs" \
        SEATA_TEST_EXIT_CODE="$exit_code" bash "$case_dir/bin/seata-server.sh" "$@" \
        > "$case_dir/stdout" 2> "$case_dir/stderr" &
    launcher_pid=$!
    owned_pids=("$launcher_pid")
}

collect_jvm_pid() {
    wait_for "the JVM PID" test -s "$case_dir/jvm.pid"
    jvm_pid=$(cat "$case_dir/jvm.pid")
    [[ "$jvm_pid" =~ ^[0-9]+$ ]] || fail "invalid JVM PID: $jvm_pid"
    owned_pids+=("$jvm_pid")
}

stop_jvm() {
    kill -TERM "$jvm_pid"
    wait_for "the shutdown hook" test -s "$case_dir/shutdown-hook"
    wait_for "the JVM to exit" stopped "$jvm_pid"
    wait "$launcher_pid" 2>/dev/null || true
    launcher_pid=
    case_dir=
    owned_pids=()
}

options=(-h 127.0.0.1 -p 8091 -m file -n 1 -e test -ssm file -lsm file)
launch foreground true 0 start "${options[@]}"
wait_for "foreground startup" test -s "$case_dir/ready"
collect_jvm_pid
[[ "$launcher_pid" == "$jvm_pid" ]] || fail "foreground launcher PID $launcher_pid differs from JVM PID $jvm_pid"
printf '%s\n' "${options[@]}" > "$case_dir/expected-args"
cmp -s "$case_dir/expected-args" "$case_dir/args" || fail "start/options were not forwarded correctly"
stop_jvm

launch startup-failure true 37
wait_for "startup failure" stopped "$launcher_pid"
if wait "$launcher_pid"; then exit_code=0; else exit_code=$?; fi
[[ "$exit_code" == 37 ]] || fail "startup failure exit code was $exit_code, expected 37"
grep -q '^FIXTURE_STDOUT$' "$case_dir/stdout" || fail "startup stdout was lost"
grep -q '^FIXTURE_STDERR$' "$case_dir/stderr" || fail "startup stderr was lost"
launcher_pid=
case_dir=
owned_pids=()

for foreground in false unset; do
    launch "background-$foreground" "$foreground" 0
    wait_for "background launcher to exit" stopped "$launcher_pid"
    if wait "$launcher_pid"; then exit_code=0; else exit_code=$?; fi
    [[ "$exit_code" == 0 ]] || fail "background launcher exited with $exit_code"
    wait_for "background startup" test -s "$case_dir/ready"
    collect_jvm_pid
    [[ "$launcher_pid" != "$jvm_pid" ]] || fail "background mode replaced the launcher"
    running "$jvm_pid" || fail "background JVM is not running"
    stop_jvm
done

echo "PASS: foreground shutdown, startup failure, and background defaults"
