package com.youthnightschool.security;

import com.youthnightschool.entity.User;
import com.youthnightschool.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT authentication filter that mirrors the NestJS {@code JwtAuthGuard}.
 *
 * <p>Flow:
 * <ol>
 *   <li>Extract "Authorization: Bearer xxx" from the request header</li>
 *   <li>If no Bearer token is present, continue the filter chain without setting authentication</li>
 *   <li>Verify the token via {@link JwtTokenProvider}</li>
 *   <li>Extract the userId from the claims {@code sub} field</li>
 *   <li>Load the user from the database via {@link UserRepository} to obtain the openid</li>
 *   <li>If the user exists, set the SecurityContext authentication with a
 *       {@link RequestUser} principal</li>
 *   <li>If the user is not found or the token is invalid, clear the SecurityContext
 *       (do not throw; let authorization handle 401/403)</li>
 * </ol>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenProvider jwtTokenProvider;
  private final UserRepository userRepository;

  public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.userRepository = userRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String bearerToken = extractBearerToken(request);

    if (bearerToken == null) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      Claims claims = jwtTokenProvider.verifyAuthToken(bearerToken);
      Integer userId = jwtTokenProvider.getUserId(claims);

      if (userId == null) {
        clearAuthentication();
        filterChain.doFilter(request, response);
        return;
      }

      var userOptional = userRepository.findById(userId);

      if (userOptional.isEmpty()) {
        clearAuthentication();
        filterChain.doFilter(request, response);
        return;
      }

      User user = userOptional.get();
      String unionid = jwtTokenProvider.getUnionid(claims);
      RequestUser requestUser = new RequestUser(user.getOpenid(), user.getId(), unionid);

      List<SimpleGrantedAuthority> authorities = user.getRolesList().stream()
          .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
          .toList();
      var authentication =
          new UsernamePasswordAuthenticationToken(requestUser, null, authorities);
      SecurityContextHolder.getContext().setAuthentication(authentication);

    } catch (JwtException ex) {
      log.debug("JWT verification failed: {}", ex.getMessage());
      clearAuthentication();
    }

    filterChain.doFilter(request, response);
  }

  private String extractBearerToken(HttpServletRequest request) {
    String authorization = request.getHeader("Authorization");
    if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
      return null;
    }
    return authorization.substring(BEARER_PREFIX.length()).trim();
  }

  private void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }
}
