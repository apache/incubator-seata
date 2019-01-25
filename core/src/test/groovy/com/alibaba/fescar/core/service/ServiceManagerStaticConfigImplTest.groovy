package com.alibaba.fescar.core.service

import com.alibaba.fescar.config.Configuration
import spock.lang.Specification

/**
 * Created by qiliang on 2019/1/25.
 */
class ServiceManagerStaticConfigImplTest extends Specification {

    def  service = new ServiceManagerStaticConfigImpl()

    def configuration = Mock(Configuration)

    def setup(){
        service.configuration = configuration
    }


    def 'this is example for spock test'() {

        setup:
        def pam ="txServiceGroup"

        configuration.getConfig(ConfigurationKeys.SERVICE_GROUP_MAPPING_PREFIX + pam) >> pam
        configuration.getConfig(ConfigurationKeys.SERVICE_PREFIX + pam + ConfigurationKeys.GROUPLIST_POSTFIX) >> pam

        when:
        def result = service.lookup(pam)
        then:
        result == ["txServiceGroup"]


    }

}
