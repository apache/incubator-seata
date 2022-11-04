/*
 * Copyright 1999-2019 Seata.io Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.seata.rm.datasource.undo;

import java.sql.SQLException;

/**
 *
 *
 * @author zouwei
 */
class SQLUndoDirtyException extends SQLException {

    private static final long serialVersionUID = -5168905669539637570L;

    SQLUndoDirtyException() {
        super();
    }

    SQLUndoDirtyException(String reason) {
        super(reason);
    }

    SQLUndoDirtyException(String reason, String SQLState) {
        super(reason, SQLState);
    }

    SQLUndoDirtyException(String reason, String SQLState, int vendorCode) {
        super(reason, SQLState, vendorCode);
    }

    SQLUndoDirtyException(Throwable cause) {
        super(cause);
    }

    SQLUndoDirtyException(String reason, Throwable cause) {
        super(reason, cause);
    }

    SQLUndoDirtyException(String reason, String SQLState, Throwable cause) {
        super(reason, SQLState, cause);
    }

    SQLUndoDirtyException(String reason, String SQLState, int vendorCode, Throwable cause) {
        super(reason, SQLState, vendorCode, cause);
    }
}
