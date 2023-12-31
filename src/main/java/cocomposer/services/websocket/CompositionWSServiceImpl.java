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
package cocomposer.services.websocket;

import cocomposer.model.Composition;
import cocomposer.model.CompositionRepository;
import cocomposer.model.Member;
import cocomposer.model.compositionOrder.CompositionCollaborativeChangedOrder;
import cocomposer.model.compositionOrder.CompositionDeletedOrder;
import cocomposer.security.CurrentUserInformationService;
import cocomposer.security.authentification.CoComposerMemberDetails;
import java.time.LocalDateTime;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 *
 * @author Remi Venant
 */
@Service
public class CompositionWSServiceImpl implements CompositionWSService {

    private static final Log LOG = LogFactory.getLog(CompositionWSServiceImpl.class);

    private final SimpMessagingTemplate msgTemplate;

    private final CurrentUserInformationService currentUserInformationSvc;

    private final CompositionRepository compositionRepo;

    public CompositionWSServiceImpl(SimpMessagingTemplate msgTemplate,
            CurrentUserInformationService currentUserInformationSvc, CompositionRepository compositionRepo) {
        this.msgTemplate = msgTemplate;
        this.currentUserInformationSvc = currentUserInformationSvc;
        this.compositionRepo = compositionRepo;
    }

    @Async
    @Override
    public void informCompositionWillBeDeleted(Composition composition) {
        CoComposerMemberDetails author = this.currentUserInformationSvc.getUserDetails();
        String authorEmail = null;
        if (author == null) {
            LOG.warn("Current user is null. Cannot set author in CompositionDeletedOrder.");
        } else {
            authorEmail = author.getEmail();
        }
        final String compoId = composition.getId();
        final String wsQueueEndpoint = "/queue/compositions";

        final CompositionDeletedOrder order = new CompositionDeletedOrder();
        order.setCompositionId(compoId);
        order.setAuthorEmail(authorEmail);
        composition.getGuests().forEach((Member guest) -> {
            try {
                this.msgTemplate.convertAndSendToUser(guest.getId(), wsQueueEndpoint, order);
            } catch (MessagingException ex) {
                LOG.warn("Unable to send composition deleted order to user " + guest.getEmail() + ": " + ex.getMessage());
            }
        });

    }

    @Async
    @Override
    public void informCompositionCollaborativeChanged(String compoId, boolean collaborative) {
        CoComposerMemberDetails author = this.currentUserInformationSvc.getUserDetails();
        String authorEmail = null;
        if (author == null) {
            LOG.warn("Current user is null. Cannot set author in CompositionDeletedOrder.");
        } else {
            authorEmail = author.getEmail();
        }
        
        final CompositionCollaborativeChangedOrder order = new CompositionCollaborativeChangedOrder();
        order.setCompositionId(compoId);
        order.setAuthorEmail(authorEmail);
        order.setCollaborative(collaborative);
        order.setOrderDatetime(LocalDateTime.now());

        if (collaborative) {
            final String wsQueueEndpoint = "/queue/compositions";
            this.compositionRepo.findById(compoId).ifPresent((composition) -> {
                composition.getGuests().forEach((Member guest) -> {
                    try {
                        this.msgTemplate.convertAndSendToUser(guest.getId(), wsQueueEndpoint, order);
                    } catch (MessagingException ex) {
                        LOG.warn("Unable to send composition collaboartive change order to user " + guest.getEmail() + ": " + ex.getMessage());
                    }
                });
            });
        } else {
            final String wsTopicEndpoint = String.format("/topic/compositions.%s", compoId);
            this.msgTemplate.convertAndSend(wsTopicEndpoint, order);
        }
    }

}
