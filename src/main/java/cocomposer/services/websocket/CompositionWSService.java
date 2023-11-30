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

/**
 *
 * @author Remi Venant
 */
public interface CompositionWSService {

    /*
    Inform all used that might be connected to the topic of composition it has been deleted
     */
    /**
     * Inform all guests through their dedicated queue the composition will be
     * removed
     *
     * @param composition the composition that will be deleted
     */
    void informCompositionWillBeDeleted(Composition composition);

    /**
     * Inform all user that a collaborative composition changed. If the
     * collaborative goes from true to false, inform editing users through the
     * specific composition topic. If the collaborative goes from false to true,
     * inform connected users through their general composition queue.
     *
     * @param compoId the composition id
     * @param collaborative the new value of collaborative
     */
    void informCompositionCollaborativeChanged(String compoId, boolean collaborative);
}
