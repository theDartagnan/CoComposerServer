/*
 * Copyright (C) 2023 IUT Laval - Le Mans Université.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cocomposer.configuration;

import cocomposer.security.authentification.RestAuthenticationFilter;
import cocomposer.security.csrf.CsrfCookieFilter;
import cocomposer.security.csrf.SpaCsrfTokenRequestHandler;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 *
 * @author Remi Venant
 */
@Configuration
@EnableWebSecurity
@EnableWebSocketSecurity
public class SecurityEndpointsConfiguration {

    private static final Log LOG = LogFactory.getLog(SecurityEndpointsConfiguration.class);

    @Value("${server.servlet.session.cookie.name:JSESSIONID}")
    private String sessionCookieName;

    @Bean
    public DaoAuthenticationProvider mongoLocalAuthenticatitionProvider(UserDetailsService userDetailsService, PasswordEncoder encoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(encoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(List<AuthenticationProvider> authenticationProviders) {
        return new ProviderManager(authenticationProviders);
    }

    @Bean
    public HttpSessionSecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public RestAuthenticationFilter restAuthenticationFilter(
            AuthenticationManager authenticationManager,
            HttpSessionSecurityContextRepository securityContextRepository) throws Exception {
        RestAuthenticationFilter filter = new RestAuthenticationFilter();
        filter.setFilterProcessesUrl("/api/login");
        filter.setAuthenticationManager(authenticationManager);
        filter.setAuthenticationSuccessHandler(new SimpleUrlAuthenticationSuccessHandler("/api/v1/rest/accounts/myself"));
        filter.setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler());
        filter.setSecurityContextRepository(securityContextRepository);
        return filter;
    }

    @Bean
    @ConditionalOnProperty(name = "app.security.cors", havingValue = "true")
    CorsConfiguration corsConfiguration() {
        final CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:*", "http://127.0.0.1:*", "moz-extension://*"));
        configuration.setAllowedMethods(Arrays.asList("OPTIONS", "HEAD", "GET", "POST", "PUT", "PATCH", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("content-type", "Accept", "Accept-Language", "Authorization", "X-Requested-With", "x-xsrf-token"));
        configuration.setAllowCredentials(Boolean.TRUE);
        configuration.setMaxAge(Duration.ofHours(6));
        return configuration;
    }

    @Bean
    @ConditionalOnProperty(name = "app.security.cors", havingValue = "true")
    CorsConfigurationSource corsConfigurationSource(CorsConfiguration corsConfiguration) {
        LOG.warn("CONFIGURE HTTP SECURITY WITH CORS");
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

    // Security on rest enpoints and authentication/session mgmt
    @Bean
    public SecurityFilterChain MultiSecFilterChain(HttpSecurity http,
            RestAuthenticationFilter restAuthenticationFilter,
            Optional<CorsConfigurationSource> corsConfigurationSource,
            AppSecurityConfigurationProperties appSecProperties) throws Exception {
        // Gestion de l'accès non autorisé : renvoie simplement une 403, du point d'autentification,
        // de la déconnection
        http
                .exceptionHandling(eh -> eh.accessDeniedHandler(new AccessDeniedHandlerImpl()))
                .addFilterAt(restAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .logout(logoutCustomizer -> logoutCustomizer.logoutUrl("/api/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies(this.sessionCookieName)
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                );
        // Gestion des session : création uniquement si demandé, au max 1 par utilisateur, et force la création
        // de nouvelle session
        http.sessionManagement(sm -> {
            sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).maximumSessions(1);
            sm.sessionFixation(sf -> sf.newSession());
        });
        // Gestion du CORS au besoin
        http.cors(cors -> {
            if (corsConfigurationSource.isPresent()) {
                LOG.info("ENHANCE CORS FOR HTTP");
                cors.configurationSource(corsConfigurationSource.get());
            }
        });

        // Protection pour SockJS en cas d'injection de Frame dans la page sur vieux navigateurs
        http.headers(h -> h.frameOptions(fo -> fo.sameOrigin()));

        // Protection CSRF pour une SPA (Double cookie, chiffré) si activé
        if (appSecProperties.isCsrf()) {
            CookieCsrfTokenRepository csrfTokenRepo = new CookieCsrfTokenRepository();
            csrfTokenRepo.setCookieCustomizer((cookieBuilder) -> {
                cookieBuilder
                        .path("/")
                        .httpOnly(false)
                        .sameSite("lax").build();
            });
            http.csrf(csrf -> csrf
                    .ignoringRequestMatchers("/api/websocket/**") // request initialisation SockJS non prise en compte
                    .csrfTokenRepository(csrfTokenRepo) // Token stocké dans un cookie
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())) // Résolution mixte : en clair quand token transmis via en-tête ou param de requête, chiffré sinon
                    .addFilterAfter(new CsrfCookieFilter(), UsernamePasswordAuthenticationFilter.class); // pour la mise à jour du cookie au besoin
        }

        // Par-feu applicatif
        http.authorizeHttpRequests(ahr -> {
            if (appSecProperties.isCors()) {
                // Autorise les requête OPTIONS à tous si cors
                ahr.requestMatchers(HttpMethod.OPTIONS).permitAll();
            }
            ahr
                    .requestMatchers(HttpMethod.GET, "/api/v1/rest/accounts/myself").permitAll() // Allow public access to self account for session checking
                    .requestMatchers(HttpMethod.POST, "/api/v1/rest/accounts").anonymous()// Crétion de compte publique autorisé
                    .requestMatchers("/api/**").authenticated() // toute l'api authentifié
                    .anyRequest().denyAll(); // Tout autre requete interdit
        });

        return http.build();
    }

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(MessageMatcherDelegatingAuthorizationManager.Builder messages) {
        messages.nullDestMatcher().authenticated()
                .simpDestMatchers("/app/compositions.*").authenticated()
                .simpSubscribeDestMatchers("/user/queue/errors", "/user/queue/compositions", "/topic/compositions.*").authenticated()
                .anyMessage().denyAll();
        return messages.build();
    }
}
