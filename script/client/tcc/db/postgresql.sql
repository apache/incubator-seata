-- -------------------------------- The script used for tcc fence  --------------------------------
CREATE TABLE IF NOT EXISTS public.tcc_fence_log
(
    xid              VARCHAR(128)  NOT NULL,
    branch_id        BIGINT        NOT NULL,
    action_name      VARCHAR(64)   NOT NULL,
    status           SMALLINT      NOT NULL,
    gmt_create       TIMESTAMP(3)  NOT NULL,
    gmt_modified     TIMESTAMP(3)  NOT NULL,
    CONSTRAINT pk_tcc_fence_log PRIMARY KEY (xid, branch_id)
);
CREATE INDEX idx_gmt_modified ON public.tcc_fence_log (gmt_modified);
CREATE INDEX idx_status ON public.tcc_fence_log (status);