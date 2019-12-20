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
package io.seata.saga.engine.repo.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.util.StringUtils;
import io.seata.saga.engine.repo.StateMachineRepository;
import io.seata.saga.engine.sequence.SeqGenerator;
import io.seata.saga.engine.sequence.SpringJvmUUIDSeqGenerator;
import io.seata.saga.engine.store.StateLangStore;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.StateMachine;
import io.seata.saga.statelang.parser.StateMachineParserFactory;
import io.seata.saga.statelang.parser.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/**
 * StateMachineRepository Implementation
 *
 * @author lorne.cl
 */
public class StateMachineRepositoryImpl implements StateMachineRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(StateMachineRepositoryImpl.class);
    private Map<String/** Name_Tenant **/, Item> stateMachineMapByNameAndTenant = new ConcurrentHashMap<>();
    private Map<String/** Id **/, Item> stateMachineMapById = new ConcurrentHashMap<>();
    private StateLangStore stateLangStore;
    private SeqGenerator seqGenerator = new SpringJvmUUIDSeqGenerator();
    private String charset = "UTF-8";
    private String defaultTenantId;

    @Override
    public StateMachine getStateMachineById(String stateMachineId) {

        Item item = stateMachineMapById.get(stateMachineId);
        if (item == null) {
            Item newItem = new Item();
            item = stateMachineMapById.putIfAbsent(stateMachineId, newItem);
            if (item == null) {
                item = newItem;
            }

        }
        if (item.getValue() == null && stateLangStore != null) {
            synchronized (item) {
                if (item.getValue() == null && stateLangStore != null) {
                    StateMachine stateMachine = stateLangStore.getStateMachineById(stateMachineId);
                    if (stateMachine != null) {
                        StateMachine parsedStatMachine = StateMachineParserFactory.getStateMachineParser().parse(
                            stateMachine.getContent());
                        if (parsedStatMachine == null) {
                            throw new RuntimeException(
                                "Parse State Language failed, stateMachineId:" + stateMachine.getId() + ", name:"
                                    + stateMachine.getName());
                        }
                        stateMachine.setStartState(parsedStatMachine.getStartState());
                        stateMachine.getStates().putAll(parsedStatMachine.getStates());
                        item.setValue(stateMachine);
                        stateMachineMapByNameAndTenant.put(stateMachine.getName() + "_" + stateMachine.getTenantId(),
                            item);
                    }

                }
            }
        }
        return item.getValue();
    }

    @Override
    public StateMachine getStateMachine(String stateMachineName, String tenantId) {
        Item item = stateMachineMapByNameAndTenant.get(stateMachineName + "_" + tenantId);
        if (item == null) {
            Item newItem = new Item();
            item = stateMachineMapByNameAndTenant.putIfAbsent(stateMachineName + "_" + tenantId, newItem);
            if (item == null) {
                item = newItem;
            }
        }
        if (item.getValue() == null && stateLangStore != null) {
            synchronized (item) {
                if (item.getValue() == null && stateLangStore != null) {
                    StateMachine stateMachine = stateLangStore.getLastVersionStateMachine(stateMachineName, tenantId);
                    if (stateMachine != null) {
                        StateMachine parsedStatMachine = StateMachineParserFactory.getStateMachineParser().parse(
                            stateMachine.getContent());
                        if (parsedStatMachine == null) {
                            throw new RuntimeException(
                                "Parse State Language failed, stateMachineId:" + stateMachine.getId() + ", name:"
                                    + stateMachine.getName());
                        }
                        stateMachine.setStartState(parsedStatMachine.getStartState());
                        stateMachine.getStates().putAll(parsedStatMachine.getStates());
                        item.setValue(stateMachine);
                        stateMachineMapById.put(stateMachine.getId(), item);
                    }

                }
            }
        }
        return item.getValue();
    }

    @Override
    public StateMachine getStateMachine(String stateMachineName, String tenantId, String version) {
        throw new UnsupportedOperationException("not implement yet");
    }

    @Override
    public StateMachine registryStateMachine(StateMachine stateMachine) {

        String stateMachineName = stateMachine.getName();
        String tenantId = stateMachine.getTenantId();

        if (stateLangStore != null) {
            StateMachine oldStateMachine = stateLangStore.getLastVersionStateMachine(stateMachineName, tenantId);

            if (oldStateMachine != null) {
                byte[] oldBytesContent = null;
                byte[] bytesContent = null;
                try {
                    oldBytesContent = oldStateMachine.getContent().getBytes(charset);
                    bytesContent = stateMachine.getContent().getBytes(charset);
                } catch (UnsupportedEncodingException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                if (Arrays.equals(bytesContent, oldBytesContent) && stateMachine.getVersion() != null && stateMachine
                    .getVersion().equals(oldStateMachine.getVersion())) {

                    LOGGER.info("StateMachine[{}] is already exist a same version", stateMachineName);

                    stateMachine.setId(oldStateMachine.getId());
                    stateMachine.setGmtCreate(oldStateMachine.getGmtCreate());

                    Item item = new Item(stateMachine);
                    stateMachineMapByNameAndTenant.put(stateMachineName + "_" + tenantId, item);
                    stateMachineMapById.put(stateMachine.getId(), item);
                    return stateMachine;
                }
            }
            stateMachine.setId(seqGenerator.generate(DomainConstants.SEQ_ENTITY_STATE_MACHINE));
            stateMachine.setGmtCreate(new Date());
            stateLangStore.storeStateMachine(stateMachine);
        }

        if (StringUtils.isBlank(stateMachine.getId())) {
            stateMachine.setId(seqGenerator.generate(DomainConstants.SEQ_ENTITY_STATE_MACHINE));
        }

        Item item = new Item(stateMachine);
        stateMachineMapByNameAndTenant.put(stateMachineName + "_" + tenantId, item);
        stateMachineMapById.put(stateMachine.getId(), item);
        return stateMachine;
    }

    @Override
    public void registryByResources(Resource[] resources, String tenantId) throws IOException {
        if (resources != null) {
            for (Resource resource : resources) {
                String json = IOUtils.toString(resource.getInputStream(), charset);
                StateMachine stateMachine = StateMachineParserFactory.getStateMachineParser().parse(json);
                if (stateMachine != null) {
                    stateMachine.setContent(json);
                    if (StringUtils.isBlank(stateMachine.getTenantId())) {
                        stateMachine.setTenantId(tenantId);
                    }
                    registryStateMachine(stateMachine);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("===== StateMachine Loaded: \n{}", json);
                    }
                }
            }
        }
    }

    public void setStateLangStore(StateLangStore stateLangStore) {
        this.stateLangStore = stateLangStore;
    }

    public void setSeqGenerator(SeqGenerator seqGenerator) {
        this.seqGenerator = seqGenerator;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getDefaultTenantId() {
        return defaultTenantId;
    }

    public void setDefaultTenantId(String defaultTenantId) {
        this.defaultTenantId = defaultTenantId;
    }

    private static class Item {

        private StateMachine value;

        private Item() {
        }

        private Item(StateMachine value) {
            this.value = value;
        }

        public StateMachine getValue() {
            return value;
        }

        public void setValue(StateMachine value) {
            this.value = value;
        }
    }
}