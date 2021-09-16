create table if not exists seata_state_machine_def
(
    id               varchar(32)  not null comment 'id',
    name             varchar(128) not null comment 'name',
    tenant_id        varchar(32)  not null comment 'tenant id',
    app_name         varchar(32)  not null comment 'application name',
    type             varchar(20) comment 'state language type',
    comment_         varchar(255) comment 'comment',
    ver              varchar(16)  not null comment 'version',
    gmt_create       timestamp(3)    not null comment 'create time',
    status           varchar(2)   not null comment 'status(AC:active|IN:inactive)',
    content          clob comment 'content',
    recover_strategy varchar(16) comment 'transaction recover strategy(compensate|retry)',
    primary key (id)
);

create table if not exists seata_state_machine_inst
(
    id                  varchar(128) not null comment 'id',
    machine_id          varchar(32) not null comment 'state machine definition id',
    tenant_id           varchar(32) not null comment 'tenant id',
    parent_id           varchar(128) comment 'parent id',
    gmt_started         timestamp(3)   not null comment 'start time',
    business_key        varchar(48) comment 'business key',
    start_params        clob comment 'start parameters',
    gmt_end             timestamp(3) comment 'end time',
    excep               blob comment 'exception',
    end_params          clob comment 'end parameters',
    status              varchar(2) comment 'status(SU succeed|FA failed|UN unknown|SK skipped|RU running)',
    compensation_status varchar(2) comment 'compensation status(SU succeed|FA failed|UN unknown|SK skipped|RU running)',
    is_running          tinyint(1) comment 'is running(0 no|1 yes)',
    gmt_updated         timestamp(3)   not null,
    primary key (id),
    unique key unikey_buz_tenant (business_key, tenant_id)
);

create table if not exists seata_state_inst
(
    id                       varchar(48)  not null comment 'id',
    machine_inst_id          varchar(128)  not null comment 'state machine instance id',
    name                     varchar(128) not null comment 'state name',
    type                     varchar(20) comment 'state type',
    service_name             varchar(128) comment 'service name',
    service_method           varchar(128) comment 'method name',
    service_type             varchar(16) comment 'service type',
    business_key             varchar(48) comment 'business key',
    state_id_compensated_for varchar(50) comment 'state compensated for',
    state_id_retried_for     varchar(50) comment 'state retried for',
    gmt_started              timestamp(3)    not null comment 'start time',
    is_for_update            tinyint(1) comment 'is service for update',
    input_params             clob comment 'input parameters',
    output_params            clob comment 'output parameters',
    status                   varchar(2)   not null comment 'status(SU succeed|FA failed|UN unknown|SK skipped|RU running)',
    excep                    blob comment 'exception',
    gmt_updated              timestamp(3) comment 'update time',
    gmt_end                  timestamp(3) comment 'end time',
    primary key (id, machine_inst_id)
);