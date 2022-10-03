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
package io.seata.server.console.impl;

import io.seata.common.Constants;
import io.seata.common.util.NetUtil;
import io.seata.common.util.StringUtils;
import io.seata.console.constant.Code;
import io.seata.console.exception.ConsoleException;
import io.seata.console.result.PageResult;
import io.seata.console.result.Result;
import io.seata.core.rpc.ClientInfo;
import io.seata.core.rpc.netty.ChannelManager;
import io.seata.server.console.param.ClientOfflineParam;
import io.seata.server.console.param.ClientQueryParam;
import io.seata.server.console.service.ClientService;
import io.seata.server.console.vo.ClientVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.seata.common.util.StringUtils.isBlank;

/**
 * @author TheR1sing3un
 * @date 2022/7/26 1:50
 * @description ClientService default implement
 */
@Component
@org.springframework.context.annotation.Configuration
public class ClientServiceImpl implements ClientService {

    @Override
    public PageResult<ClientVO> query(ClientQueryParam param) {
        if (param.getPageSize() <= 0 || param.getPageNum() <= 0) {
            throw new ConsoleException(Code.WRONG_PAGE);
        }
        List<ClientVO> clientVOList;
        if (StringUtils.isNullOrEmpty(param.getClientRole())) {
            clientVOList = getAllClients();
        } else if (ClientRole.RM_ROLE.getRole().equals(param.getClientRole())) {
            clientVOList = getRMClients();
        } else if (ClientRole.TM_ROLE.getRole().equals(param.getClientRole())) {
            clientVOList = getTMClients();
        } else {
            throw new ConsoleException(Code.WRONG_CLIENT_ROLE);
        }
        List<ClientVO> clientsFiltered = clientVOList.parallelStream()
                .filter(obtainPredicate(param))
                .collect(Collectors.toList());
        return PageResult.build(clientsFiltered, param.getPageNum(), param.getPageSize());
    }

    @Override
    public Result offline(ClientOfflineParam param) {
        preCheckClientOfflineParam(param);
        if (ClientRole.TM_ROLE.getRole().equals(param.getClientRole())) {
            offlineTMClient(param.getClientId());
        } else if (ClientRole.RM_ROLE.getRole().equals(param.getClientRole())) {
            offlineRMClient(param.getResourceId(), param.getClientId());
        } else {
            throw new ConsoleException(Code.WRONG_CLIENT_ROLE);
        }
        return Result.ok();
    }


    public enum ClientRole {
        /**
         * TM
         */
        TM_ROLE("TMROLE"),
        /**
         * RM
         */
        RM_ROLE("RMROLE");

        private String role;

        ClientRole(String role) {
            this.role = role;
        }

        public String getRole() {
            return role;
        }

    }

    private List<ClientVO> getTMClients() {
        return convertClientInfo2ClientVO(ChannelManager.getAllTMClients());
    }

    private List<ClientVO> getRMClients() {
        return convertClientInfo2ClientVO(ChannelManager.getAllRMClients());
    }

    private List<ClientVO> getAllClients() {
        List<ClientInfo> allTMClients = ChannelManager.getAllTMClients();
        List<ClientInfo> allRMClients = ChannelManager.getAllRMClients();
        List<ClientVO> allClients = new ArrayList<>(allRMClients.size() + allTMClients.size());
        allClients.addAll(convertClientInfo2ClientVO(allRMClients));
        allClients.addAll(convertClientInfo2ClientVO(allTMClients));
        return allClients;
    }

    private void offlineTMClient(String clientId) {
        ChannelManager.offlineTMClient(clientId);
    }

    private void offlineRMClient(String resourceId, String clientId) {
        ChannelManager.offlineRMClient(resourceId, clientId);
    }

    private Predicate<? super ClientVO> obtainPredicate(ClientQueryParam param) {

        return session -> {
            return
                    // ip
                    (isBlank(param.getIp()) || session.getClientId().contains(param.getIp())) &&
                            // applicationId
                            (isBlank(param.getApplicationId()) || session.getApplicationId().contains(param.getApplicationId())) &&
                            // resourceId
                            (isBlank(param.getResourceId()) || param.getResourceId().equals(session.getResourceId()));
        };
    }

    private List<ClientVO> convertClientInfo2ClientVO(List<ClientInfo> clientInfos) {
        List<ClientVO> clientVOList = new ArrayList<>(clientInfos.size());
        clientInfos.forEach(x -> clientVOList.add(new ClientVO(x)));
        return clientVOList;
    }

    private void preCheckClientOfflineParam(ClientOfflineParam param) {
        // need correct format clientId
        if (!isCorrectClientId(param.getClientId())) {
            throw new ConsoleException(Code.LACK_CLIENT_ID);
        }
        // need clientRole
        if (StringUtils.isNullOrEmpty(param.getClientRole())) {
            throw new ConsoleException(Code.LACK_CLIENT_ROLE);
        }
        // need resourceId for RM
        if (ClientRole.RM_ROLE.getRole().equals(param.getClientRole()) && StringUtils.isNullOrEmpty(param.getResourceId())) {
            throw new ConsoleException(Code.LACK_RESOURCE_ID);
        }
        // wrong clientRole
        if (!ClientRole.RM_ROLE.getRole().equals(param.getClientRole()) && !ClientRole.TM_ROLE.getRole().equals(param.getClientRole())) {
            throw new ConsoleException(Code.WRONG_CLIENT_ROLE);
        }
    }

    // need format app:ip:port
    private boolean isCorrectClientId(String clientId) {
        return !StringUtils.isNullOrEmpty(clientId)
                && clientId.split(":").length == 3
                && NetUtil.isCorrectFormatAddress(clientId.split(":")[1] + Constants.CLIENT_ID_SPLIT_CHAR + clientId.split(":")[2]);
    }
}
