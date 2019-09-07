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
package io.seata.rm.datasource.mock;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import com.alibaba.druid.mock.MockStatementBase;
import com.alibaba.druid.mock.handler.MockExecuteHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock driver
 * @author will
 * @date 2019/8/14
 */
public class MockDriver extends com.alibaba.druid.mock.MockDriver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockDriver.class);

    /**
     * the mock value of return value
     */
    private Object[][] mockReturnValue = null;

    /**
     * the mock value of columns meta return value
     */
    private Object[][] mockColumnsMetasReturnValue = null;

    /**
     *  the mock value of index meta return value
     */
    private Object[][] mockIndexMetasReturnValue = null;

    /**
     * the mock execute handler
     */
    private MockExecuteHandler mockExecuteHandler = null;

    public MockDriver(Object[][] mockColumnsMetasReturnValue, Object[][] mockIndexMetasReturnValue) {
        this(new Object[][]{}, mockColumnsMetasReturnValue, mockIndexMetasReturnValue);
    }

    /**
     * Instantiate a new MockDriver
     */
    public MockDriver(Object[][] mockReturnValue, Object[][] mockColumnsMetasReturnValue, Object[][] mockIndexMetasReturnValue) {
        this.mockReturnValue = mockReturnValue;
        this.mockColumnsMetasReturnValue = mockColumnsMetasReturnValue;
        this.mockIndexMetasReturnValue = mockIndexMetasReturnValue;
        this.setMockExecuteHandler(new MockExecuteHandlerImpl(mockReturnValue));
    }

    /**
     * Instantiate a new MockConnection
     * @param driver
     * @param url
     * @param connectProperties
     * @return
     */
    @Override
    public MockConnection createMockConnection(com.alibaba.druid.mock.MockDriver driver, String url,
                                               Properties connectProperties) {
        return new MockConnection(this, url, connectProperties);
    }

    @Override
    public ResultSet executeQuery(MockStatementBase stmt, String sql) throws SQLException {
        return this.mockExecuteHandler.executeQuery(stmt, sql);
    }

    /**
     * mock the return value
     * @return
     */
    public Object[][] getMockReturnValue() {
        return mockReturnValue;
    }

    /**
     *  get the return value
     * @param mockReturnValue
     */
    public void setMockReturnValue(Object[][] mockReturnValue) {
        this.mockReturnValue = mockReturnValue;
    }

    /**
     * mock the return value of columns meta
     * @param mockColumnsMetasReturnValue
     */
    public void setMockColumnsMetasReturnValue(Object[][] mockColumnsMetasReturnValue) {
        this.mockColumnsMetasReturnValue = mockColumnsMetasReturnValue;
    }

    /**
     * get the return value of columns meta
     * @return
     */
    public Object[][] getMockColumnsMetasReturnValue() {
        return mockColumnsMetasReturnValue;
    }

    /**
     * mock the return value of index meta
     * @param mockIndexMetasReturnValue
     */
    public void setMockIndexMetasReturnValue(Object[][] mockIndexMetasReturnValue) {
        this.mockIndexMetasReturnValue = mockIndexMetasReturnValue;
    }

    /**
     * get the return value of index meta
     * @return
     */
    public Object[][] getMockIndexMetasReturnValue() {
        return mockIndexMetasReturnValue;
    }

    /**
     * set the mock execute handler
     * @param mockExecuteHandler
     */
    public void setMockExecuteHandler(MockExecuteHandler mockExecuteHandler){
        this.mockExecuteHandler = mockExecuteHandler;
    }
}
