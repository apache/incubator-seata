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
## Seata Compatibility Module

Starting from version 2.1.x, Seata's package name has been changed from `io.seata` to `org.apache.seata`.

### Overview

The `seata-all` compatibility module provides comprehensive backward compatibility support for APIs under the legacy `io.seata` package. This allows users to gradually migrate to the new `org.apache.seata` package without modifying existing code.

### Supported APIs

### 1. Core Context

#### `io.seata.core.context.RootContext`
API for managing global transaction context information.

**Main Methods:**
- `getXID()` - Get the current global transaction ID
- `bind(String xid)` - Bind a global transaction ID
- `unbind()` - Unbind the global transaction ID
- `inGlobalTransaction()` - Check if in a global transaction
- `getBranchType()` - Get the branch transaction type
- `bindBranchType(BranchType branchType)` - Bind branch transaction type
- `getTimeout()` - Get global transaction timeout
- `setTimeout(Integer timeout)` - Set global transaction timeout
- `bindGlobalLockFlag()` / `unbindGlobalLockFlag()` - Manage global lock flag
- `requireGlobalLock()` - Check if global lock is required
- `inTccBranch()` - Check if in TCC branch
- `inSagaBranch()` - Check if in SAGA branch

**Usage Example:**
```java
import io.seata.core.context.RootContext;

// Get global transaction ID
String xid = RootContext.getXID();

// Check if in global transaction
if (RootContext.inGlobalTransaction()) {
    // Handle global transaction logic
}
```

### 2. Transaction Clients

#### `io.seata.rm.RMClient`
Resource Manager (RM) client for initializing and managing resource managers.

**Main Features:**
- Register resource managers with the transaction coordinator
- Manage branch transaction commit/rollback

#### `io.seata.tm.TMClient`
Transaction Manager (TM) client for initializing and managing transaction managers.

**Main Features:**
- Register transaction managers with the transaction coordinator
- Manage global transaction start and commit

**Usage Example:**
```java
import io.seata.tm.TMClient;
import io.seata.rm.RMClient;

// Initialize TM and RM clients
TMClient.init("seata-tm", "default");
RMClient.init("seata-rm", "default");
```

### 3. Transaction Annotations

#### `io.seata.spring.annotation.GlobalTransactional`
Annotation for marking global transaction methods.

**Main Attributes:**
- `name` - Global transaction name
- `timeoutMills` - Global transaction timeout (milliseconds)
- `rollbackFor` - Specify exception types to trigger rollback
- `noRollbackFor` - Specify exception types that should not trigger rollback
- `propagation` - Transaction propagation strategy
- `lockRetryInterval` - Global lock retry interval
- `lockRetryTimes` - Global lock retry times
- `lockStrategyMode` - Global lock strategy mode (Pessimistic/Optimistic)

**Usage Example:**
```java
import io.seata.spring.annotation.GlobalTransactional;

@GlobalTransactional(name = "createOrder", rollbackFor = Exception.class)
public void createOrder(Order order) {
    // Business logic
}
```

#### Other Annotations
- `io.seata.spring.annotation.GlobalLock` - Global lock annotation
- `io.seata.spring.annotation.LocalLock` - Local lock annotation

### 4. DataSource Proxy

#### `io.seata.rm.datasource.DataSourceProxy`
DataSource proxy class for intercepting SQL statements and implementing automatic rollback in AT mode.

**Main Methods:**
- `getConnection()` - Get proxy connection
- `getTargetDataSource()` - Get target data source
- `getBranchType()` - Get branch transaction type

**Usage Example:**
```java
import io.seata.rm.datasource.DataSourceProxy;

@Bean
public DataSourceProxy dataSourceProxy(DataSource dataSource) {
    return new DataSourceProxy(dataSource);
}
```

### 5. Models and Enums

#### `io.seata.core.model.BranchType`
Enumeration for branch transaction types.

**Available Values:**
- `AT` - Auto Transaction (Automatic Compensation)
- `TCC` - Try-Confirm-Cancel (Two-Phase Confirmation)
- `SAGA` - Event-driven Distributed Transaction Processing
- `XA` - X/Open Architecture Distributed Transaction

#### `io.seata.core.model.GlobalStatus`
Enumeration for global transaction status.

**Available Values:**
- `Begin` - Transaction started
- `Committing` - Transaction committing
- `Committed` - Transaction committed
- `CommitFailed` - Transaction commit failed
- `Rolling` - Transaction rolling back
- `Rollbacked` - Transaction rolled back
- `RollbackFailed` - Transaction rollback failed

#### `io.seata.common.LockStrategyMode`
Global lock strategy mode.

**Available Values:**
- `PESSIMISTIC` - Pessimistic lock (default)
- `OPTIMISTIC` - Optimistic lock

### 6. Exception Handling

#### `io.seata.core.exception.TransactionException`
Base class for transaction exceptions.

#### `io.seata.core.exception.TransactionExceptionCode`
Enumeration for transaction exception codes.

**Main Exception Codes:**
- `BeginFailed` - Transaction begin failed
- `CommitFailed` - Transaction commit failed
- `RollbackFailed` - Transaction rollback failed
- `TimeoutRollback` - Timeout rollback
- `BranchTransactionNotExist` - Branch transaction does not exist

### 7. Serializers

#### `io.seata.core.serializer.Serializer`
Interface for implementing custom serialization methods.

**Supported Serializers:**
- `SeataSerializer` - Seata default serializer
- `ProtoStuffSerializer` - Protobuf serializer
- `KryoSerializer` - Kryo serializer
- `HessianSerializer` - Hessian serializer

### 8. Compressors

#### `io.seata.core.compressor.Compressor`
Interface for implementing data compression functionality.

**Supported Compression Algorithms:**
- `Gzip` - GZIP compression
- `Deflater` - Deflater compression
- `LZ4` - LZ4 compression
- `Bzip2` - Bzip2 compression
- `Zstd` - Zstandard compression

### 9. Service Discovery

#### `io.seata.discovery.registry.RegistryService`
Service registration and discovery interface.

#### `io.seata.discovery.registry.RegistryProvider`
SPI interface for service registry providers.

**Supported Registry Centers:**
- `Nacos` - Nacos registry
- `Eureka` - Eureka registry
- `Consul` - Consul registry
- `ZooKeeper` - ZooKeeper registry
- `etcd3` - etcd v3 registry

### 10. Configuration Center

#### `io.seata.core.config.ConfigurationProvider`
SPI interface for configuration providers.

**Supported Configuration Centers:**
- `Nacos` - Nacos configuration center
- `Apollo` - Apollo configuration center
- `ZooKeeper` - ZooKeeper configuration center
- `Consul` - Consul configuration center
- `etcd3` - etcd v3 configuration center

### 11. TCC Mode

#### `io.seata.rm.tcc.api.BusinessActionContext`
Business operation context for passing context information in TCC mode.

#### `io.seata.rm.tcc.api.LocalTCC`
Annotation for marking TCC business interfaces.

#### `io.seata.rm.tcc.api.TwoPhaseBusinessAction`
Annotation for marking Try, Confirm, and Cancel methods in two-phase business operations.

**Usage Example:**
```java
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import io.seata.rm.tcc.api.BusinessActionContext;

@LocalTCC
public interface OrderService {
    @TwoPhaseBusinessAction(name = "createOrder", commitMethod = "confirmCreateOrder", 
                           rollbackMethod = "cancelCreateOrder")
    boolean createOrder(BusinessActionContext context, Order order);
    
    boolean confirmCreateOrder(BusinessActionContext context, Order order);
    
    boolean cancelCreateOrder(BusinessActionContext context, Order order);
}
```

### 12. SAGA Mode

#### `io.seata.saga.engine.StateMachineEngine`
State machine execution engine.

#### `io.seata.saga.engine.StateMachineConfig`
State machine configuration.

#### `io.seata.saga.statelang.domain.State`
Base class for SAGA states.

**Usage Example:**
```java
import io.seata.saga.engine.StateMachineEngine;

@Bean
public StateMachineEngine stateMachineEngine() {
    // Configure state machine engine
    return stateMachineEngineImpl;
}
```

### 13. Integration API

#### `io.seata.integration.tx.api.interceptor.parser.InterfaceParser`
Interface parser for parsing RPC call interface information.

#### `io.seata.integration.http.TransactionPropagationInterceptor`
HTTP transaction propagation interceptor for propagating global transaction IDs in HTTP requests.

**Usage Example:**
```java
import io.seata.integration.http.TransactionPropagationInterceptor;

@Bean
public TransactionPropagationInterceptor transactionPropagationInterceptor() {
    return new TransactionPropagationInterceptor();
}
```

## Migration Guide

### Step 1: Add Dependency
```xml
<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-all</artifactId>
    <version>2.x.x</version>
</dependency>
```

### Step 2: Use Compatible APIs
Existing code using APIs from the `io.seata.*` package does not need to be modified. The system will automatically forward calls to the new `org.apache.seata.*` package.

### Step 3: Gradual Migration
It is recommended to gradually replace `io.seata.*` imports in your code with `org.apache.seata.*` to gain access to the latest features and optimizations.

## Important Notes

1. **@Deprecated Marker** - All compatibility APIs are marked as `@Deprecated`, and gradual migration to the `org.apache.seata.*` package is recommended.
2. **Feature Completeness** - The compatibility module supports most commonly used APIs from Seata 2.0.x versions.
3. **Performance Consideration** - The compatibility layer adds a small amount of method call overhead. It is recommended to migrate gradually after business stabilization.
4. **Documentation Reference** - For detailed API documentation, please refer to the official documentation of the `org.apache.seata.*` package.

## Related Resources

- [Seata Official Documentation](https://seata.apache.org/)
- [Seata GitHub Repository](https://github.com/apache/incubator-seata)
- [Contributing Guide](../CONTRIBUTING.md)

