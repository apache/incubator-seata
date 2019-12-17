CREATE TABLE seata_state_machine_def
(
    id               VARCHAR(32)  NOT NULL,
    name             VARCHAR(255) NOT NULL,
    tenant_id        VARCHAR(32)  NOT NULL,
    app_name         VARCHAR(32)  NOT NULL,
    type             VARCHAR(20),
    comment_         VARCHAR(255),
    ver              VARCHAR(16)  NOT NULL,
    gmt_create       TIMESTAMP    NOT NULL,
    status           VARCHAR(2)   NOT NULL,
    content          CLOB,
    recover_strategy VARCHAR(16),
    PRIMARY KEY (id)
);

CREATE TABLE seata_state_machine_inst
(
    id                  VARCHAR(46) NOT NULL,
    machine_id          VARCHAR(32) NOT NULL,
    tenant_id           VARCHAR(32) NOT NULL,
    parent_id           VARCHAR(46),
    gmt_started         TIMESTAMP   NOT NULL,
    business_key        VARCHAR(48),
    uni_business_key    VARCHAR(48) GENERATED ALWAYS AS (
                            CASE
                                WHEN "BUSINESS_KEY" IS NULL
                                    THEN "ID"
                                ELSE "BUSINESS_KEY"
                                END),
    start_params        CLOB,
    gmt_end             TIMESTAMP,
    excep               BLOB,
    end_params          CLOB,
    status              VARCHAR(2),
    compensation_status VARCHAR(2),
    is_running          SMALLINT,
    gmt_updated         TIMESTAMP   NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX state_machine_inst_unibuzkey ON seata_state_machine_inst (uni_business_key, tenant_id);

CREATE TABLE seata_state_inst
(
    id                       VARCHAR(32)  NOT NULL,
    machine_inst_id          VARCHAR(46)  NOT NULL,
    name                     VARCHAR(255) NOT NULL,
    type                     VARCHAR(20),
    service_name             VARCHAR(255),
    service_method           VARCHAR(255),
    service_type             VARCHAR(16),
    business_key             VARCHAR(48),
    state_id_compensated_for VARCHAR(32),
    state_id_retried_for     VARCHAR(32),
    gmt_started              TIMESTAMP    NOT NULL,
    is_for_update            SMALLINT,
    input_params             CLOB,
    output_params            CLOB,
    status                   VARCHAR(2)   NOT NULL,
    excep                    BLOB,
    gmt_end                  TIMESTAMP,
    PRIMARY KEY (id, machine_inst_id)
);
