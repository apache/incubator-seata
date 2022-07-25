package io.seata.server.console.impl;

import io.seata.console.result.PageResult;
import io.seata.core.rpc.ClientInfo;
import io.seata.core.rpc.netty.ChannelManager;
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
 * @description
 */
@Component
@org.springframework.context.annotation.Configuration
public class ClientServiceImpl implements ClientService {

    @Override
    public PageResult<ClientVO> query(ClientQueryParam param) {
        if (param.getPageSize() <= 0 || param.getPageNum() <= 0) {
            throw new IllegalArgumentException("wrong pageSize or pageNum");
        }
        List<ClientVO> clientVOList;
        if (param.getClientRole() == null || "".equals(param.getClientRole())) {
            clientVOList = getAllClients();
        } else if (ClientRole.RM_ROLE.getRole().equals(param.getClientRole())){
            clientVOList = getRMClients();
        } else if (ClientRole.TM_ROLE.getRole().equals(param.getClientRole())){
            clientVOList = getTMClients();
        } else {
            throw new IllegalArgumentException("wrong client role");
        }
        List<ClientVO> clientsFiltered = clientVOList.parallelStream()
                .filter(obtainPredicate(param))
                .collect(Collectors.toList());
        return PageResult.build(clientsFiltered, param.getPageNum(), param.getPageSize());
    }

    public enum ClientRole {
        TM_ROLE("TMROLE"),
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
        return allClients;
    }

    private Predicate<? super ClientVO> obtainPredicate(ClientQueryParam param) {

        return session -> {
            return
                    // ip
                    (isBlank(param.getIp()) || session.getClientId().contains(param.getIp())) &&
                            // applicationId
                            (isBlank(param.getApplicationId()) || session.getApplicationId().contains(param.getApplicationId())) &&
                            // resourceId
                            (isBlank(param.getResourceId()) || session.getResourceId().contains(param.getResourceId()));
        };
    }

    private List<ClientVO> convertClientInfo2ClientVO(List<ClientInfo> clientInfos) {
        List<ClientVO> clientVOList = new ArrayList<>(clientInfos.size());
        clientInfos.forEach(x -> clientVOList.add(new ClientVO(x)));
        return clientVOList;
    }
}
