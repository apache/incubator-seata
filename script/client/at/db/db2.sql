-- for AT mode you must to init this sql for you business database. the seata server not need it.
create table undo_log
(
    branch_id     bigint       not null,
    xid           varchar(128) not null,
    context       varchar(128) not null,
    rollback_info blob(2G)     not null,
    log_status    int          not null,
    log_created   TIMESTAMP    not null,
    log_modified  TIMESTAMP    not null
);

create unique index UX_UNDO_LOG
    on undo_log (branch_id, xid);