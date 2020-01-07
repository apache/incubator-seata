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
 * The interface Order service.
 */
public interface OrderService extends DataResetService {
    /**
     * Sets account service.
     *
     * @param accountService the account service
     */
    void setAccountService(AccountService accountService);

    /**
     * Create.
     *
     * @param userId        the user id
     * @param commodityCode the commodity code
     * @param count         the count
     * @throws SQLException the sql exception
     */
    void create(String userId, String commodityCode, Integer count) throws SQLException;
}
