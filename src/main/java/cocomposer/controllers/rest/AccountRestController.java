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
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    private final Validator validator;

    @Autowired
    public AccountRestController(AccountService accountService, Validator validator) {
        this.accountSvc = accountService;
        this.validator = validator;
    }

    @GetMapping("myself")
    @JsonView(MemberViews.Administrative.class)
    public ResponseEntity<Member> getMyself(@AuthenticationPrincipal CoComposerMemberDetails currentUser) {
        if (currentUser == null) {
            return ResponseEntity.noContent().build();
        }
        Member member = this.accountSvc.getAccount(currentUser.getUsername());
        return ResponseEntity.ok(member);
    }

    @PostMapping
    @JsonView(MemberViews.Administrative.class)
    public Member createAccount(@RequestBody MemberCreation memberCreationInfo) {
        if (memberCreationInfo == null
                || memberCreationInfo.memberInfo() == null
                || memberCreationInfo.password() == null) {
            throw new IllegalArgumentException("Missing creation data");
        }
        if (!this.validator.validateValue(PasswordUpdate.class, "currentPassword", memberCreationInfo.password).isEmpty()) {
            throw new IllegalArgumentException("Incorrect update password data");
        }
        return this.accountSvc.createAccount(memberCreationInfo.memberInfo(), memberCreationInfo.password());
    }

    @PatchMapping("{userId:[abcdef0-9]{24}}")
    @JsonView(MemberViews.Administrative.class)
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
    public ResponseEntity deleteAccount(@PathVariable String userId) {
        this.accountSvc.deleteAccount(userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("{userId:[abcdef0-9]{24}}/password")
    public ResponseEntity changeAccountPassword(@PathVariable String userId, @RequestBody PasswordUpdate passwordUpdate) {
        if (passwordUpdate == null || !this.validator.validate(passwordUpdate).isEmpty()) {
            throw new IllegalArgumentException("Incorrect update password data");
        }
        this.accountSvc.updateAccountPassword(userId,
                passwordUpdate.getCurrentPassword(),
                passwordUpdate.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    public static record MemberCreation(Member memberInfo, String password) {

    }

    public static class PasswordUpdate {

        @NotBlank
        @Pattern(regexp = "[\\w%:;<>\\.\\*\\#\\$\\?\\+\\-]{8,100}", flags = Pattern.Flag.CASE_INSENSITIVE)
        private String currentPassword;

        @NotBlank
        @Pattern(regexp = "[\\w%:;<>\\.\\*\\#\\$\\?\\+\\-]{8,100}", flags = Pattern.Flag.CASE_INSENSITIVE)
        private String newPassword;

        public PasswordUpdate() {
        }

        public PasswordUpdate(String currentPassword, String newPassword) {
            this.currentPassword = currentPassword;
            this.newPassword = newPassword;
        }

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }

    }
}
