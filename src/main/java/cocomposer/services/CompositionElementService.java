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

import cocomposer.model.CompositionElement;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.NoSuchElementException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 *
 * @author Remi Venant
 */
public interface CompositionElementService {

    /**
     * Add a new composition element to a composition
     *
     * @param compoId the composition id
     * @param elementInfo the composition element info
     * @return the created composition element
     * @throws AccessDeniedException if not authorized
     * @throws ConstraintViolationException if invalid parameter
     * @thorws DuplicateKeyException if element already present
     * @throws NoSuchElementException if the composition does not exist
     */
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and hasPermission(#compoId, 'composition', 'edit-personnal'))")
    CompositionElement addElementPersonnal(
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String compoId,
            @NotNull CompositionElement elementInfo
    ) throws AccessDeniedException, ConstraintViolationException, DuplicateKeyException, NoSuchElementException;

    /**
     * Add a new composition element to a composition
     *
     * @param compoId the composition id
     * @param elementInfo the composition element info
     * @return the created composition element
     * @throws AccessDeniedException if not authorized
     * @throws ConstraintViolationException if invalid parameter
     * @thorws DuplicateKeyException if element already present
     * @throws NoSuchElementException if the composition does not exist
     */
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and hasPermission(#compoId, 'composition', 'edit-collaborative'))")
    CompositionElement addElementCollaborative(
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String compoId,
            @NotNull CompositionElement elementInfo
    ) throws AccessDeniedException, ConstraintViolationException, DuplicateKeyException, NoSuchElementException;

    /**
     * Update a composition element.
     *
     * @param compoId the composition id
     * @param elementInfo the composition element to update
     * @return the updated composition element
     * @throws AccessDeniedException if not authorized
     * @throws ConstraintViolationException if invalid parameter
     * @throws NoSuchElementException if the composition or the composition
     * element does not exist
     */
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and hasPermission(#compoId, 'composition', 'edit-personnal'))")
    CompositionElement updateElementPersonnal(
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String compoId,
            @NotNull CompositionElement elementInfo
    ) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException;

    /**
     * Update a composition element.
     *
     * @param compoId the composition id
     * @param elementInfo the composition element to update
     * @return the updated composition element
     * @throws AccessDeniedException if not authorized
     * @throws ConstraintViolationException if invalid parameter
     * @throws NoSuchElementException if the composition or the composition
     * element does not exist
     */
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and hasPermission(#compoId, 'composition', 'edit-collaborative'))")
    CompositionElement updateElementCollaborative(
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String compoId,
            @NotNull CompositionElement elementInfo
    ) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException;

    /**
     * Update a composition element position.
     *
     * @param compoId the composition id
     * @param elementId the composition element id
     * @param x the abscissa
     * @param y the ordinate
     * @throws AccessDeniedException if not authorized
     * @throws ConstraintViolationException if invalid parameter
     * @throws NoSuchElementException if the composition or the composition
     * element does not exist
     */
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and hasPermission(#compoId, 'composition', 'edit-personnal'))")
    void updateElementPositionPersonnal(
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String compoId,
            @NotNull @Pattern(regexp = "[\\-\\w\\#]+", flags = Pattern.Flag.CASE_INSENSITIVE) String elementId,
            double x, double y
    ) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException;

    /**
     * Update a composition element position.
     *
     * @param compoId the composition id
     * @param elementId the composition element id
     * @param x the abscissa
     * @param y the ordinate
     * @throws AccessDeniedException if not authorized
     * @throws ConstraintViolationException if invalid parameter
     * @throws NoSuchElementException if the composition or the composition
     * element does not exist
     */
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and hasPermission(#compoId, 'composition', 'edit-collaborative'))")
    void updateElementPositionCollaborative(
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String compoId,
            @NotNull @Pattern(regexp = "[\\-\\w\\#]+", flags = Pattern.Flag.CASE_INSENSITIVE) String elementId,
            double x, double y
    ) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException;

    /**
     * Delete a composition element.
     *
     * @param compoId the composition id
     * @param elementId the composition element id
     * @throws AccessDeniedException if not authorized
     * @throws ConstraintViolationException if invalid parameter
     * @throws NoSuchElementException if the composition or the composition
     * element does not exist
     */
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and hasPermission(#compoId, 'composition', 'edit-personnal'))")
    void deleteElementPersonnal(
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String compoId,
            @NotNull @Pattern(regexp = "[\\-\\w\\#]+", flags = Pattern.Flag.CASE_INSENSITIVE) String elementId
    ) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException;

    /**
     * Delete a composition element.
     *
     * @param compoId the composition id
     * @param elementId the composition element id
     * @throws AccessDeniedException if not authorized
     * @throws ConstraintViolationException if invalid parameter
     * @throws NoSuchElementException if the composition or the composition
     * element does not exist
     */
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and hasPermission(#compoId, 'composition', 'edit-collaborative'))")
    void deleteElementCollaborative(
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String compoId,
            @NotNull @Pattern(regexp = "[\\-\\w\\#]+", flags = Pattern.Flag.CASE_INSENSITIVE) String elementId
    ) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException;
}
