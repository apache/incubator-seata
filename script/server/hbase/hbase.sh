#!/bin/bash
#source /etc/profile
$HBASE_HOME/bin/hbase shell <<EOF
create "seata:table","global","branches"
create "seata:statusTable","transactionId"
create "seata:lockTable","lock"
create "seata:lockKey","transactionId"
EOF