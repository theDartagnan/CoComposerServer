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

import cocomposer.model.Member;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.NoSuchElementException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 *
 * @author Remi Venant
 */
public interface AccountService {

    /**
     * Retrieve member instance based on its internal userId.
     *
     * @param userId the user id
     * @return the user's account
     * @throws AccessDeniedException if authorization fails
     * @throws ConstraintViolationException userId null, empty or invalid length
     * (24)
     * @throws NoSuchElementException the userId does not match any account
     */
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #userId")
    Member getAccount(
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String userId
    ) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException;

    /**
     * Create a new account
     *
     * @param accountInfo information of the member. any given id will be
     * cleared
     * @param clearPassword member's password
     * @return the created member (with its internal id, but without the
     * credential)
     * @throws AccessDeniedException if authorization fails
     * @throws ConstraintViolationException invalid accountInfo or password
     * @throws DuplicateKeyException if the email used for the acount is already
     * known
     */
    @PreAuthorize("permitAll")
    Member createAccount(
            @NotNull Member accountInfo,
            @NotBlank @Size(min = 4, max = 150) String clearPassword
    ) throws AccessDeniedException, ConstraintViolationException, DuplicateKeyException;

    /**
     * Update a user account
     *
     * @param userId the user id
     * @param accountInfo the information to update. As none of them can be
     * null, a null field indicates to not update the given field
     * @return the updated user's account
     * @throws AccessDeniedException if authorization fails
     * @throws IllegalArgumentException if userId or any field value to update
     * is invalid
     * @throws NoSuchElementException if the userId does not match any account
     * @throws DuplicateKeyException if the new email address is already used by
     * another member
     */
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #userId")
    Member updateAccount(
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String userId,
            @NotNull Member accountInfo
    ) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException, DuplicateKeyException;

    /**
     * Delete a user account. Will also delete user credential and all owned map
     * (that may disconnect current editor or viewers working on one of them).
     *
     * @param userId the user id
     * @throws AccessDeniedException if authorization fails
     * @throws AccessDeniedException if authorization failed
     * @throws IllegalArgumentException if userId is not valid
     * @throws NoSuchElementException if the userId does not match any account
     */
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #userId")
    void deleteAccount(
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String userId
    ) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException;

    /**
     * Change a user's password
     *
     * @param userId the user id
     * @param currentClearPassword the current password
     * @param newClearPassword the new password
     * @throws AccessDeniedException if authorization fails or if the given
     * current password is incorrect
     * @throws ConstraintViolationException if any param is not validated
     * @throws NoSuchElementException if the userId does not match any
     * credential
     * @throws IllegalArgumentException if the current password and the new one
     * are the same
     */
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #userId")
    void updateAccountPassword(
            @NotNull @Pattern(regexp = "[abcdef0-9]{24}", flags = Pattern.Flag.CASE_INSENSITIVE) String userId,
            @NotBlank @Size(min = 4, max = 150) String currentClearPassword,
            @NotBlank @Size(min = 4, max = 150) String newClearPassword
    ) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException, IllegalArgumentException;
}
