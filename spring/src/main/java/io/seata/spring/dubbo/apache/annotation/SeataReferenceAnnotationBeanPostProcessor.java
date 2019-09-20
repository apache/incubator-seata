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
package io.seata.spring.dubbo.apache.annotation;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.ReflectionUtil;
import io.seata.spring.api.SimpleInterceptor;
import io.seata.spring.dubbo.apache.util.AnnotationUtils;
import io.seata.spring.dubbo.apache.util.ServiceBeanNameBuilder;
import io.seata.spring.tcc.TccSimpleInterceptor;
import io.seata.spring.util.TCCBeanParserUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.springframework.core.BridgeMethodResolver.findBridgedMethod;
import static org.springframework.core.BridgeMethodResolver.isVisibilityBridgeMethodPair;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.core.annotation.AnnotationUtils.getAnnotation;

public class SeataReferenceAnnotationBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
    implements ApplicationContextAware, ApplicationListener, PriorityOrdered {
    
    public static final String                                                              BEAN_NAME                                = "seataReferenceAnnotationBeanPostProcessor";
    
    private static final Logger                                                             LOGGER                                   = LoggerFactory
        .getLogger(SeataReferenceAnnotationBeanPostProcessor.class);
    
    private final static int                                                                CACHE_SIZE                               = Integer
        .getInteger("", 32);
    
    private final ConcurrentMap<String, AnnotatedInjectionMetadata>                         injectionMetadataCache                   = new ConcurrentHashMap<>(
        CACHE_SIZE);
    
    private final ConcurrentMap<String, Object>                                             injectedObjectsCache                     = new ConcurrentHashMap<>(
        CACHE_SIZE);
    
    private final ConcurrentMap<String, SeataDubboReferenceBean>                            dubboReferenceBeanCache                  = new ConcurrentHashMap<>(
        CACHE_SIZE);
    
    private final ConcurrentHashMap<String, ReferenceBeanInvocationHandler>                 localReferenceBeanInvocationHandlerCache = new ConcurrentHashMap<>(
        CACHE_SIZE);
    
    private final ConcurrentMap<InjectionMetadata.InjectedElement, SeataDubboReferenceBean> injectedFieldReferenceBeanCache          = new ConcurrentHashMap<>(
        CACHE_SIZE);
    
    private final ConcurrentMap<InjectionMetadata.InjectedElement, SeataDubboReferenceBean> injectedMethodReferenceBeanCache         = new ConcurrentHashMap<>(
        CACHE_SIZE);
    
    public ClassLoader                                                                      classLoader                              = ClassUtils
        .getDefaultClassLoader();
    
    public ApplicationContext                                                               applicationContext;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        
    }
    
    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }
    
    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        return super.postProcessBeforeInstantiation(beanClass, beanName);
    }
    
    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
        return super.postProcessAfterInstantiation(bean, beanName);
    }
    
    @Override
    public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName)
        throws BeansException {
        return super.postProcessPropertyValues(pvs, pds, bean, beanName);
    }
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return super.postProcessBeforeInitialization(bean, beanName);
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        containSagaReference(bean, beanName);
        return super.postProcessAfterInitialization(bean, beanName);
    }
    
    /**
     * 功能描述:
     *
     * @param bean
     *
     * @param beanName
     *
     * @return boolean
     * @author ZHONGFUHUA-PC
     * @date 2019/9/17 14:26
     */
    public boolean containSagaReference(Object bean, String beanName) {
        AnnotatedInjectionMetadata metadata = findInjectionMetadata(beanName, bean.getClass(), null);
        if (CollectionUtils.isNotEmpty(metadata.getFieldElements()) || CollectionUtils.isNotEmpty(metadata.getMethodElements())) {
            try {
                metadata.inject(bean, beanName, null);
            } catch (BeanCreationException ex) {
                throw ex;
            } catch (Throwable ex) {
                throw new BeanCreationException(beanName, "Injection of @" + Reference.class.getName() + " dependencies is failed", ex);
            }
            return true;
        }
        return false;
    }
    
    public AnnotatedInjectionMetadata findInjectionMetadata(String beanName, Class<?> clazz, PropertyValues pvs) {
        String cacheKey = (StringUtils.hasLength(beanName) ? beanName
            : clazz.getName());
        AnnotatedInjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
        if (InjectionMetadata.needsRefresh(metadata, clazz)) {
            synchronized (this.injectionMetadataCache) {
                metadata = this.injectionMetadataCache.get(cacheKey);
                if (InjectionMetadata.needsRefresh(metadata, clazz)) {
                    if (metadata != null) {
                        metadata.clear(pvs);
                    }
                    try {
                        metadata = buildAnnotatedMetadata(clazz);
                        this.injectionMetadataCache.put(cacheKey, metadata);
                    } catch (NoClassDefFoundError err) {
                        throw new IllegalStateException("Failed to introspect object class [" + clazz.getName()
                            + "] for annotation metadata: could not find class that it depends on", err);
                    }
                }
            }
        }
        return metadata;
    }
    
    public List<AnnotatedFieldElement> findFieldAnnotationMetadata(final Class<?> beanClass) {
        final List<AnnotatedFieldElement> elements = new LinkedList<>();
        ReflectionUtils.doWithFields(beanClass, field -> {
            Reference annotation = getAnnotation(field, Reference.class);
            if (annotation != null) {
                if (Modifier.isStatic(field.getModifiers())) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("@" + Reference.class.getName() + " is not supported on static fields: " + field);
                    }
                    return;
                }
                elements.add(new AnnotatedFieldElement(field, annotation, this.applicationContext));
            }
            
        });
        
        return elements;
        
    }
    
    public List<AnnotatedMethodElement> findAnnotatedMethodMetadata(final Class<?> beanClass) {
        final List<AnnotatedMethodElement> elements = new LinkedList<>();
        ReflectionUtils.doWithMethods(beanClass, method -> {
            Method bridgedMethod = findBridgedMethod(method);
            if (!isVisibilityBridgeMethodPair(method, bridgedMethod)) {
                return;
            }
            Reference annotation = findAnnotation(bridgedMethod, Reference.class);
            if (annotation != null && method.equals(ClassUtils.getMostSpecificMethod(method, beanClass))) {
                if (Modifier.isStatic(method.getModifiers())) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("@" + Reference.class.getSimpleName() + " annotation is not supported on static methods: " + method);
                    }
                    return;
                }
                
                if (method.getParameterTypes().length == 0) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("@" + Reference.class.getSimpleName() + " annotation should only be used on methods with parameters: " + method);
                    }
                }
                PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, beanClass);
                elements.add(new AnnotatedMethodElement(method, pd, annotation, this.applicationContext));
            }
        });
        
        return elements;
        
    }
    
    public AnnotatedInjectionMetadata buildAnnotatedMetadata(final Class<?> beanClass) {
        Collection<AnnotatedFieldElement> fieldElements = findFieldAnnotationMetadata(beanClass);
        Collection<AnnotatedMethodElement> methodElements = findAnnotatedMethodMetadata(beanClass);
        return new AnnotatedInjectionMetadata(beanClass, fieldElements, methodElements);
        
    }
    
    public class AnnotatedInjectionMetadata extends InjectionMetadata {
        
        public final Collection<AnnotatedFieldElement>  fieldElements;
        
        public final Collection<AnnotatedMethodElement> methodElements;
        
        public AnnotatedInjectionMetadata(Class<?> targetClass, Collection<AnnotatedFieldElement> fieldElements,
            Collection<AnnotatedMethodElement> methodElements) {
            super(targetClass, combine(fieldElements, methodElements));
            this.fieldElements = fieldElements;
            this.methodElements = methodElements;
        }
        
        public Collection<AnnotatedFieldElement> getFieldElements() {
            return fieldElements;
        }
        
        public Collection<AnnotatedMethodElement> getMethodElements() {
            return methodElements;
        }
        
    }
    
    public static <T> Collection<T> combine(Collection<? extends T>... elements) {
        List<T> allElements = new ArrayList<>();
        for (Collection<? extends T> e : elements) {
            allElements.addAll(e);
        }
        return allElements;
    }
    
    public class AnnotatedMethodElement extends InjectionMetadata.InjectedElement {
        
        public final Method             method;
        
        public final Reference          annotation;
        
        public final ApplicationContext applicationContext;
        
        protected AnnotatedMethodElement(Method method, PropertyDescriptor pd, Reference annotation, ApplicationContext applicationContext) {
            super(method, pd);
            this.method = method;
            this.annotation = annotation;
            this.applicationContext = applicationContext;
        }
        
        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {
            //boolean isSagaProxy = SAGABeanParserUtils.isSagaDubboProxy(bean, this.getMember().getName());
            boolean isSagaProxy = false;
            boolean isTccProxy = TCCBeanParserUtils.isTccDubboProxy(bean, this.getMember().getName());
            if (isSagaProxy || isTccProxy) {
                Class<?> injectedType = pd.getPropertyType();
                Object injectedObject = getInjectedObject(annotation, bean, injectedType, this, isSagaProxy);
                ReflectionUtils.makeAccessible(method);
                method.invoke(bean, injectedObject);
            }
        }
        
    }
    
    public class AnnotatedFieldElement extends InjectionMetadata.InjectedElement {
        
        public final Field              field;
        
        public final Reference          annotation;
        
        public final ApplicationContext applicationContext;
        
        protected AnnotatedFieldElement(Field field, Reference annotation, ApplicationContext applicationContext) {
            super(field, null);
            this.field = field;
            this.annotation = annotation;
            this.applicationContext = applicationContext;
        }
        
        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {
            //boolean isSagaProxy = SAGABeanParserUtils.isSagaDubboProxy(bean, this.getMember().getName());
            boolean isSagaProxy = false;
            boolean isTccProxy = TCCBeanParserUtils.isTccDubboProxy(bean, this.getMember().getName());
            if (isSagaProxy || isTccProxy) {
                Class<?> injectedType = field.getType();
                Object injectedObject = getInjectedObject(annotation, bean, injectedType, this, isSagaProxy);
                ReflectionUtils.makeAccessible(field);
                field.set(bean, injectedObject);
            }
        }
    }
    
    public Object getInjectedObject(Reference annotation, Object bean, Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement,
        boolean isSagaProxy)
        throws Exception {
        
        String cacheKey = buildInjectedObjectCacheKey(annotation, injectedType, injectedElement);
        
        Object injectedObject = injectedObjectsCache.get(cacheKey);
        
        if (injectedObject == null) {
            injectedObject = doGetInjectedBean(annotation, bean, injectedType, injectedElement, isSagaProxy);
            injectedObjectsCache.putIfAbsent(cacheKey, injectedObject);
        }
        
        return injectedObject;
        
    }
    
    public Object doGetInjectedBean(Reference reference, Object bean, Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement,
        boolean isSagaProxy)
        throws Exception {
        
        String referencedBeanName = buildReferencedBeanName(reference, injectedType);
        
        SeataDubboReferenceBean seataDubboReferenceBean = buildReferenceBeanIfAbsent(referencedBeanName, bean, injectedElement, isSagaProxy);
        
        cacheInjectedReferenceBean(seataDubboReferenceBean, injectedElement);
        
        Object proxy = buildProxy(referencedBeanName, seataDubboReferenceBean, injectedType);
        
        return proxy;
    }
    
    private Object buildProxy(String referencedBeanName, SeataDubboReferenceBean seataDubboReferenceBean, Class<?> injectedType) throws Exception {
        InvocationHandler handler = buildInvocationHandler(referencedBeanName, seataDubboReferenceBean);
        Object proxy = Proxy.newProxyInstance(getClassLoader(), new Class[] {injectedType }, handler);
        return proxy;
    }
    
    private InvocationHandler buildInvocationHandler(String referencedBeanName, SeataDubboReferenceBean seataDubboReferenceBean) throws Exception {
        
        ReferenceBeanInvocationHandler handler = localReferenceBeanInvocationHandlerCache.get(referencedBeanName);
        
        if (handler == null) {
            handler = new ReferenceBeanInvocationHandler(seataDubboReferenceBean);
        }
        
        if (applicationContext.containsBean(referencedBeanName)) { // Is local @Service Bean or not ?
            // ReferenceBeanInvocationHandler's initialization has to wait for current local @Service Bean has been exported.
            localReferenceBeanInvocationHandlerCache.put(referencedBeanName, handler);
        } else {
            // Remote Reference Bean should initialize immediately
            handler.init();
        }
        
        return handler;
    }
    
    private static class ReferenceBeanInvocationHandler implements InvocationHandler {
        
        private final SeataDubboReferenceBean seataDubboReferenceBean;
        
        private Object                        bean;
        
        private SimpleInterceptor             interceptor;
        
        private ReferenceBeanInvocationHandler(SeataDubboReferenceBean seataDubboReferenceBean) {
            this.seataDubboReferenceBean = seataDubboReferenceBean;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            interceptor.invoke(proxy, method, args);
            return method.invoke(bean, args);
        }
        
        private void init() throws Exception {
            this.bean = seataDubboReferenceBean.getObject();
            if (seataDubboReferenceBean.isSagaProxy()) {
                //interceptor = new SagaSimpleInterceptor();
            } else {
                interceptor = new TccSimpleInterceptor();
            }
        }
    }
    
    public SeataDubboReferenceBean buildReferenceBeanIfAbsent(String referencedBeanName, Object bean,
        InjectionMetadata.InjectedElement injectedElement, boolean isSagaProxy)
        throws Exception {
        SeataDubboReferenceBean seataDubboReferenceBean = dubboReferenceBeanCache.get(referencedBeanName);
        if (seataDubboReferenceBean == null) {
            Object value = ReflectionUtil.getFieldValue(bean, injectedElement.getMember().getName());
            seataDubboReferenceBean = new SeataDubboReferenceBean(value, isSagaProxy);
            dubboReferenceBeanCache.put(referencedBeanName, seataDubboReferenceBean);
        }
        return seataDubboReferenceBean;
    }
    
    public void cacheInjectedReferenceBean(SeataDubboReferenceBean seataDubboReferenceBean, InjectionMetadata.InjectedElement injectedElement) {
        if (injectedElement.getMember() instanceof Field) {
            injectedFieldReferenceBeanCache.put(injectedElement, seataDubboReferenceBean);
        } else if (injectedElement.getMember() instanceof Method) {
            injectedMethodReferenceBeanCache.put(injectedElement, seataDubboReferenceBean);
        }
    }
    
    public String buildInjectedObjectCacheKey(Reference reference, Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) {
        String key = buildReferencedBeanName(reference, injectedType) + "#source=" + (injectedElement.getMember()) + "#attributes="
            + AnnotationUtils.getAttributes(reference, getEnvironment(), true);
        return key;
    }
    
    public String buildReferencedBeanName(Reference reference, Class<?> injectedType) {
        ServiceBeanNameBuilder builder = ServiceBeanNameBuilder.create(reference, injectedType, getEnvironment());
        return getEnvironment().resolvePlaceholders(builder.build());
    }
    
    public Environment getEnvironment() {
        return this.applicationContext.getEnvironment();
    }
    
    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
