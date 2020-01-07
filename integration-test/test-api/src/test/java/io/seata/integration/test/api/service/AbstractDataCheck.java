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
package io.seata.integration.test.api.service;

import java.sql.SQLException;

/**
 * The type Abstract data check.
 */
public abstract class AbstractDataCheck {

    /**
     * Valid negative check boolean.
     *
     * @param field the field
     * @param id    the id
     * @return the boolean
     */
    public boolean validNegativeCheck(String field, String id) throws SQLException {
        return doNegativeCheck(field, id) >= 0;
    }

    /**
     * Do negative check int.
     *
     * @param field the field
     * @param id    the id
     * @return the int
     * @throws SQLException the sql exception
     */
    public abstract int doNegativeCheck(String field, String id) throws SQLException;
}
