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
package cocomposer.security.authorization;

import cocomposer.model.CompositionRepository;
import java.util.function.Supplier;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.messaging.access.intercept.MessageAuthorizationContext;

/**
 *
 * @author Remi Venant
 * @param <T>
 */
public class CompositionTopicSubscriptionAuthorizationManager<T> implements AuthorizationManager<MessageAuthorizationContext<T>> {

    private static final Log LOG = LogFactory.getLog(CompositionTopicSubscriptionAuthorizationManager.class);

    private final AuthorizationManager authenticatedAuthorizationManager = AuthenticatedAuthorizationManager.authenticated();

    private final CompositionRepository compositionRepo;

    public CompositionTopicSubscriptionAuthorizationManager(CompositionRepository compositionRepo) {
        this.compositionRepo = compositionRepo;
    }

    private String checkSubscriptionAndextractCompositionId(Message<T> message) {
        if (message == null) {
            LOG.debug("Autfhorizing composition topic sub: No message");
            return null;
        }
        // Extract Stomp headers
        final StompHeaderAccessor sha = StompHeaderAccessor.wrap(message);
        if (sha == null) {
            LOG.debug("Authorizing composition topic sub: No stom headers");
            return null;
        }
        // Check command is subscription
        if (sha.getCommand() != StompCommand.SUBSCRIBE) {
            LOG.debug("Authorizing composition topic sub: No subscription message");
            return null;
        }
        //
        final String topicDestination = sha.getDestination();
        if (topicDestination == null) {
            LOG.debug("Authorizing composition topic sub: no destination topic");
            return null;
        }
        final String[] destinationComponents = topicDestination.split("\\.", 2);
        if (destinationComponents.length != 2 || !destinationComponents[0].equals("/topic/compositions")) {
            LOG.debug("Authorizing composition topic sub: no two-parts destination topic");
            return null;
        }
        return destinationComponents[1];
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, MessageAuthorizationContext<T> object) {
        // Use Authentified ?
        AuthorizationDecision isAuthentified = this.authenticatedAuthorizationManager.check(authentication, object);
        if (isAuthentified == null || !isAuthentified.isGranted()) {
            LOG.info("Authorizing composition topic sub: user not authentified");
            return new AuthorizationDecision(false);
        }
        // Message is subscription with compositionId ?
        final String compositionId = this.checkSubscriptionAndextractCompositionId(object.getMessage());
        if (compositionId == null) {
            LOG.info("Authorizing composition topic sub: no composition id");
            return new AuthorizationDecision(false);
        }
        // Extract current user. Authencation will not return null to get() call as user is already authentified
        final String userId = authentication.get().getName();

        // Use can access the composition collaboratively ?
        if (!this.compositionRepo.canUserEditCollabCompo(compositionId, userId)) {
            LOG.info("Authorizing composition topic sub: user cannot access the compo collaboratively");
            return new AuthorizationDecision(false);
        }

        // User ha access
        return new AuthorizationDecision(true);
    }

}
