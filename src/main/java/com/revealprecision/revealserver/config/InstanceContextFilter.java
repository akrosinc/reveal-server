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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class InstanceContextFilter extends OncePerRequestFilter {

    private final InstanceService instanceService;
    private final UserService  userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        UUID instanceId = UUID.fromString(request.getHeader("X-Instance-ID"));
        UUID userId = userService.getCurrentUser().getIdentifier(); // from SecurityContext

        if (instanceId != null && userId != null) {
            // Validate user is actually a member of this instance
            if (!instanceService.isMember(userId, instanceId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "User is not a member of this instance");
                return;
            }
            // Store in thread-local context
            InstanceContext.set(instanceId);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            InstanceContext.clear(); // CRITICAL: prevent thread leaks
        }
    }
}