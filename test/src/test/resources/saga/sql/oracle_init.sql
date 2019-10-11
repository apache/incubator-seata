create table seata_state_machine_def
(
    id varchar(32) not null,
    name varchar(255) not null,
    tenant_id varchar(32) not null,
    app_name varchar(32) not null,
    type varchar(20),
    comment_ varchar(255),
    ver varchar(16) not null,
    gmt_create timestamp not null,
    status varchar(2) not null,
    content clob,
    recover_strategy varchar(16),
    primary key(id)
);

create table seata_state_machine_inst
(
    id varchar(32) not null,
    machine_id varchar(32) not null,
    tenant_id varchar(32) not null,
    parent_id varchar(46),
    gmt_started timestamp not null,
    business_key varchar(48),
    uni_business_key varchar(48) generated always as(
        CASE
            WHEN "BUSINESS_KEY" IS NULL
            THEN "ID"
            ELSE "BUSINESS_KEY"
        END),
    start_params clob,
    gmt_end timestamp,
    excep blob,
    end_params clob,
    status varchar(2),
    compensation_status varchar(2),
    is_running smallint,
    gmt_updated timestamp not null,
    primary key(id)
);
create unique index state_machine_inst_unibuzkey on seata_state_machine_inst(uni_business_key, tenant_id);

create table seata_state_inst
(
    id varchar(32) not null,
    machine_inst_id varchar(32) not null,
    name varchar(255) not null,
    type varchar(20),
    service_name varchar(255),
    service_method varchar(255),
    service_type varchar(16),
    business_key varchar(48),
    state_id_compensated_for varchar(32),
    state_id_retried_for varchar(32),
    gmt_started timestamp not null,
    is_for_update smallint,
    input_params clob,
    output_params clob,
    status varchar(2) not null,
    excep blob,
    gmt_end timestamp,
    primary key(id, machine_inst_id)
);