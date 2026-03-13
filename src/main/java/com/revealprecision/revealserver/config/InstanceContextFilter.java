package com.revealprecision.revealserver.config;

import com.revealprecision.revealserver.service.InstanceService;
import com.revealprecision.revealserver.service.UserService;
import com.revealprecision.revealserver.util.UserUtils;
import java.io.IOException;
import java.util.UUID;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class InstanceContextFilter extends OncePerRequestFilter {

    public static String INSTANCE_HEADER_KEY = "X-Instance-ID";

    private final InstanceService instanceService;
    private final UserService  userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        try {
            String instanceIdHeader = request.getHeader(INSTANCE_HEADER_KEY);

            if(StringUtils.isNotEmpty(instanceIdHeader)){
                UUID instanceId = UUID.fromString(instanceIdHeader);
                UUID userId = userService.getCurrentUser().getIdentifier(); // from SecurityContext

                if (userId != null) {
                    // Validate user is actually a member of this instance
                    if (!instanceService.isMember(userId, instanceId)) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "User is not a member of this instance");
                        return;
                    }
                    // Store in thread-local context
                    InstanceContext.set(instanceId);
                }
            }

            chain.doFilter(request, response);
        }
        finally {
            InstanceContext.clear(); // CRITICAL: prevent thread leaks
        }
    }
}