<!--
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
-->
# Seata Benchmark CLI Tool

A command-line benchmark tool for stress testing Seata transaction modes.

## Features

- Support for **AT**, **TCC**, **SAGA**, and **SAGA_ANNOTATION** transaction modes
- **Dual execution modes**:
  - **Empty mode** (`--branches 0`): Pure Seata protocol overhead testing
  - **Real mode** (`--branches N`): Actual distributed transaction execution
- **Configurable TPS** (Transactions Per Second) control
- **Multi-threaded** workload generation
- **Fault injection** with configurable rollback percentage
- **Selectable SAGA state machine shapes** such as `simple` and `order`
- **Selectable SAGA workload implementations** such as `mock` and `db`
- **Step-targeted SAGA failure injection** for forward steps such as `inventory`, `payment`, and `order`
- **Reproducible SAGA failure injection** with `--saga-random-seed`
- **Step-targeted SAGA timeout simulation** with `--saga-timeout-step` and `--saga-timeout-ms`
- **Window-based progress reporting** (every 10 seconds)
- Performance metrics collection (latency percentiles, success rate, TPS)
- **CSV export** for post-analysis
- **Warmup support** (exclude initial ramp-up from final statistics)
- **YAML configuration file** support

## Prerequisites

- JDK 8 or higher
- Maven 3.6+
- Running Seata Server
- Docker (required for real mode with Testcontainers)

## Build

```bash
cd test-suite/seata-benchmark-cli
../../mvnw clean package
```

The executable JAR will be created at `target/seata-benchmark-cli.jar`

## Usage

### Basic Usage

```bash
# AT mode benchmark (empty transaction)
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode AT \
  --tps 100 \
  --threads 1 \
  --duration 60

# TCC mode benchmark (empty transaction)
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode TCC \
  --tps 100 \
  --threads 1 \
  --duration 60

# SAGA mode benchmark (empty transaction)
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode SAGA \
  --tps 100 \
  --threads 1 \
  --duration 60

# SAGA_ANNOTATION mode benchmark (empty transaction)
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode SAGA_ANNOTATION \
  --tps 100 \
  --threads 1 \
  --duration 60
```

### Real Mode (with actual database operations)

```bash
# AT mode with real MySQL transactions (via Testcontainers)
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode AT \
  --tps 100 \
  --threads 1 \
  --duration 60 \
  --branches 3

# TCC mode with real try/confirm/cancel
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode TCC \
  --tps 100 \
  --threads 1 \
  --duration 60 \
  --branches 3

# SAGA mode with state machine engine
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode SAGA \
  --tps 100 \
  --threads 1 \
  --duration 60 \
  --branches 3 \
  --rollback-percentage 5

# SAGA mode with explicit order state machine shape
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode SAGA \
  --tps 100 \
  --threads 1 \
  --duration 60 \
  --branches 3 \
  --saga-shape order

# SAGA mode with DB-backed business actions (via Testcontainers MySQL)
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode SAGA \
  --tps 100 \
  --threads 1 \
  --duration 60 \
  --branches 3 \
  --saga-shape order \
  --saga-workload db

# SAGA mode with payment-step failure injection
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode SAGA \
  --tps 100 \
  --threads 1 \
  --duration 60 \
  --branches 3 \
  --rollback-percentage 20 \
  --saga-fail-step payment

# SAGA mode with reproducible payment-step failure injection
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode SAGA \
  --tps 100 \
  --duration 60 \
  --branches 3 \
  --saga-shape order \
  --rollback-percentage 20 \
  --saga-fail-step payment \
  --saga-random-seed 123

# SAGA mode with payment-step timeout simulation
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode SAGA \
  --tps 10 \
  --threads 1 \
  --duration 10 \
  --branches 3 \
  --saga-shape order \
  --saga-timeout-step payment \
  --saga-timeout-ms 3000
```

### Performance Testing Modes

The benchmark CLI supports two primary testing modes:

#### 1. Fixed Concurrency Mode (推荐用于压力测试)
- Set fixed thread count (`--threads`)
- Set TPS to a very high value (e.g., `--tps 100000`) to avoid rate limiting
- Tests maximum throughput under fixed concurrency

Example:
```bash
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode AT \
  --threads 50 \
  --tps 100000 \
  --duration 60
```

#### 2. Fixed TPS Mode (推荐用于容量规划)
- Set target TPS (`--tps`)
- Use single thread or few threads (`--threads 1`)
- Tests latency and resource usage at target throughput

Example:
```bash
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode AT \
  --tps 500 \
  --threads 1 \
  --duration 60
```

**Note**: Avoid using both fixed TPS and multiple threads simultaneously, as this leads to unclear test semantics. The benchmark will reject such configurations.

### Advanced Options

```bash
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode AT \
  --tps 100000 \
  --threads 50 \
  --duration 300 \
  --warmup-duration 30 \
  --rollback-percentage 2 \
  --branches 3 \
  --export-csv results.csv \
  --application-id my-benchmark-app \
  --tx-service-group my_tx_group
```

### All CLI Options

```
Usage: seata-benchmark [-hV] [--application-id=<applicationId>]
                       [-d=<duration>] [--export-csv=<exportCsv>]
                       [-m=<mode>] [-s=<server>] [-t=<targetTps>]
                       [--saga-shape=<sagaShape>]
                       [--saga-workload=<sagaWorkload>]
                       [--saga-fail-step=<sagaFailStep>]
                       [--saga-random-seed=<sagaRandomSeed>]
                       [--saga-timeout-step=<sagaTimeoutStep>]
                       [--saga-timeout-ms=<sagaTimeoutMs>]
                       [--threads=<threads>] [--tx-service-group=<txServiceGroup>]
                       [--warmup-duration=<warmupDuration>]
                       [--rollback-percentage=<rollbackPercentage>]
                       [--branches=<branches>]

Options:
  -s, --server=<server>                Seata Server address (host:port)
  -m, --mode=<mode>                    Transaction mode: AT, TCC, SAGA, or SAGA_ANNOTATION
  -t, --tps=<targetTps>                Target TPS (default: 100)
      --threads=<threads>              Concurrent threads (default: 10)
  -d, --duration=<duration>            Duration in seconds (default: 60)
      --warmup-duration=<warmupDuration>
                                       Warmup duration in seconds (default: 0)
      --rollback-percentage=<rollbackPercentage>
                                       Rollback percentage for fault injection (0-100, default: 2)
      --saga-shape=<sagaShape>         Select SAGA state machine shape: simple or order.
                                       If omitted, the benchmark keeps the existing
                                       branches-based compatibility behavior.
      --saga-workload=<sagaWorkload>   Select SAGA workload implementation: mock or db.
                                       The default is mock. The db workload uses
                                       Testcontainers MySQL for DB-backed order,
                                       inventory, and payment actions.
      --saga-fail-step=<sagaFailStep>  Restrict SAGA failure injection to one forward step:
                                       inventory, payment, or order.
                                       The failure ratio is still controlled by
                                       --rollback-percentage.
      --saga-random-seed=<sagaRandomSeed>
                                       Optional random seed for reproducible SAGA
                                       failure injection behavior.
      --saga-timeout-step=<sagaTimeoutStep>
                                       Simulate SAGA timeout at one forward step:
                                       inventory, payment, or order.
      --saga-timeout-ms=<sagaTimeoutMs>
                                       Simulated timeout delay in milliseconds for
                                       SAGA timeout injection (default: 3000).
      --branches=<branches>            Number of branch transactions
                                       0 = empty mode (protocol overhead only)
                                       >=1 = real mode (actual execution)
      --export-csv=<exportCsv>         Export metrics to CSV file
      --application-id=<applicationId>
                                       Seata application ID (default: benchmark-app)
      --tx-service-group=<txServiceGroup>
                                       Seata tx service group (default: default_tx_group)
  -h, --help                           Show this help message
  -V, --version                        Print version information
```

## Configuration File

The benchmark tool supports YAML configuration files. Configuration priority (highest to lowest):

1. CLI arguments
2. Environment variable `BENCHMARK_CONFIG_FILE`
3. System property `benchmark.config.file`
4. Default classpath `benchmark.yaml`

### Example Configuration

```yaml
# benchmark.yaml
server: 127.0.0.1:8091
mode: AT
targetTps: 100
threads: 10
duration: 60
warmupDuration: 10
rollbackPercentage: 2
branches: 0
applicationId: benchmark-app
txServiceGroup: default_tx_group
```

## Output

### Console Progress

During execution, the tool displays progress every 10 seconds:

```
Starting benchmark...

[00:10] 1000 txns, 100.2 txns/sec, 99.0% success
[00:20] 2000 txns, 100.1 txns/sec, 99.2% success
[00:30] 3000 txns, 99.8 txns/sec, 99.5% success
...
```

### Final Report

When the benchmark completes, a final report is displayed:

```
===================================================
           Seata Benchmark Final Report
===================================================
Mode:                  SAGA
Saga Workload:         db
Saga Shape:            order
Total Transactions:    6,000
Success Count:         5,780
Failed Count:          220
Success Rate:          96.33%
Committed Count:       4,860
Compensated Count:     920
Execution Failed Count: 180
Compensation Failed Count: 40
Unknown Count:         0
Committed Rate:        81.00%
Compensated Rate:      15.33%
End-State Success Rate: 96.33%
Average TPS:           100.0
Elapsed Time:          60 seconds

Latency Statistics:
  P50:                 12 ms
  P95:                 45 ms
  P99:                 89 ms
  P99.9:               120 ms
  Max:                 230 ms
===================================================
```

For timeout simulation scenarios, the reported `Elapsed Time` may slightly exceed the configured `--duration` because already-started transactions are allowed to finish before the workload generator stops.

For DB-backed SAGA scenarios, the final report and CSV output also include `Saga Workload`, so benchmark results from `mock` and `db` workloads can be compared explicitly.

### CSV Export

Use `--export-csv` to export metrics:

```bash
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode AT \
  --tps 100 \
  --duration 60 \
  --export-csv results.csv
```

Output format:

```csv
Metric,Value
Mode,SAGA
Saga Workload,db
Saga Shape,order
Total Transactions,6000
Success Count,5780
Failed Count,220
Success Rate (%),96.33
Committed Count,4860
Compensated Count,920
Execution Failed Count,180
Compensation Failed Count,40
Unknown Count,0
Committed Rate (%),81.00
Compensated Rate (%),15.33
End-State Success Rate (%),96.33
Average TPS,100.0
Elapsed Time (s),60
Latency P50 (ms),12
Latency P95 (ms),45
Latency P99 (ms),89
Latency P99.9 (ms),120
Latency Max (ms),230
Export Time,2025-12-01 10:30:45
```

## Examples

### Test AT Mode Performance (Empty Transaction)

```bash
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode AT \
  --tps 100 \
  --duration 60
```

### Test AT Mode with Real Database Operations

```bash
# Requires Docker for Testcontainers MySQL
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode AT \
  --tps 100 \
  --duration 60 \
  --branches 3
```

### Test SAGA Mode with State Machine Engine

```bash
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode SAGA \
  --tps 100 \
  --duration 60 \
  --branches 3 \
  --rollback-percentage 5
```

### Test SAGA Mode with Payment-Step Failure Injection

```bash
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode SAGA \
  --tps 100 \
  --duration 60 \
  --branches 3 \
  --rollback-percentage 20 \
  --saga-fail-step payment
```

### Test SAGA Mode with DB-Backed Business Actions

```bash
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode SAGA \
  --tps 100 \
  --duration 60 \
  --branches 3 \
  --saga-shape order \
  --saga-workload db
```

### Test DB-Backed SAGA with Payment-Step Failure Injection

```bash
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode SAGA \
  --tps 10 \
  --threads 1 \
  --duration 10 \
  --branches 3 \
  --saga-shape order \
  --saga-workload db \
  --rollback-percentage 20 \
  --saga-fail-step payment
```

### Test SAGA_ANNOTATION Mode (Annotation-based Compensation)

```bash
# Empty mode: protocol overhead only
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode SAGA_ANNOTATION \
  --tps 100 \
  --duration 60

# Real mode: 3 branches per transaction, 5% compensation rate
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode SAGA_ANNOTATION \
  --tps 100 \
  --duration 60 \
  --branches 3 \
  --rollback-percentage 5
```

### Compare SAGA vs SAGA_ANNOTATION

```bash
# State-machine Saga
java -jar seata-benchmark-cli.jar --server 127.0.0.1:8091 \
  --mode SAGA --tps 10000 --threads 50 --duration 60 --branches 3

# Annotation-based Saga
java -jar seata-benchmark-cli.jar --server 127.0.0.1:8091 \
  --mode SAGA_ANNOTATION --tps 10000 --threads 50 --duration 60 --branches 3
```

### Test TCC Mode (try/confirm/cancel)

```bash
# Empty mode: protocol overhead only (fixed TPS)
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode TCC \
  --tps 100 \
  --threads 1 \
  --duration 60

# Empty mode: max throughput (fixed concurrency)
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode TCC \
  --tps 100000 \
  --threads 50 \
  --duration 60

# Real mode: 3 branches per transaction, 10% cancel rate
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode TCC \
  --tps 100 \
  --threads 1 \
  --duration 60 \
  --branches 3 \
  --rollback-percentage 10
```

### Test with Warmup

```bash
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode AT \
  --tps 200 \
  --threads 1 \
  --duration 120 \
  --warmup-duration 30
```

## Implementation Details

### Transaction Modes

| Mode             | Empty Mode (branches=0) | Real Mode (branches>0) |
|------------------|-------------------------|------------------------|
| AT               | Pure protocol overhead  | MySQL via Testcontainers (account transfer) |
| TCC              | Pure protocol overhead  | Real try/confirm/cancel via @LocalTCC interceptor |
| SAGA             | Mock simulation         | State machine engine with compensation |
| SAGA_ANNOTATION  | Pure protocol overhead  | Annotation interceptor + TC compensation callback |

### Empty Transaction Mode

For accurate benchmarking of Seata Server capacity, the tool executes **empty transactions**:

- **AT Mode**: Only `begin()` and `commit()` operations, no SQL execution
- **TCC Mode**: Empty global transaction, no branch registration
- **SAGA Mode**: Simplified simulation without state machine
- **SAGA_ANNOTATION Mode**: Empty global transaction, no branch registration

This approach:
- Measures pure Seata protocol overhead
- Eliminates database performance variability
- Provides consistent baseline metrics
- Requires no database configuration

### Real Transaction Mode

When `--branches` is set to a value greater than 0:

- **AT Mode**:
  - Starts MySQL container via Testcontainers
  - Creates accounts table with initial test data
  - Executes real account transfer operations
  - Each branch performs debit/credit operations

- **TCC Mode**:
  - Uses `@LocalTCC` + `@TwoPhaseBusinessAction` annotation path
  - Each branch is registered via the TCC interceptor (JDK dynamic proxy, no Spring required)
  - `try` (prepare) runs in the business thread; `commit` / `rollback` are invoked by the TC
  - No-op implementation so the benchmark measures pure TCC protocol overhead
  - `--branches N` = N TCC branch registrations per transaction

- **SAGA Mode**:
  - Uses Seata state machine engine
  - Executes predefined state machine definitions
  - Supports `mock` and `db` workloads
  - Supports compensation on failure
  - Available state machines:
    - `benchmarkSimpleSaga`: For 1-2 branches
    - `benchmarkOrderSaga`: For 3+ branches (order/inventory/payment)
  - DB workload behavior:
    - Starts MySQL via Testcontainers
    - Creates benchmark inventory, account, and order tables
    - Executes DB-backed inventory/payment/order actions with compensation

### SAGA Workloads

- `mock` workload:
  - Keeps the lightweight benchmark-oriented implementation
  - Uses in-memory Saga services with simulated delay, failure injection, and timeout injection
  - Is the default workload and preserves backward compatibility

- `db` workload:
  - Starts a MySQL container via Testcontainers
  - Initializes benchmark tables for inventory, account, and order data
  - Executes DB-backed order, inventory, and payment actions while still supporting the same Saga shape, fail-step, random-seed, and timeout options
  - Is intended for more realistic business-style Saga benchmarking

- **SAGA_ANNOTATION Mode**:
  - Uses `@SagaTransactional` + `@CompensationBusinessAction` annotation path
  - Each branch is registered via the annotation interceptor (JDK dynamic proxy, no Spring required)
  - On rollback, the TC invokes the compensation method for every registered branch
  - `--branches N` = N `@CompensationBusinessAction` calls per transaction

### Fault Injection

Use `--rollback-percentage` to simulate transaction failures:

```bash
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode SAGA \
  --tps 100 \
  --duration 60 \
  --branches 3 \
  --rollback-percentage 10  # 10% of transactions will trigger rollback/compensation
```

### Warmup Support

Use `--warmup-duration` to exclude JVM warmup period from final statistics:

```bash
java -jar seata-benchmark-cli.jar \
  --server 127.0.0.1:8091 \
  --mode AT \
  --tps 100 \
  --duration 120 \
  --warmup-duration 30  # First 30 seconds excluded from final stats
```

### Latency Sampling

To prevent OOM on large-scale tests, the tool samples latencies (inspired by Kafka ProducerPerformance):
- Maximum 500,000 latency samples stored
- For tests with >500K transactions, every Nth transaction is sampled

## Troubleshooting

### Connection Issues

If you see connection errors:

1. Verify Seata Server is running on the configured address
2. Check network connectivity
3. Ensure Seata Server version compatibility

### Performance Issues

If TPS is lower than expected:

1. Increase thread count (`--threads`)
2. Check Seata Server resource usage
3. Verify network latency
4. Try empty mode first (`--branches 0`) to isolate Seata overhead

### Docker Issues (Real Mode)

If real mode fails:

1. Verify Docker is running
2. Check Docker has sufficient resources
3. Ensure network connectivity to Docker Hub for image pulling

## Logs

Logs are written to `seata-benchmark.log` in the current directory.

## Roadmap

### Current Version (v1.0)
- AT, TCC, SAGA, and SAGA_ANNOTATION mode support
- Empty and real transaction modes
- YAML configuration file support
- Fault injection (rollback percentage)
- Window-based progress reporting
- Warmup support
- CSV export

### Future Versions

**v1.1 - Enhancement:**
- P99.9 percentile
- XA mode support

**v2.0 - Advanced Features:**
- Real-time TUI (Terminal User Interface)
- Monitoring integration (global_table/branch_table)
- Distributed benchmark coordination
- Custom business scenario plugins

## License

Licensed under the Apache License, Version 2.0
