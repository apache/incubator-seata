create table seata_state_machine_def
(
    id varchar(32) not null,
    name varchar(128) not null,
    tenant_id varchar(32) not null,
    app_name varchar(32) not null,
    type varchar(20),
    comment_ varchar(255),
    ver varchar(16) not null,
    gmt_create timestamp(3) not null,
    status varchar(2) not null,
    content clob(65536) inline length 2048,
    recover_strategy varchar(16),
    primary key(id)
);

create table seata_state_machine_inst
(
    id varchar(128) not null,
    machine_id varchar(32) not null,
    tenant_id varchar(32) not null,
    parent_id varchar(128),
    gmt_started timestamp(3) not null,
    business_key varchar(48),
    uni_business_key varchar(128) not null generated always as( --Unique index does not allow empty columns on DB2
        CASE
            WHEN "BUSINESS_KEY" IS NULL
            THEN "ID"
            ELSE "BUSINESS_KEY"
        END),
    start_params clob(65536) inline length 1024,
    gmt_end timestamp(3),
    excep blob(10240),
    end_params clob(65536) inline length 1024,
    status varchar(2),
    compensation_status varchar(2),
    is_running smallint,
    gmt_updated timestamp(3) not null,
    primary key(id)
);
create unique index state_machine_inst_unibuzkey on seata_state_machine_inst(uni_business_key, tenant_id);

create table seata_state_inst
(
    id varchar(48) not null,
    machine_inst_id varchar(128) not null,
    name varchar(128) not null,
    type varchar(20),
    service_name varchar(128),
    service_method varchar(128),
    service_type varchar(16),
    business_key varchar(48),
    state_id_compensated_for varchar(50),
    state_id_retried_for varchar(50),
    gmt_started timestamp(3) not null,
    is_for_update smallint,
    input_params clob(65536) inline length 1024,
    output_params clob(65536) inline length 1024,
    status varchar(2) not null,
    excep blob(10240),
    gmt_updated timestamp(3),
    gmt_end timestamp(3),
    primary key(id, machine_inst_id)
);