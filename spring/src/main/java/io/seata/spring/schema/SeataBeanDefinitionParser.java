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
package io.seata.spring.schema;

import java.util.HashSet;
import java.util.Set;

import io.seata.common.DefaultValues;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.spring.annotation.GlobalTransactionScanner;
import io.seata.tm.api.DefaultFailureHandlerImpl;
import io.seata.tm.api.transaction.Propagation;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static io.seata.spring.annotation.GlobalTransactionScanner.DEFAULT_MODE;

/**
 * The type seata bean definition parser
 *
 * @author xingfudeshi@gmail.com
 */
public class SeataBeanDefinitionParser implements BeanDefinitionParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeataBeanDefinitionParser.class);


    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        return doParse(element, parserContext, GlobalTransactionScanner.class);
    }

    /**
     * do parse
     *
     * @param element
     * @param parserContext
     * @param beanClass
     * @return org.springframework.beans.factory.config.BeanDefinition
     * @author xingfudeshi@gmail.com
     */
    private static BeanDefinition doParse(Element element, ParserContext parserContext, Class<?> beanClass) {
        registerSeataTargetScanner(parserContext, element);
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(beanClass);
        beanDefinition.setLazyInit(false);
        beanDefinition.setConstructorArgumentValues(makeConstructorArgumentValues(element));
        parserContext.getRegistry().registerBeanDefinition(beanClass.getName(), beanDefinition);
        return beanDefinition;
    }

    /**
     * registwr seata target scanner
     *
     * @param parserContext
     * @param element
     * @return void
     * @author xingfudeshi@gmail.com
     */
    private static void registerSeataTargetScanner(ParserContext parserContext, Element element) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(SeataTargetScanner.class);
        beanDefinition.setLazyInit(false);
        ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
        constructorArgumentValues.addIndexedArgumentValue(0, getSeataConfig(element));
        beanDefinition.setConstructorArgumentValues(constructorArgumentValues);
        parserContext.getRegistry().registerBeanDefinition(SeataTargetScanner.class.getName(), beanDefinition);
    }

    /**
     * get seata config
     *
     * @param element
     * @return java.util.List<io.seata.spring.schema.GlobalTransactionalConfig>
     * @author xingfudeshi@gmail.com
     */
    private static Set<GlobalTransactionalConfig> getSeataConfig(Element element) {
        NodeList nodeList = element.getChildNodes();
        int length = nodeList.getLength();
        Set<GlobalTransactionalConfig> globalTransactionalConfigs = new HashSet<>();
        for (int i = 0; i < length; i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                Element ele = (Element) node;
                GlobalTransactionalConfig globalTransactionalConfig = new GlobalTransactionalConfig();
                globalTransactionalConfig.setScanPackage(ele.getAttribute("scanPackage"));
                globalTransactionalConfig.setPattern(ele.getAttribute("pattern"));
                String timeoutMills = ele.getAttribute("timeoutMills");
                if (StringUtils.isNotBlank(timeoutMills)) {
                    globalTransactionalConfig.setTimeoutMills(Integer.parseInt(timeoutMills));
                } else {
                    globalTransactionalConfig.setTimeoutMills(DefaultValues.DEFAULT_GLOBAL_TRANSACTION_TIMEOUT);
                }
                globalTransactionalConfig.setName(ele.getAttribute("name"));
                String rollbackFor = ele.getAttribute("rollbackFor");
                if (StringUtils.isNotBlank(rollbackFor)) {
                    globalTransactionalConfig.setRollbackFor(convertClass(rollbackFor));
                }
                String rollbackForClassName = ele.getAttribute("rollbackForClassName");
                if (StringUtils.isNotBlank(rollbackForClassName)) {
                    globalTransactionalConfig.setRollbackForClassName(rollbackForClassName.split(","));
                }
                String noRollbackFor = ele.getAttribute("noRollbackFor");
                if (StringUtils.isNotBlank(noRollbackFor)) {
                    globalTransactionalConfig.setNoRollbackFor(convertClass(noRollbackFor));
                }
                String noRollbackForClassName = ele.getAttribute("noRollbackForClassName");
                if (StringUtils.isNotBlank(noRollbackForClassName)) {
                    globalTransactionalConfig.setNoRollbackForClassName(noRollbackForClassName.split(","));
                }
                String propagation = ele.getAttribute("propagation");
                if (StringUtils.isNotBlank(propagation)) {
                    globalTransactionalConfig.setPropagation(Propagation.valueOf(propagation));
                } else {
                    globalTransactionalConfig.setPropagation(Propagation.REQUIRED);
                }
                String lockRetryInternal = ele.getAttribute("lockRetryInternal");
                if (StringUtils.isNotBlank(lockRetryInternal)) {
                    globalTransactionalConfig.setLockRetryInternal(Integer.parseInt(lockRetryInternal));
                } else {
                    globalTransactionalConfig.setLockRetryInternal(0);
                }
                String lockRetryTimes = ele.getAttribute("lockRetryTimes");
                if (StringUtils.isNotBlank(lockRetryTimes)) {
                    globalTransactionalConfig.setLockRetryTimes(Integer.parseInt(lockRetryTimes));
                } else {
                    globalTransactionalConfig.setLockRetryTimes(-1);
                }
                globalTransactionalConfigs.add(globalTransactionalConfig);
            }
        }
        return globalTransactionalConfigs;
    }

    /**
     * convert class
     *
     * @param str
     * @return java.lang.Class<? extends java.lang.Throwable>[]
     * @author xingfudeshi@gmail.com
     */
    private static Class<? extends Throwable>[] convertClass(String str) {
        String[] arr = str.split(",");
        Class<? extends Throwable>[] classes = new Class[arr.length];
        for (int j = 0; j < classes.length; j++) {
            try {
                classes[j] = (Class<? extends Throwable>) Class.forName(arr[j]);
            } catch (ClassNotFoundException e) {
                throw new ShouldNeverHappenException(e);
            }
        }
        return classes;
    }

    /**
     * make ConstructorArgumentValues
     *
     * @param element
     * @return org.springframework.beans.factory.config.ConstructorArgumentValues
     * @author xingfudeshi@gmail.com
     */
    private static ConstructorArgumentValues makeConstructorArgumentValues(Element element) {
        ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
        constructorArgumentValues.addIndexedArgumentValue(0, element.getAttribute("applicationId"));
        constructorArgumentValues.addIndexedArgumentValue(1, element.getAttribute("txServiceGroup"));
        String mode = element.getAttribute("mode");
        if (StringUtils.isNotBlank(mode)) {
            constructorArgumentValues.addIndexedArgumentValue(2, mode);
        } else {
            constructorArgumentValues.addIndexedArgumentValue(2, DEFAULT_MODE);
        }
        String failureHandler = element.getAttribute("failureHandler");
        if (StringUtils.isNotBlank(failureHandler)) {
            constructorArgumentValues.addIndexedArgumentValue(3, new RuntimeBeanReference(failureHandler));
        } else {
            constructorArgumentValues.addIndexedArgumentValue(3, new DefaultFailureHandlerImpl());
        }

        return constructorArgumentValues;
    }

}
