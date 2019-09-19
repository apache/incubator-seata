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
package io.seata.spring.annotation.selector;

import io.seata.spring.dubbo.apache.annotation.SeataDubboApacheReferenceScanRegistrar;
import io.seata.spring.dubbo.alibaba.annotation.SeataDubboAlibabaReferenceScanRegistrar;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;


public class SeataDubboReferenceScanRegistrarSelector implements ImportSelector, Ordered {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        try {
            boolean dubboAlibaba = null != Class.forName("com.alibaba.dubbo.config.annotation.Reference");
            if(dubboAlibaba){
                return of(SeataDubboAlibabaReferenceScanRegistrar.class.getName());
            }else{
                return of(SeataDubboApacheReferenceScanRegistrar.class.getName());
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return of(SeataDubboAlibabaReferenceScanRegistrar.class.getName());
        }
    }

    private static <T> T[] of(T... values) {
        return values;
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }


}