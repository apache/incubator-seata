/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.core.constants;

/**
 * client table columns name.
 *
 * @author zjinlei
 */
public interface ClientTableColumnsName {

    /**
     * The constant undo_log column name xid
     * this field is not use in mysql
     */
    String UNDO_LOG_ID = "id";

    /**
     * The constant undo_log column name xid
     */
    String UNDO_LOG_XID = "xid";

    /**
     * The constant undo_log column name branch_id
     */
    String UNDO_LOG_BRANCH_XID = "branch_id";

    /**
     * The constant undo_log column name context
     */
    String UNDO_LOG_CONTEXT = "context";

    /**
     * The constant undo_log column name rollback_info
     */
    String UNDO_LOG_ROLLBACK_INFO = "rollback_info";

    /**
     * The constant undo_log column name log_status
     */
    String UNDO_LOG_LOG_STATUS = "log_status";

    /**
     * The constant undo_log column name log_created
     */
    String UNDO_LOG_LOG_CREATED = "log_created";

    /**
     * The constant undo_log column name log_modified
     */
    String UNDO_LOG_LOG_MODIFIED = "log_modified";
}
