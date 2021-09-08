CREATE TABLE tcc_fence_log
(
    xid              VARCHAR2(128)  NOT NULL,
    branch_id        NUMBER(19)     NOT NULL,
    action_name      VARCHAR2(64)   NOT NULL,
    status           NUMBER(3)      NOT NULL,
    gmt_create       TIMESTAMP(3)   NOT NULL,
    gmt_modified     TIMESTAMP(3)   NOT NULL,
    PRIMARY KEY (xid, branch_id)
);
CREATE INDEX idx_gmt_modified ON tcc_fence_log (gmt_modified);
CREATE INDEX idx_status ON tcc_fence_log (status);