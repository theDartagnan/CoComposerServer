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
package cocomposer.applicationListeners;

import cocomposer.security.CurrentUserInformationService;
import cocomposer.security.authentification.CoComposerMemberDetails;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

/**
 *
 * @author Remi Venant
 */
public class WSUserCompositionSubscriptionManager {

    private static final Log LOG = LogFactory.getLog(WSUserCompositionSubscriptionManager.class);

    private final ConcurrentHashMap<String, ConcurrentSkipListSet<ConnectedUserWithSession>> usersByDestination = new ConcurrentHashMap<>();

    private final SimpMessagingTemplate msgTemplate;

    private final CurrentUserInformationService userInfoSvc;

    public WSUserCompositionSubscriptionManager(SimpMessagingTemplate msgTemplate, CurrentUserInformationService userInfoSvc) {
        this.msgTemplate = msgTemplate;
        this.userInfoSvc = userInfoSvc;
    }

    @EventListener
    public void onSessionSubscribeEvent(SessionSubscribeEvent event) {
        final CoComposerMemberDetails member = this.userInfoSvc.extractMemberFromPrincipal(event.getUser());
        if (member == null) {
            LOG.warn("No member found on stomp subscription");
            return;
        }
        final StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        final String sessionId = sha.getSessionId();
        final String topicDestination = sha.getDestination();
        if (sessionId == null || topicDestination == null) {
            LOG.warn("No sessionId or destination found on stomp subscription");
            return;
        }

        final String[] destinationComponents = topicDestination.split("\\.", 2);
        if (destinationComponents.length != 2 || !destinationComponents[0].equals("/topic/compositions")) {
            return;
        }
        ConcurrentSkipListSet<ConnectedUserWithSession> compositionUsers = this.usersByDestination.compute(topicDestination, (k, users) -> {
            if (users == null) {
                users = new ConcurrentSkipListSet<>();
            }
            users.add(new ConnectedUserWithSession(sessionId, member.getUsername(), member.getEmail()));
            return users;
        });
        final UserTopicSubscriptionInfoOrder order = new UserTopicSubscriptionInfoOrder(member.getEmail(), member.getUsername(), UserTopicSubscriptionInfoType.MEMBER_JOINED);
        this.msgTemplate.convertAndSend(topicDestination, order);
        this.sendConnectedUsersToUser(member, destinationComponents[1], compositionUsers);
    }

    @Async
    private void sendConnectedUsersToUser(CoComposerMemberDetails user, String compositionId, ConcurrentSkipListSet<ConnectedUserWithSession> compositionUsers) {
        ConnectedUsersToCompositionOrder connectedUsers = new ConnectedUsersToCompositionOrder(compositionId,
                compositionUsers.parallelStream().map(ConnectedUserWithSession::toConnectedUser).toList(),
                UserTopicSubscriptionInfoType.CONNECTED_MEMBERS);
        this.msgTemplate.convertAndSendToUser(user.getUsername(), "/queue/compositions", connectedUsers);
    }

    private String removeUserSessionAndGetDestination(String username, String sessionId) {
        final ConnectedUserWithSession userComparator = new ConnectedUserWithSession(sessionId, username, null);
        final Iterator<Entry<String, ConcurrentSkipListSet<ConnectedUserWithSession>>> mainIt
                = this.usersByDestination.entrySet().iterator();
        while (mainIt.hasNext()) {
            final Entry<String, ConcurrentSkipListSet<ConnectedUserWithSession>> usersBySessionId = mainIt.next();
            if (usersBySessionId.getValue().remove(userComparator)) {
                if (usersBySessionId.getValue().isEmpty()) {
                    mainIt.remove();
                }
                return usersBySessionId.getKey();
            }
        }
        return null;
    }

    private List<String> removeUserAndGetDestinations(String username) {
        final ArrayList<String> destinations = new ArrayList<>();
        final Iterator<Entry<String, ConcurrentSkipListSet<ConnectedUserWithSession>>> mainIt
                = this.usersByDestination.entrySet().iterator();
        while (mainIt.hasNext()) {
            final Entry<String, ConcurrentSkipListSet<ConnectedUserWithSession>> usersBySessionId = mainIt.next();
            final Iterator<ConnectedUserWithSession> sessionIt = usersBySessionId.getValue().iterator();
            boolean removed = false;
            while (!removed && sessionIt.hasNext()) {
                ConnectedUserWithSession user = sessionIt.next();
                if (user.matchId(username)) {
                    sessionIt.remove();
                    removed = true;
                }
            }
            if (removed) {
                destinations.add(usersBySessionId.getKey());
                if (usersBySessionId.getValue().isEmpty()) {
                    mainIt.remove();
                }
            }
        }
        return destinations;
    }

    @EventListener
    public void onSessionUnsubscribe(SessionUnsubscribeEvent event) {
        final CoComposerMemberDetails member = this.userInfoSvc.extractMemberFromPrincipal(event.getUser());
        if (member == null) {
            LOG.warn("No member found on stomp subscription");
            return;
        }
        final StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        final String sessionId = sha.getSessionId();
        if (sessionId == null) {
            LOG.warn("No sessionId found on stomp unsubscription");
            return;
        }
        final String destination = this.removeUserSessionAndGetDestination(member.getUsername(), sessionId);
        if (destination != null) {
            final UserTopicSubscriptionInfoOrder order = new UserTopicSubscriptionInfoOrder(member.getEmail(), member.getUsername(), UserTopicSubscriptionInfoType.MEMBER_LEFT);
            this.msgTemplate.convertAndSend(destination, order);
        }
    }

    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        final CoComposerMemberDetails member = this.userInfoSvc.extractMemberFromPrincipal(event.getUser());
        if (member == null) {
            LOG.warn("No member found on stomp subscription");
            return;
        }
        List<String> removedDestinations = this.removeUserAndGetDestinations(member.getUsername());
        if (!removedDestinations.isEmpty()) {
            final UserTopicSubscriptionInfoOrder order = new UserTopicSubscriptionInfoOrder(member.getEmail(), member.getUsername(), UserTopicSubscriptionInfoType.MEMBER_LEFT);
            removedDestinations.forEach((destination) -> {
                this.msgTemplate.convertAndSend(destination, order);
            });
        }
    }

    private static enum UserTopicSubscriptionInfoType {
        MEMBER_JOINED, MEMBER_LEFT, CONNECTED_MEMBERS
    }

    private static record UserTopicSubscriptionInfoOrder(String email, String id, UserTopicSubscriptionInfoType orderType) {

    }

    private static record ConnectedUser(String email, String id) {

    }

    private static record ConnectedUsersToCompositionOrder(String compositionId, Collection<ConnectedUser> users, UserTopicSubscriptionInfoType orderType) {

    }

    private static class ConnectedUserWithSession implements Comparable<ConnectedUserWithSession> {

        private final String sessionId;
        private final String id;
        private final String email;

        public ConnectedUserWithSession(String sessionId, String id, String email) {
            this.sessionId = sessionId;
            this.id = id;
            this.email = email;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getId() {
            return id;
        }

        public boolean matchId(String id) {
            return this.id.equals(id);
        }

        public String getEmail() {
            return email;
        }

        public ConnectedUser toConnectedUser() {
            return new ConnectedUser(email, id);
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 67 * hash + Objects.hashCode(this.sessionId);
            hash = 67 * hash + Objects.hashCode(this.id);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ConnectedUserWithSession other = (ConnectedUserWithSession) obj;
            if (!Objects.equals(this.sessionId, other.sessionId)) {
                return false;
            }
            return Objects.equals(this.id, other.id);
        }

        @Override
        public int compareTo(ConnectedUserWithSession o) {
            int res = this.id.compareTo(o.id);
            if (res == 0) {
                res = this.sessionId.compareTo(o.sessionId);
            }
            return res;
        }

    }
}
