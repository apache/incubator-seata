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

import org.apache.dubbo.config.spring.util.BeanRegistrar;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * <P>Copyright (C), 2003-2019, 浩鲸云计算科技股份有限公司</P>
 * <P>描述说明：</P>
 *
 * @author ZHONGFUHUA-PC
 * @date 2019/9/18 13:37
 * @since JDK1.8
 */
public class SeataDubboApacheReferenceScanRegistrar implements ImportBeanDefinitionRegistrar {
    
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        BeanRegistrar.registerInfrastructureBean(registry, SeataReferenceAnnotationBeanPostProcessor.BEAN_NAME,
            SeataReferenceAnnotationBeanPostProcessor.class);
    }
}
