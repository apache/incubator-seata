/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.integration.tx.api.interceptor;

import org.apache.seata.integration.tx.api.remoting.RemotingDesc;
import org.apache.seata.integration.tx.api.remoting.RemotingParser;
import org.apache.seata.integration.tx.api.remoting.parser.DefaultRemotingParser;

/**
 * parser transaction bean
 *
 */
public class TxBeanParserUtils {

    private TxBeanParserUtils() {
    }

    /**
     * is auto proxy transaction bean
     *
     * @param bean               the bean
     * @param beanName           the bean name
     * @return boolean boolean
     */
    public static boolean isTxRemotingBean(Object bean, String beanName) {
        return parserRemotingServiceInfo(bean, beanName);
    }


    /**
     * get remoting bean info: sofa:service, sofa:reference, dubbo:reference, dubbo:service
     *
     * @param bean     the bean
     * @param beanName the bean name
     * @return if sofa:service, sofa:reference, dubbo:reference, dubbo:service return true, else return false
     */
    public static boolean parserRemotingServiceInfo(Object bean, String beanName) {
        RemotingParser remotingParser = DefaultRemotingParser.get().isRemoting(bean, beanName);
        if (remotingParser != null) {
            return DefaultRemotingParser.get().parserRemotingServiceInfo(bean, beanName, remotingParser) != null;
        }
        return false;
    }

    /**
     * get the remoting description of Tx bean
     *
     * @param beanName the bean name
     * @return remoting desc
     */
    public static RemotingDesc getRemotingDesc(String beanName) {
        return DefaultRemotingParser.get().getRemotingBeanDesc(beanName);
    }

}
