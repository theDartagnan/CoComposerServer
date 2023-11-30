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
package cocomposer.security;

import cocomposer.security.authentification.CoComposerMemberDetails;
import java.security.Principal;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 *
 * @author Remi Venant
 */
@Service
public class CurrentUserInformationServiceImpl implements CurrentUserInformationService {

    @Override
    public CoComposerMemberDetails getUserDetails() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(CoComposerMemberDetails.class::isInstance)
                .map(CoComposerMemberDetails.class::cast)
                .orElse(null);
    }

    @Override
    public String getUsername() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .orElse(null);
    }

    @Override
    public CoComposerMemberDetails extractMemberFromPrincipal(Principal principal) {
        if (principal == null) {
            return null;
        }
        if (principal instanceof CoComposerMemberDetails member) {
            return member;
        } else if (principal instanceof UsernamePasswordAuthenticationToken uat) {
            if (uat.getPrincipal() instanceof CoComposerMemberDetails member) {
                return member;
            }
        }
        return null;
    }

}
