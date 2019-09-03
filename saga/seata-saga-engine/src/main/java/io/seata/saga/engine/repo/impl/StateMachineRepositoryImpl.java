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

import io.seata.common.util.StringUtils;
import io.seata.saga.engine.store.StateLangStore;
import io.seata.saga.engine.store.StateLogStore;
import io.seata.saga.engine.repo.StateMachineRepository;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.StateMachine;
import io.seata.saga.statelang.parser.StateMachineParserFactory;
import io.seata.saga.engine.sequence.SeqGenerator;
import io.seata.saga.engine.sequence.SpringJvmUUIDSeqGenerator;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * StateMachineRepository Implementation
 * @author lorne.cl
 */
public class StateMachineRepositoryImpl implements StateMachineRepository {

    private Map<String/** Name **/, Item> stateMachineMapByName = new ConcurrentHashMap<>();
    private Map<String/** Id **/, Item>   stateMachineMapById   = new ConcurrentHashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(StateMachineRepositoryImpl.class);

    private StateLangStore    stateLangStore;
    private SeqGenerator      seqGenerator = new SpringJvmUUIDSeqGenerator();
    private String            charset = "UTF-8";

    private static class Item {

        private StateMachine value;

        private Item(){}

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

    @Override
    public StateMachine getStateMachineById(String stateMachineId) {

        Item item = stateMachineMapById.get(stateMachineId);
        if(item == null){
            Item newItem = new Item();
            item = stateMachineMapById.putIfAbsent(stateMachineId, newItem);
            if(item == null){
                item = newItem;
            }

        }
        if(item.getValue() == null && stateLangStore!=null){
            synchronized (item){
                if(item.getValue() == null && stateLangStore!=null){
                    StateMachine stateMachine = stateLangStore.getStateMachineById(stateMachineId);
                    if(stateMachine != null){
                        StateMachine parsedStatMachine = StateMachineParserFactory.getStateMachineParser().parse(stateMachine.getContent());
                        if(parsedStatMachine == null){
                            throw new RuntimeException("Parse State Language failed, stateMachineId："+stateMachine.getId()+", name:"+stateMachine.getName());
                        }
                        stateMachine.setStartAt(parsedStatMachine.getStartAt());
                        stateMachine.getStates().putAll(parsedStatMachine.getStates());
                        item.setValue(stateMachine);
                        stateMachineMapByName.put(stateMachine.getName(), item);
                    }

                }
            }
        }
        return item.getValue();
    }

    @Override
    public StateMachine getStateMachine(String stateMachineName) {
        Item item = stateMachineMapByName.get(stateMachineName);
        if(item == null){
            Item newItem = new Item();
            item = stateMachineMapByName.putIfAbsent(stateMachineName, newItem);
            if(item == null){
                item = newItem;
            }
        }
        if(item.getValue() == null && stateLangStore!=null){
            synchronized (item){
                if(item.getValue() == null && stateLangStore!=null){
                    StateMachine stateMachine = stateLangStore.getStateMachineById(stateMachineName);
                    if(stateMachine != null){
                        StateMachine parsedStatMachine = StateMachineParserFactory.getStateMachineParser().parse(stateMachine.getContent());
                        if(parsedStatMachine == null){
                            throw new RuntimeException("Parse State Language failed, stateMachineId："+stateMachine.getId()+", name:"+stateMachine.getName());
                        }
                        stateMachine.setStartAt(parsedStatMachine.getStartAt());
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
    public StateMachine getStateMachine(String stateMachineName, String version) {
        throw new UnsupportedOperationException("not implement yet");
    }

    @Override
    public StateMachine registryStateMachine(StateMachine stateMachine) {

        String stateMachineName = stateMachine.getName();

        if(stateLangStore != null){
            StateMachine oldStateMachine = stateLangStore.getLastVersionStateMachine(stateMachineName);

            if (oldStateMachine != null) {
                byte[] oldBytesContent = null;
                byte[] bytesContent = null;
                try {
                    oldBytesContent = oldStateMachine.getContent().getBytes(charset);
                    bytesContent = stateMachine.getContent().getBytes(charset);
                } catch (UnsupportedEncodingException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                if (Arrays.equals(bytesContent, oldBytesContent) && stateMachine.getVersion() != null && stateMachine.getVersion().equals(
                        oldStateMachine.getVersion())) {

                    LOGGER.info("StateMachine[" + stateMachineName + "] is already exist a same version");

                    stateMachine.setId(oldStateMachine.getId());
                    stateMachine.setGmtCreate(oldStateMachine.getGmtCreate());

                    Item item = new Item(stateMachine);
                    stateMachineMapByName.put(stateMachineName, item);
                    stateMachineMapById.put(stateMachine.getId(), item);
                    return stateMachine;
                }
            }
            stateMachine.setId(seqGenerator.generate(DomainConstants.SEQ_NAME_STATE_MACHINE));
            stateMachine.setGmtCreate(new Date());
            stateLangStore.storeStateMachine(stateMachine);
        }

        if(StringUtils.isBlank(stateMachine.getId())){
            stateMachine.setId(seqGenerator.generate(DomainConstants.SEQ_NAME_STATE_MACHINE));
        }

        Item item = new Item(stateMachine);
        stateMachineMapByName.put(stateMachineName, item);
        stateMachineMapById.put(stateMachine.getId(), item);
        return stateMachine;
    }

    public void load(Resource[] resources) throws IOException {
        if (resources != null) {
            for (Resource resource : resources) {
                String json = IOUtils.toString(resource.getInputStream(), "UTF-8");
                StateMachine stateMachine = StateMachineParserFactory.getStateMachineParser().parse(json);
                if (stateMachine != null) {
                    stateMachine.setContent(json);
                    registryStateMachine(stateMachine);

                    LOGGER.info("===== StateMachine Loaded: \n" + json);
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
}