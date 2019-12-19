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
import java.util.List;
import java.util.Properties;
import com.alibaba.druid.mock.MockStatementBase;
import com.alibaba.druid.mock.handler.MockExecuteHandler;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock driver
 * @author will
 */
public class MockDriver extends com.alibaba.druid.mock.MockDriver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockDriver.class);

    /**
     * the mock column labels of return value
     */
    private List<String> mockReturnValueColumnLabels;

    /**
     * the mock value of return value
     */
    private Object[][] mockReturnValue;

    /**
     * the mock value of columns meta return value
     */
    private Object[][] mockColumnsMetasReturnValue;

    /**
     *  the mock value of index meta return value
     */
    private Object[][] mockIndexMetasReturnValue;

    /**
     * the mock execute handler
     */
    private MockExecuteHandler mockExecuteHandler;

    public MockDriver(Object[][] mockColumnsMetasReturnValue, Object[][] mockIndexMetasReturnValue) {
        this(Lists.newArrayList(), new Object[][]{}, mockColumnsMetasReturnValue, mockIndexMetasReturnValue);
    }

    /**
     * Instantiate a new MockDriver
     */
    public MockDriver(List<String> mockReturnValueColumnLabels, Object[][] mockReturnValue, Object[][] mockColumnsMetasReturnValue, Object[][] mockIndexMetasReturnValue) {
        this.mockReturnValueColumnLabels = mockReturnValueColumnLabels;
        this.mockReturnValue = mockReturnValue;
        this.mockColumnsMetasReturnValue = mockColumnsMetasReturnValue;
        this.mockIndexMetasReturnValue = mockIndexMetasReturnValue;
        this.setMockExecuteHandler(new MockExecuteHandlerImpl(mockReturnValueColumnLabels, mockReturnValue, mockColumnsMetasReturnValue));
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

    public MockPreparedStatement createSeataMockPreparedStatement(MockConnection conn, String sql) {
        return new MockPreparedStatement(conn, sql);
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
        this.mockReturnValue = mockReturnValue == null ? new Object[][]{} : mockReturnValue;
    }

    /**
     * mock the return value of columns meta
     * @param mockColumnsMetasReturnValue
     */
    public void setMockColumnsMetasReturnValue(Object[][] mockColumnsMetasReturnValue) {
        this.mockColumnsMetasReturnValue = mockColumnsMetasReturnValue == null ? new Object[][]{} : mockColumnsMetasReturnValue;
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
        this.mockIndexMetasReturnValue = mockIndexMetasReturnValue == null ? new Object[][]{} : mockIndexMetasReturnValue;
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
