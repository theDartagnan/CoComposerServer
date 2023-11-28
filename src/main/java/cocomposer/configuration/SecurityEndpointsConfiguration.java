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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 *
 * @author Remi Venant
 */
@Configuration
@EnableWebSecurity
//@EnableWebSocketSecurity
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
    @Profile("cors")
    CorsConfiguration corsConfiguration() {
        final CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:*", "http://127.0.0.1:*", "moz-extension://*"));
        configuration.setAllowedMethods(Arrays.asList("OPTIONS", "HEAD", "GET", "POST", "PUT", "PATCH", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("content-type", "Accept", "Accept-Language", "Authorization", "X-Requested-With", "x-xsrf-token"));
        configuration.setAllowCredentials(Boolean.TRUE);
        return configuration;
    }

    @Bean
    @Profile("cors")
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
            Optional<CorsConfigurationSource> corsConfigurationSource) throws Exception {
        return http
                .exceptionHandling(eh -> eh.accessDeniedHandler(new AccessDeniedHandlerImpl())) // return a simple 403 error if unauthorized
                .cors(cors -> {
                    if (corsConfigurationSource.isPresent()) {
                        cors.configurationSource(corsConfigurationSource.get());
                    }
                }) // Enable cors for localhost
                .addFilterAt(restAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // Add the rest authentication filter
                .logout(logoutCustomizer -> logoutCustomizer.logoutUrl("/api/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies(this.sessionCookieName)
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                ) // Set logout uri and enforce session cookie deletion
                .sessionManagement(sm -> {
                    sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).maximumSessions(1);
                    sm.sessionFixation(sf -> sf.newSession());
                }) // Set 1 session max per user and enforce new session creation on authentication to avoir session fixation
                .headers(h -> h.frameOptions(fo -> fo.sameOrigin())) // Enforce same origin for framing (use with sockjs & old version of IE)
                .csrf(csrf -> csrf.disable())
                //.csrf(csrf -> ??) // Configure CSRF
                // Next is firewall HTTP request rules
                .authorizeHttpRequests(ahr -> ahr
                .requestMatchers(HttpMethod.OPTIONS).permitAll() // allow all options request (for cors)
                .requestMatchers(HttpMethod.POST, "/api/v1/rest/accounts").permitAll() // Allow public account creation
                .requestMatchers("/api/v1/rest/**").authenticated() // Minimum requirements for api 'rest or websocket) access: being authenticated
                //.requestMatchers(HttpMethod.GET).permitAll() // allow error access page and all static resources
                .anyRequest().denyAll()) // Any other request refused
                .build();
    }
}
