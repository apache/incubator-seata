-- -------------------------------- The script used for sage  --------------------------------
CREATE TABLE IF NOT EXISTS public.seata_state_machine_def
(
    id               VARCHAR(32)  NOT NULL,
    name             VARCHAR(128) NOT NULL,
    tenant_id        VARCHAR(32)  NOT NULL,
    app_name         VARCHAR(32)  NOT NULL,
    type             VARCHAR(20),
    comment_         VARCHAR(255),
    ver              VARCHAR(16)  NOT NULL,
    gmt_create       TIMESTAMP(3) NOT NULL,
    status           VARCHAR(2)   NOT NULL,
    content          TEXT,
    recover_strategy VARCHAR(16),
    CONSTRAINT pk_seata_state_machine_def PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.seata_state_machine_inst
(
    id                  VARCHAR(128)                NOT NULL,
    machine_id          VARCHAR(32)                NOT NULL,
    tenant_id           VARCHAR(32)                NOT NULL,
    parent_id           VARCHAR(128),
    gmt_started         TIMESTAMP(3)               NOT NULL,
    business_key        VARCHAR(48),
    start_params        TEXT,
    gmt_end             TIMESTAMP(3) DEFAULT now(),
    excep               BYTEA,
    end_params          TEXT,
    status              VARCHAR(2),
    compensation_status VARCHAR(2),
    is_running          BOOLEAN,
    gmt_updated         TIMESTAMP(3) DEFAULT now() NOT NULL,
    CONSTRAINT pk_seata_state_machine_inst PRIMARY KEY (id),
    CONSTRAINT unikey_buz_tenant UNIQUE (business_key, tenant_id)
)
;
CREATE TABLE IF NOT EXISTS public.seata_state_inst
(
    id                       VARCHAR(48)  NOT NULL,
    machine_inst_id          VARCHAR(128)  NOT NULL,
    name                     VARCHAR(128) NOT NULL,
    type                     VARCHAR(20),
    service_name             VARCHAR(128),
    service_method           VARCHAR(128),
    service_type             VARCHAR(16),
    business_key             VARCHAR(48),
    state_id_compensated_for VARCHAR(50),
    state_id_retried_for     VARCHAR(50),
    gmt_started              TIMESTAMP(3) NOT NULL,
    is_for_update            BOOLEAN,
    input_params             TEXT,
    output_params            TEXT,
    status                   VARCHAR(2)   NOT NULL,
    excep                    BYTEA,
    gmt_updated              TIMESTAMP(3) DEFAULT now(),
    gmt_end                  TIMESTAMP(3) DEFAULT now(),
    CONSTRAINT pk_seata_state_inst PRIMARY KEY (id, machine_inst_id)
);
