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
package cocomposer.controllers.websocket;

import cocomposer.model.compositionOrder.CompositionCollaborativeChangedOrder;
import cocomposer.model.compositionOrder.CompositionOrder;
import cocomposer.model.compositionOrder.CompositionTitleChangedOrder;
import cocomposer.model.compositionOrder.ElementAddedOrder;
import cocomposer.model.compositionOrder.ElementChangedOrder;
import cocomposer.model.compositionOrder.ElementDeletedOrder;
import cocomposer.model.compositionOrder.ElementPositionChangedOrder;
import cocomposer.model.CompositionElement;
import cocomposer.security.CurrentUserInformationService;
import cocomposer.security.authentification.CoComposerMemberDetails;
import cocomposer.services.CompositionElementService;
import cocomposer.services.CompositionService;
import java.security.Principal;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

/**
 *
 * @author Remi Venant
 */
@Controller
@MessageMapping("compositions")
public class CompositionWebsocketController {

    private static final Log LOG = LogFactory.getLog(CompositionWebsocketController.class);

    private final CompositionService compositionSvc;

    private final CompositionElementService compositionElementSvc;

    private final CurrentUserInformationService userInfoSvc;

    @Autowired
    public CompositionWebsocketController(CompositionService compositionSvc, CompositionElementService compositionElementSvc, CurrentUserInformationService userInfoSvc) {
        this.compositionSvc = compositionSvc;
        this.compositionElementSvc = compositionElementSvc;
        this.userInfoSvc = userInfoSvc;
    }

    @MessageMapping("{compoId:[abcdef0-9]{24}}")
    public CompositionOrder handlerCompositionOrder(
            @DestinationVariable("compoId") String compoId,
            @Payload CompositionOrder order, Principal currentUser) {

        final CoComposerMemberDetails author = this.userInfoSvc.extractMemberFromPrincipal(currentUser);
        if (author == null) {
            throw new AccessDeniedException("No Member. Proper Authentication required");
        }

        // Set / override author and composition id info
        order.setCompositionId(compoId);
        order.setAuthorEmail(author.getEmail());

        return switch (order.getOrderType()) {
            case "compositiontitleChanged" ->
                this.handleCompositionTitleChanged((CompositionTitleChangedOrder) order);
            case "compositionCollaborativeChanged" ->
                this.handleCompositionCollaborativeChange((CompositionCollaborativeChangedOrder) order);
            case "elementAdded" ->
                this.handleElementAdded((ElementAddedOrder) order);
            case "elementChanged" ->
                this.handleElementChangedOrder((ElementChangedOrder) order);
            case "elementPositionChanged" ->
                this.handleElementPositionChangedOrder((ElementPositionChangedOrder) order);
            case "elementDeleted" ->
                this.handleElementDeletedOrder((ElementDeletedOrder) order);
            default ->
                throw new IllegalArgumentException("Wrong order type");
        };
    }

    private CompositionTitleChangedOrder handleCompositionTitleChanged(CompositionTitleChangedOrder order) {
        this.compositionSvc.updateCompositionTitleCollaborative(order.getCompositionId(), order.getTitle());
        return order;
    }

    private CompositionCollaborativeChangedOrder handleCompositionCollaborativeChange(CompositionCollaborativeChangedOrder order) {
        this.compositionSvc.updateCompositionCollaborativeCollaborative(order.getCompositionId(), order.isCollaborative());
        return order;
    }

    private ElementAddedOrder handleElementAdded(ElementAddedOrder order) {
        CompositionElement element = this.compositionElementSvc.addElementCollaborative(order.getCompositionId(), order.getElement());
        order.setElement(element);
        return order;
    }

    private ElementChangedOrder handleElementChangedOrder(ElementChangedOrder order) {
        CompositionElement element = this.compositionElementSvc.updateElementCollaborative(order.getCompositionId(), order.getElement());
        order.setElement(element);
        return order;
    }

    private ElementDeletedOrder handleElementDeletedOrder(ElementDeletedOrder order) {
        this.compositionElementSvc.deleteElementCollaborative(order.getCompositionId(), order.getElementId());
        return order;
    }

    private ElementPositionChangedOrder handleElementPositionChangedOrder(ElementPositionChangedOrder order) {
        this.compositionElementSvc.updateElementPositionCollaborative(
                order.getCompositionId(), order.getElementId(), order.getX(), order.getY());
        return order;
    }
}
