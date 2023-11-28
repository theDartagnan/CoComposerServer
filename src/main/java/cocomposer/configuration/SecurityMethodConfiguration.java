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

import cocomposer.model.CompositionRepository;
import cocomposer.security.authorization.CoCompositionPermissionEvaluator;
import jakarta.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 *
 * @author Remi Venant
 */
@Configuration
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class SecurityMethodConfiguration {

    private static final Log LOG = LogFactory.getLog(SecurityMethodConfiguration.class);

    @PostConstruct
    private void init() {
        LOG.info("ENFORCING METHOD SECURITY");
    }

    @Bean
    public DefaultMethodSecurityExpressionHandler methodExpressionHandler(PermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler dmse = new DefaultMethodSecurityExpressionHandler();
        dmse.setPermissionEvaluator(permissionEvaluator);
        return dmse;
    }

    @Bean
    public PermissionEvaluator permissionEvaluator(CompositionRepository compoRepo) {
        return new CoCompositionPermissionEvaluator(compoRepo);
    }
}
