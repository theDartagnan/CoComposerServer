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
import cocomposer.model.MemberCredential;
import cocomposer.model.MemberCredentialRepository;
import cocomposer.model.MemberRepository;
import jakarta.validation.ConstraintViolationException;
import java.util.NoSuchElementException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 *
 * @author Remi Venant
 */
@Service
@Validated
public class AccountServiceImpl implements AccountService {

    private static final Log LOG = LogFactory.getLog(AccountServiceImpl.class);

    private final MemberCredentialRepository memberCredRepo;

    private final MemberRepository memberRepo;

    private final PasswordEncoder passwordEncoder;

    private final CompositionService compositionSvc;

    @Autowired
    public AccountServiceImpl(MemberCredentialRepository memberCredRepo, MemberRepository memberRepo, PasswordEncoder passwordEncoder, CompositionService compositionSvc) {
        this.memberCredRepo = memberCredRepo;
        this.memberRepo = memberRepo;
        this.passwordEncoder = passwordEncoder;
        this.compositionSvc = compositionSvc;
    }

    @Override
    public Member getAccount(String userId) throws ConstraintViolationException, NoSuchElementException {
        /*
        Precondition:
        - userId is validated
         */
        return this.memberRepo.findById(userId).get();
    }

    @Override
    public Member createAccount(Member accountInfo, String clearPassword) throws ConstraintViolationException, DuplicateKeyException {
        /*
        Preconditions:
        - accountInfo is not null
        - clearPassword is validated
         */
        // Any other field will be validated by the next component (Mongo Template)
        // Attempt password encoding
        final String encodedPassword = this.passwordEncoder.encode(clearPassword);
        // Create a proper account to ensure properties are properly managed
        Member member = new Member(accountInfo.getEmail(), accountInfo.getFirstname(), accountInfo.getLastname());
        // Create the account. May raise DuplicateKeyException or ConstraintViolationException
        member = this.memberRepo.save(member);
        // Create the member's credential (should not raise DuplicateKeyException if previous instruction passed)
        this.memberCredRepo.save(new MemberCredential(member, encodedPassword));

        return member;
    }

    @Override
    public Member updateAccount(String userId, Member accountInfo) throws ConstraintViolationException, NoSuchElementException, DuplicateKeyException {
        /*
        Preconditions:
        - userId is validated
        - accountInfo is not null
         */
        // Retrieve use current account. May throw NoSuchElementException
        Member userAccount = this.memberRepo.findById(userId).get();

        // For each non null field in accountInfo, update useAccount et indicate an update is required
        boolean updateRequired = false;
        if (accountInfo.getEmail() != null && !userAccount.getEmail().equals(accountInfo.getEmail())) {
            updateRequired = true;
            userAccount.setEmail(accountInfo.getEmail());
        }
        if (accountInfo.getFirstname() != null && !userAccount.getFirstname().equals(accountInfo.getFirstname())) {
            updateRequired = true;
            userAccount.setFirstname(accountInfo.getFirstname());
        }
        if (accountInfo.getLastname() != null && !userAccount.getLastname().equals(accountInfo.getLastname())) {
            updateRequired = true;
            userAccount.setLastname(accountInfo.getLastname());
        }

        // Attempt to save userAccount if required. May throw ConstraintViolationException or DuplicateKeyException
        if (updateRequired) {
            userAccount = this.memberRepo.save(userAccount);
        }
        // if no error occured, return userAccount as it is in database
        return userAccount;
    }

    @Override
    public void deleteAccount(String userId) throws ConstraintViolationException, NoSuchElementException {
        /*
        Preconditions:
        - userId is validated
         */
        // Retrieve use current account. May throw NoSuchElementException
        Member userAccount = this.memberRepo.findById(userId).get();
        // Attempt to delete all owned map (and disconnect current logged user on them)
        this.compositionSvc.deleteAllOwnedCompositionsFromUser(userId);
        // Delete the userCredential
        this.memberCredRepo.deleteByMember(userAccount);
        // Delete the user account
        this.memberRepo.delete(userAccount);
    }

    @Override
    public void updateAccountPassword(String userId, String currentClearPassword, String newClearPassword) throws
            ConstraintViolationException, NoSuchElementException, IllegalArgumentException, AccessDeniedException {
        /*
        Preconditions:
        - userId is validated
        - currentClearPassword is validated
        - newClearPassword is validated
         */
        // Checks passwords differ
        if (currentClearPassword.equals(newClearPassword)) {
            throw new IllegalArgumentException("Same passwords");
        }
        // Retrieve current use credential
        MemberCredential cred = this.memberCredRepo.findByMemberId(userId).get();
        // Check the current password
        if (!this.passwordEncoder.matches(currentClearPassword, cred.getEncodedPassword())) {
            throw new AccessDeniedException("Bad current password.");
        }

        // Retrieve the default password encoder and encode the new password
        cred.setEncodedPassword(this.passwordEncoder.encode(newClearPassword));

        // save the updated cred in database
        this.memberCredRepo.save(cred);
    }

}
