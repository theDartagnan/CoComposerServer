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
package cocomposer.controllers.rest;

import cocomposer.model.Member;
import cocomposer.model.views.MemberViews;
import cocomposer.security.authentification.CoComposerMemberDetails;
import cocomposer.services.AccountService;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Remi Venant
 */
@RestController
@RequestMapping("/api/v1/rest/accounts")
public class AccountRestController {

    private final AccountService accountSvc;

    @Autowired
    public AccountRestController(AccountService accountService) {
        this.accountSvc = accountService;
    }

    @GetMapping("myself")
    @JsonView(MemberViews.Normal.class)
    public Member getMyself(@AuthenticationPrincipal CoComposerMemberDetails currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return this.accountSvc.getAccount(currentUser.getUsername());
    }

    @PostMapping
    @JsonView(MemberViews.Normal.class)
    public Member createAccount(@RequestBody MemberCreation memberCreationInfo) {
        if (memberCreationInfo == null
                || memberCreationInfo.memberInfo() == null
                || memberCreationInfo.password() == null) {
            throw new IllegalArgumentException("Missing creation data");
        }
        return this.accountSvc.createAccount(memberCreationInfo.memberInfo(), memberCreationInfo.password());
    }

    @PatchMapping("{userId:[abcdef0-9]{24}}")
    @JsonView(MemberViews.Normal.class)
    public Member patchAccount(@PathVariable String userId, @RequestBody Member memberInfo) {
        if (memberInfo == null) {
            throw new IllegalArgumentException("Missing update data");
        }
        if (memberInfo.getId() != null && !memberInfo.getId().equals(userId)) {
            throw new IllegalArgumentException("Inconsistant update data");
        }
        return this.accountSvc.updateAccount(userId, memberInfo);
    }

    @DeleteMapping("{userId:[abcdef0-9]{24}}")
    public void deleteAccount(@PathVariable String userId) {
        this.accountSvc.deleteAccount(userId);
    }

    @PutMapping("{userId:[abcdef0-9]{24}}/password")
    public void changeAccountPassword(@PathVariable String userId, @RequestBody PasswordUpdate passwordUpdate) {
        if (passwordUpdate == null
                || passwordUpdate.currentPassword() == null
                || passwordUpdate.newPassword() == null) {
            throw new IllegalArgumentException("Missing update password data");
        }
        this.accountSvc.updateAccountPassword(userId,
                passwordUpdate.currentPassword(),
                passwordUpdate.newPassword());
    }

    public static record MemberCreation(Member memberInfo, String password) {

    }

    public static record PasswordUpdate(String currentPassword, String newPassword) {

    }
}
