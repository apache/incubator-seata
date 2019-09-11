-- the table to store GlobalSession data
create table global_table (
    xid varchar2(128)  not null,
    transaction_id number(20),
    status number(3) not null,
    application_id varchar2(32),
    transaction_service_group varchar2(32),
    transaction_name varchar2(64),
    timeout int,
    begin_time number(20),
    application_data varchar2(2000),
    gmt_create TIMESTAMP,
    gmt_modified TIMESTAMP,
    primary key (xid)
    )
    /
create index idx_gmt_modified_status
    on global_table (gmt_modified, status)
/
create index idx_transaction_id
    on global_table (transaction_id)
/

-- the table to store BranchSession data
create table branch_table (
        branch_id number(20) not null,
        xid varchar2(128) not null,
    transaction_id number(20) ,
    resource_group_id varchar2(32),
    resource_id varchar2(256) ,
    lock_key varchar2(128) ,
    branch_type varchar2(8) ,
    status number(3),
    client_id varchar2(64),
    application_data varchar2(2000),
    gmt_create timestamp,
    gmt_modified timestamp,
    primary key (branch_id)
    )
    /
create index branch_table_idx_xid on branch_table  (xid)
/

-- the table to store lock data
create table lock_table (
    row_key varchar2(128) not null,
    xid varchar2(96),
    transaction_id number(20) ,
    branch_id number(20),
    resource_id varchar2(256) ,
    table_name varchar2(32) ,
    pk varchar2(32) ,
    gmt_create timestamp ,
    gmt_modified timestamp,
    primary key(row_key)
    )
/
