-- -------------------------------- The script used for sage  --------------------------------
CREATE TABLE seata_state_machine_def
(
    id               VARCHAR2(32)  NOT NULL,
    name             VARCHAR2(255) NOT NULL,
    tenant_id        VARCHAR2(32)  NOT NULL,
    app_name         VARCHAR2(32)  NOT NULL,
    type             VARCHAR2(20),
    comment_         VARCHAR2(255),
    ver              VARCHAR2(16)  NOT NULL,
    gmt_create       TIMESTAMP(0)  NOT NULL,
    status           VARCHAR2(2)   NOT NULL,
    content          CLOB,
    recover_strategy VARCHAR2(16),
    PRIMARY KEY (id)
);

CREATE TABLE seata_state_machine_inst
(
    id                  VARCHAR2(46)                      NOT NULL,
    machine_id          VARCHAR2(32)                      NOT NULL,
    tenant_id           VARCHAR2(32)                      NOT NULL,
    parent_id           VARCHAR2(46),
    gmt_started         TIMESTAMP(0)                      NOT NULL,
    business_key        VARCHAR2(48),
    start_params        CLOB,
    gmt_end             TIMESTAMP(0) DEFAULT systimestamp,
    excep               BLOB,
    end_params          CLOB,
    status              VARCHAR2(2),
    compensation_status VARCHAR2(2),
    is_running          NUMBER(3),
    gmt_updated         TIMESTAMP(0) DEFAULT systimestamp NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT unikey_buz_tenant UNIQUE (business_key, tenant_id)
);

CREATE TABLE seata_state_inst
(
    id                       VARCHAR2(32)  NOT NULL,
    machine_inst_id          VARCHAR2(46)  NOT NULL,
    name                     VARCHAR2(255) NOT NULL,
    type                     VARCHAR2(20),
    service_name             VARCHAR2(255),
    service_method           VARCHAR2(255),
    service_type             VARCHAR2(16),
    business_key             VARCHAR2(48),
    state_id_compensated_for VARCHAR2(32),
    state_id_retried_for     VARCHAR2(32),
    gmt_started              TIMESTAMP(0)  NOT NULL,
    is_for_update            NUMBER(3),
    input_params             CLOB,
    output_params            CLOB,
    status                   VARCHAR2(2)   NOT NULL,
    excep                    BLOB,
    gmt_end                  TIMESTAMP(0) DEFAULT systimestamp,
    PRIMARY KEY (id, machine_inst_id)
);
