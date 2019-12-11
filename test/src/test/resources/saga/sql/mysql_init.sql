create table if not exists seata_state_machine_def
(
    id varchar(32) not null comment 'id',
    name varchar(255) not null comment 'name',
    tenant_id varchar(32) not null comment 'tenant id',
    app_name varchar(32) not null comment 'application name',
    type varchar(20) comment 'state language type',
    comment_ varchar(255) comment 'comment',
    ver varchar(16) not null  comment 'version',
    gmt_create datetime(3) not null comment 'create time',
    status varchar(2) not null comment 'status(AC:active|IN:inactive)',
    content longtext comment 'content',
    recover_strategy varchar(16) comment 'transaction recover strategy(compensate|retry)',
    primary key(id)
) comment 'state machine definition';

create table if not exists seata_state_machine_inst
(
    id varchar(46) not null comment 'id',
    machine_id varchar(32) not null comment 'state machine definition id',
    tenant_id varchar(32) not null comment 'tenant id',
    parent_id varchar(46) comment 'parent id',
    gmt_started datetime(3) not null comment 'start time',
    business_key varchar(48) comment 'business key',
    start_params longtext comment 'start parameters',
    gmt_end datetime(3) comment 'end time',
    excep longblob comment 'exception',
    end_params longtext comment 'end parameters',
    status varchar(2) comment 'status(SU succeed|FA failed|UN unknown|SK skipped|RU running)',
    compensation_status varchar(2) comment 'compensation status(SU succeed|FA failed|UN unknown|SK skipped|RU running)',
    is_running tinyint(1) comment 'is running(0 no|1 yes)',
    gmt_updated datetime(3) not null,
    primary key(id),
    unique key unikey_buz_tenant (business_key, tenant_id)
) comment 'state machine instance';

create table if not exists seata_state_inst
(
    id varchar(32) not null comment 'id',
    machine_inst_id varchar(46) not null  comment 'state machine instance id',
    name varchar(255) not null comment 'state name',
    type varchar(20) comment 'state type',
    service_name varchar(255) comment 'service name',
    service_method varchar(255) comment 'method name',
    service_type varchar(16) comment 'service type',
    business_key varchar(48) comment 'business key',
    state_id_compensated_for varchar(32) comment 'state compensated for',
    state_id_retried_for varchar(32) comment 'state retried for',
    gmt_started datetime(3) not null comment 'start time',
    is_for_update tinyint(1) comment 'is service for update',
    input_params longtext comment 'input parameters',
    output_params longtext comment 'output parameters',
    status varchar(2) not null comment 'status(SU succeed|FA failed|UN unknown|SK skipped|RU running)',
    excep longblob comment 'exception',
    gmt_end datetime(3) comment 'end time',
    primary key(id, machine_inst_id)
) comment 'state instance';