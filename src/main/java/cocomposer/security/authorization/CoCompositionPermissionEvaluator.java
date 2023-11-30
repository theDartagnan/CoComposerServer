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

import cocomposer.model.Composition;
import cocomposer.model.CompositionRepository;
import java.io.Serializable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

/**
 *
 * @author Remi Venant
 */
public class CoCompositionPermissionEvaluator implements PermissionEvaluator {

    private static final Log LOG = LogFactory.getLog(CoCompositionPermissionEvaluator.class);

    private final CompositionRepository compoRepo;

    public CoCompositionPermissionEvaluator(CompositionRepository compoRepo) {
        this.compoRepo = compoRepo;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        try {
            if (targetDomainObject == null) {
                return false;
            }
            if (targetDomainObject instanceof Composition composition) {
                return this.hasPermission(authentication, composition.getId(), "composition", permission);
            } else {
                return false;
            }
        } catch (Exception ex) {
            LOG.error("Exception got while evaluating permission (" + ex.getClass().getName() + ") : " + ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        try {
            final String userId = this.extractUserIdFromAuthentication(authentication);
            final String perm = ((String) permission).toLowerCase();
            if (null == targetType) {
                LOG.error("Bad permission \"" + perm + "\"");
                return false;
            } else {
                switch (targetType) {
                    case "composition" -> {
                        return this.checkCompositionPermission(userId, targetId, perm);
                    }
                    default -> {
                        LOG.error("Bad permission \"" + perm + "\"");
                        return false;
                    }
                }
            }

        } catch (Exception ex) {
            LOG.error("Exception got while evaluating permission (" + ex.getClass().getName() + ") : " + ex.getMessage());
            return false;
        }
    }

    private String extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return authentication.getName();
    }

    public boolean checkCompositionPermission(String userId, Serializable targetId, String permission) {
        if (targetId == null) {
            LOG.warn("null targetId");
            return false;
        }
        final String mapId = targetId.toString();
        switch (permission) {
            case "own" -> {
                return this.compoRepo.existsByIdAndOwnerId(mapId, userId);
            }
            case "edit-personnal" -> {
                return this.compoRepo.canUserEditPersonnalCompo(mapId, userId);
            }
            case "edit-collaborative" -> {
                return this.compoRepo.canUserEditCollabCompo(mapId, userId);
            }
            default -> {
                LOG.error("Received unmanageable permission for map: \"" + permission + "\"");
                return false;
            }
        }
    }
}
