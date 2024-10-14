package org.apache.seata.server.auth.controller;

import org.apache.seata.common.result.Code;
import org.apache.seata.common.result.SingleResult;
import org.apache.seata.server.auth.config.ClusterSecurityConfig;
import org.apache.seata.server.auth.security.User;
import org.apache.seata.server.auth.utils.RaftRegJwtTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/metadata/v1/auth")
public class ClusterAuthController {
    @Autowired
    @Qualifier("clusterJwtTokenUtils")
    private RaftRegJwtTokenUtils jwtTokenUtils;
    @Autowired
    @Qualifier("clusterAuthenticationManager")
    private AuthenticationManager authenticationManager;

    /**
     * Whether the Seata is in broken states or not, and cannot recover except by being restarted
     *
     * @param response the response
     * @param user     the user
     * @return HTTP code equal to 200 indicates that Seata is in right states. HTTP code equal to 500 indicates that
     * Seata is in broken states.
     */
    @PostMapping("/login")
    public SingleResult<String> login(HttpServletResponse response, @RequestBody User user) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                user.getUsername(), user.getPassword());
        try {
            //AuthenticationManager(default ProviderManager) #authenticate check Authentication
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            //bind authentication to securityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
            //create token
            String accessToken = jwtTokenUtils.createAccessToken(authentication);
            String refreshToken = jwtTokenUtils.createRefreshToken(authentication);

            String authHeader = ClusterSecurityConfig.TOKEN_PREFIX + accessToken;
            //put token into http header
            response.addHeader(ClusterSecurityConfig.AUTHORIZATION_HEADER, authHeader);
            response.addHeader(ClusterSecurityConfig.REFRESH_TOKEN, refreshToken);

            return new SingleResult<>(Code.SUCCESS, authHeader);
        } catch (BadCredentialsException authentication) {
            return new SingleResult<>(Code.LOGIN_FAILED);
        }
    }
}
