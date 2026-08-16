package com.jomariabejo.connectly_api.config;

import com.jomariabejo.connectly_api.model.Role;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.service.JwtService;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JwtAuthenticationFilter}.
 *
 * <p>The filter is exercised directly through its protected {@code doFilterInternal} -- the test
 * lives in the same package, so no servlet container or {@code OncePerRequestFilter} plumbing is
 * needed. Requests, responses and the chain come from {@code spring-test}'s mock web classes;
 * {@link MockFilterChain#getRequest()} stays {@code null} until the chain is invoked, which is how
 * the tests tell "the chain continued" from "the request was swallowed".
 *
 * <p>{@link SecurityContextHolder} is backed by a static {@code ThreadLocal}, and JUnit runs every
 * test of a class on the same thread. Any authentication a test stores would therefore leak into
 * the next test (or into other test classes) unless the context is wiped both before each test --
 * in case an earlier class left residue -- and after each test, so this class cleans up after
 * itself. Hence {@code clearContext()} in both {@code @BeforeEach} and {@code @AfterEach}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    private static final String EMAIL = "someone@example.com";

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HandlerExceptionResolver handlerExceptionResolver;

    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    private User user;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        filter = new JwtAuthenticationFilter(jwtService, userDetailsService, handlerExceptionResolver);

        request = new MockHttpServletRequest("GET", "/api/posts");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        Role role = new Role();
        role.setId(2L);
        role.setName("USER");

        user = new User("someone", "hashed", EMAIL);
        user.setId(1L);
        user.getRoles().add(role);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("without a bearer token")
    class WithoutBearerToken {

        @Test
        @DisplayName("passes a request with no Authorization header straight through")
        void missingHeaderIsIgnored() throws Exception {
            filter.doFilterInternal(request, response, chain);

            assertThat(chain.getRequest())
                    .as("the chain must still be invoked for anonymous requests")
                    .isSameAs(request);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verifyNoInteractions(jwtService, userDetailsService);
        }

        @Test
        @DisplayName("ignores an Authorization header that is not a Bearer scheme")
        void nonBearerSchemeIsIgnored() throws Exception {
            request.addHeader("Authorization", "Basic c29tZW9uZTpzZWNyZXQ=");

            filter.doFilterInternal(request, response, chain);

            assertThat(chain.getRequest()).isSameAs(request);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verifyNoInteractions(jwtService, userDetailsService);
        }
    }

    @Nested
    @DisplayName("with a bearer token")
    class WithBearerToken {

        @Test
        @DisplayName("authenticates the request when the token is valid")
        void validTokenAuthenticates() throws Exception {
            request.addHeader("Authorization", "Bearer valid-token");
            when(jwtService.extractUsername("valid-token")).thenReturn(EMAIL);
            when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(user);
            when(jwtService.isTokenValid("valid-token", user)).thenReturn(true);

            filter.doFilterInternal(request, response, chain);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
            assertThat(authentication.getPrincipal())
                    .as("the loaded UserDetails becomes the principal")
                    .isSameAs(user);
            assertThat(authentication.getCredentials())
                    .as("the credentials are erased -- the token itself is never stored")
                    .isNull();
            assertThat(authentication.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
            assertThat(authentication.getDetails()).isInstanceOf(WebAuthenticationDetails.class);
            assertThat(chain.getRequest()).isSameAs(request);
        }

        @Test
        @DisplayName("leaves the context empty when the token fails validation, but lets the request continue")
        void invalidTokenLeavesContextEmpty() throws Exception {
            request.addHeader("Authorization", "Bearer stale-token");
            when(jwtService.extractUsername("stale-token")).thenReturn(EMAIL);
            when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(user);
            when(jwtService.isTokenValid("stale-token", user)).thenReturn(false);

            filter.doFilterInternal(request, response, chain);

            assertThat(SecurityContextHolder.getContext().getAuthentication())
                    .as("a rejected token must not authenticate anyone")
                    .isNull();
            assertThat(chain.getRequest())
                    .as("the request proceeds anonymously; authorization decides later")
                    .isSameAs(request);
        }

        @Test
        @DisplayName("never looks up a user when the token carries no subject")
        void nullSubjectSkipsUserLookup() throws Exception {
            request.addHeader("Authorization", "Bearer no-subject-token");
            when(jwtService.extractUsername("no-subject-token")).thenReturn(null);

            filter.doFilterInternal(request, response, chain);

            verifyNoInteractions(userDetailsService);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(chain.getRequest()).isSameAs(request);
        }

        @Test
        @DisplayName("does not re-authenticate a request that is already authenticated")
        void alreadyAuthenticatedRequestIsLeftAlone() throws Exception {
            User earlier = new User("earlier", "hashed", "earlier@example.com");
            earlier.setId(99L);
            Authentication existing = new UsernamePasswordAuthenticationToken(
                    earlier, null, earlier.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(existing);

            request.addHeader("Authorization", "Bearer valid-token");
            when(jwtService.extractUsername("valid-token")).thenReturn(EMAIL);

            filter.doFilterInternal(request, response, chain);

            verifyNoInteractions(userDetailsService);
            assertThat(SecurityContextHolder.getContext().getAuthentication())
                    .as("the pre-existing authentication must survive untouched")
                    .isSameAs(existing);
            assertThat(chain.getRequest()).isSameAs(request);
        }
    }

    @Nested
    @DisplayName("when token processing throws")
    class WhenTokenProcessingThrows {

        @Test
        @DisplayName("hands the exception to the HandlerExceptionResolver instead of letting it escape")
        void routesExceptionToResolver() throws Exception {
            request.addHeader("Authorization", "Bearer garbage");
            MalformedJwtException boom = new MalformedJwtException("Malformed JWT");
            when(jwtService.extractUsername("garbage")).thenThrow(boom);

            filter.doFilterInternal(request, response, chain);

            verify(handlerExceptionResolver).resolveException(request, response, null, boom);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(chain.getRequest())
                    .as("a request with a broken token stops at the filter")
                    .isNull();
            verify(userDetailsService, never()).loadUserByUsername(EMAIL);
        }
    }
}
