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
        registerGtxTargetScanner(parserContext, element);
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(beanClass);
        beanDefinition.setLazyInit(false);
        beanDefinition.setConstructorArgumentValues(makeConstructorArgumentValues(element));
        parserContext.getRegistry().registerBeanDefinition(beanClass.getName(), beanDefinition);
        return beanDefinition;
    }

    /**
     * registwr gtx target scanner
     *
     * @param parserContext
     * @param element
     * @return void
     * @author xingfudeshi@gmail.com
     */
    private static void registerGtxTargetScanner(ParserContext parserContext, Element element) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(GtxTargetScanner.class);
        beanDefinition.setLazyInit(false);
        ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
        constructorArgumentValues.addIndexedArgumentValue(0, getGtxConfig(element));
        beanDefinition.setConstructorArgumentValues(constructorArgumentValues);
        parserContext.getRegistry().registerBeanDefinition(GtxTargetScanner.class.getName(), beanDefinition);
    }

    /**
     * get gtx config
     *
     * @param element
     * @return java.util.List<io.seata.spring.schema.GtxConfig>
     * @author xingfudeshi@gmail.com
     */
    private static Set<GtxConfig> getGtxConfig(Element element) {
        NodeList nodeList = element.getChildNodes();
        int length = nodeList.getLength();
        Set<GtxConfig> gtxConfigs = new HashSet<>();
        for (int i = 0; i < length; i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                Element ele = (Element) node;
                GtxConfig gtxConfig = new GtxConfig();
                gtxConfig.setScanPackage(ele.getAttribute("scanPackage"));
                gtxConfig.setPattern(ele.getAttribute("pattern"));
                String timeoutMills = ele.getAttribute("timeoutMills");
                if (StringUtils.isNotBlank(timeoutMills)) {
                    gtxConfig.setTimeoutMills(Integer.parseInt(timeoutMills));
                } else {
                    gtxConfig.setTimeoutMills(DefaultValues.DEFAULT_GLOBAL_TRANSACTION_TIMEOUT);
                }
                gtxConfig.setName(ele.getAttribute("name"));
                String rollbackFor = ele.getAttribute("rollbackFor");
                if (StringUtils.isNotBlank(rollbackFor)) {
                    gtxConfig.setRollbackFor(convertClass(rollbackFor));
                }
                String rollbackForClassName = ele.getAttribute("rollbackForClassName");
                if (StringUtils.isNotBlank(rollbackForClassName)) {
                    gtxConfig.setRollbackForClassName(rollbackForClassName.split(","));
                }
                String noRollbackFor = ele.getAttribute("noRollbackFor");
                if (StringUtils.isNotBlank(noRollbackFor)) {
                    gtxConfig.setNoRollbackFor(convertClass(noRollbackFor));
                }
                String noRollbackForClassName = ele.getAttribute("noRollbackForClassName");
                if (StringUtils.isNotBlank(noRollbackForClassName)) {
                    gtxConfig.setNoRollbackForClassName(noRollbackForClassName.split(","));
                }
                String propagation = ele.getAttribute("propagation");
                if (StringUtils.isNotBlank(propagation)) {
                    gtxConfig.setPropagation(Propagation.valueOf(propagation));
                } else {
                    gtxConfig.setPropagation(Propagation.REQUIRED);
                }
                String lockRetryInternal = ele.getAttribute("lockRetryInternal");
                if (StringUtils.isNotBlank(lockRetryInternal)) {
                    gtxConfig.setLockRetryInternal(Integer.parseInt(lockRetryInternal));
                } else {
                    gtxConfig.setLockRetryInternal(0);
                }
                String lockRetryTimes = ele.getAttribute("lockRetryTimes");
                if (StringUtils.isNotBlank(lockRetryTimes)) {
                    gtxConfig.setLockRetryTimes(Integer.parseInt(lockRetryTimes));
                } else {
                    gtxConfig.setLockRetryTimes(-1);
                }
                gtxConfigs.add(gtxConfig);
            }
        }
        return gtxConfigs;
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
