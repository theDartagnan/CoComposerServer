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
package cocomposer.services;

import cocomposer.model.Composition;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.NoSuchElementException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 *
 * @author Remi Venant
 */
public interface CompositionService {

    /**
     * Get users' owned composition resume, compositions whom he is an editor.
     * This service will not check for user account existence.
     *
     * @param userId
     * @return
     * @throws AccessDeniedException if not authorized
     * @throws ConstraintViolationException if invalid parameter
     */
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and authentication.name == #userId)")
    MemberCompositionSummariesCollection getUserCompositions(
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String userId
    ) throws AccessDeniedException, ConstraintViolationException;

    /**
     * Get a composition by its id. If the user is not the owner, s.he will be
     * automatically added as a guest
     *
     * @param compoId the map id
     * @param userId
     * @return the map
     * @throws AccessDeniedException if not authorized
     * @throws ConstraintViolationException if invalid parameter
     * @throws NoSuchElementException if the map does not exist or is not
     * available for the user
     */
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and authentication.name == #userId)")
    Composition getComposition(
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String compoId,
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String userId
    ) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException;

    /**
     * Create a Composition for a given user This service will check for user
     * account existence to ensure consistency.
     *
     * @param ownerUserId the owner user id
     * @param compositionInfo the composition info
     * @return
     * @throws AccessDeniedException if not authorized
     * @throws ConstraintViolationException if invalid parameter
     * @throws NoSuchElementException if the user does not exist
     */
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and authentication.name == #ownerUserId)")
    Composition createComposition(
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String ownerUserId,
            @NotNull Composition compositionInfo
    ) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException;

    /**
     * Update personnal composition title.
     *
     * @param compoId the composition id
     * @param newTitle the new tittle
     * @return the updated title
     * @throws AccessDeniedException if not authorized, including if the
     * ownerUserId is not the owner of the composition
     * @throws ConstraintViolationException if parameter invalid
     * @throws NoSuchElementException if composition does not exist
     */
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and hasPermission(#compoId, 'composition', 'own') and hasPermission(#compoId, 'composition', 'edit-personnal'))")
    String updateCompositionTitlePersonnal(
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String compoId,
            @NotNull String newTitle
    ) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException;

    /**
     * Update collaborative composition title.
     *
     * @param compoId the composition id
     * @param newTitle the new tittle
     * @return the updated title
     * @throws AccessDeniedException if not authorized, including if the
     * ownerUserId is not the owner of the composition
     * @throws ConstraintViolationException if parameter invalid
     * @throws NoSuchElementException if composition does not exist
     */
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and hasPermission(#compoId, 'composition', 'own') and hasPermission(#compoId, 'composition', 'edit-collaborative'))")
    String updateCompositionTitleCollaborative(
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String compoId,
            @NotNull String newTitle
    ) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException;

    /**
     * Update composition collaborative indicator.
     *
     * @param compoId the composition id
     * @param collaborative the collaborative indicator
     * @return the updated collaborative indicator
     * @throws AccessDeniedException if not authorized, including if the
     * ownerUserId is not the owner of the composition
     * @throws ConstraintViolationException if parameter invalid
     * @throws NoSuchElementException if composition does not exist
     */
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and hasPermission(#compoId, 'composition', 'own') and hasPermission(#compoId, 'composition', 'edit-personnal'))")
    boolean updateCompositionCollaborativePersonnal(
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String compoId,
            @NotNull boolean collaborative
    ) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException;

    /**
     * Update composition collaborative indicator.
     *
     * @param compoId the composition id
     * @param collaborative the collaborative indicator
     * @return the updated collaborative indicator
     * @throws AccessDeniedException if not authorized, including if the
     * ownerUserId is not the owner of the composition
     * @throws ConstraintViolationException if parameter invalid
     * @throws NoSuchElementException if composition does not exist
     */
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and hasPermission(#compoId, 'composition', 'own') and hasPermission(#compoId, 'composition', 'edit-collaborative'))")
    boolean updateCompositionCollaborativeCollaborative(
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String compoId,
            @NotNull boolean collaborative
    ) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException;

    /**
     * Delete a composition. Will inform online users that are actively working
     * on this map of this deletion.
     *
     * @param compoId the map id
     * @throws AccessDeniedException if not authorized, including if the
     * ownerUserId is not the owner of the composition
     * @throws ConstraintViolationException if parameter invalid
     * @throws NoSuchElementException f map does not exist
     */
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and hasPermission(#compoId, 'composition', 'own'))")
    void deleteComposition(
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String compoId
    ) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException;

    /**
     * Delete all compositions a user owns. Will inform online users that are
     * actively working on one of these maps of this deletion. This service will
     * not check for user account existence.
     *
     * @param userId the user id
     * @return the number of compositions deleted
     * @throws AccessDeniedException if not authorized
     * @throws ConstraintViolationException if parameter invalid
     */
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and authentication.name == #userId)")
    long deleteAllOwnedCompositionsFromUser(
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String userId
    ) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException;
}
