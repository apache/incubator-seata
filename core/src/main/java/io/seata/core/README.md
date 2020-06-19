## request

rm client -> server

```
RegisterRMRequest

MergedWarpMessage

BranchRegisterRequest
BranchReportRequest
GlobalLockQueryRequest
```

tm client -> server

```
RegisterTMRequest

MergedWarpMessage

GlobalBeginRequest
GlobalCommitRequest
GlobalRollbackRequest
GlobalStatusRequest
GlobalReportRequest
```

server -> rm client

```
BranchCommitRequest
BranchRollbackRequest
UndoLogDeleteRequest
```

server -> tm client

```
// null
```

## response

Server -> rm client

```
RegisterRMResponse

MergeResultMessage
BranchRegisterResponse
BranchReportResponse
GlobalLockQueryResponse
```

Server -> tm client

```
RegisterTMResponse

MergeResultMessage
GlobalBeginResponse
GlobalCommitResponse
GlobalReportResponse
GlobalRollbackResponse
```

rm client -> server

```
BranchCommitResponse
BranchRollbackResponse
```

tm client -> server

```
// null
```

