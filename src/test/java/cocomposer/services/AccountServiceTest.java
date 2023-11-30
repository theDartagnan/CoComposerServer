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

import cocomposer.config.TestDatasetConfig;
import cocomposer.config.TestDatasetGenerator;
import cocomposer.model.Member;
import jakarta.validation.ConstraintViolationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

/**
 *
 * @author Remi Venant
 */
@ActiveProfiles({"mongo-test", "no-ext-broker"})
@Import(TestDatasetConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AccountServiceTest {

    private static final Log LOG = LogFactory.getLog(AccountServiceTest.class);

    @Autowired
    private TestDatasetGenerator testDataset;

    @Autowired
    private AccountService testedService;

    public AccountServiceTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
        this.testDataset.createOnlyMembersDataset();
    }

    @AfterEach
    public void tearDown() {
        this.testDataset.clear();
    }

    @Test
    public void testGetAccountIsSecured() {
        assertThatThrownBy(()
                -> this.testedService.getAccount(this.testDataset.getTestInstances().getCredMem1().getId()))
                .as("no authentication is rejected")
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @WithMockUser(username = "anAdmin", roles = {"ADMIN", "USER"})
    //@WithUserDetails(value = "admin@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testGetAccountParameterValidation() {
        assertThatThrownBy(()
                -> this.testedService.getAccount(null))
                .as("null account id is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.testedService.getAccount(""))
                .as("empty account id is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.testedService.getAccount("a".repeat(23)))
                .as("wrong size account id is rejected (1)")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.testedService.getAccount("a".repeat(25)))
                .as("wrong size account id is rejected (2)")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.testedService.getAccount("aaaaaaaaaaGaaaaaaaaaab"))
                .as("bad character in account id is rejected")
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testCreateAccountParameterValidation() {
        // Testing memberInfo

        // memberInfo itsetl
        assertThatThrownBy(()
                -> this.testedService.createAccount(null, "aValidPassword"))
                .as("null memberInfo is rejected")
                .isInstanceOf(ConstraintViolationException.class);

        // id
        // email
        assertThatThrownBy(()
                -> this.testedService.createAccount(new Member(null, "firstname", "lastname"), "aValidPassword"))
                .as("null email is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.testedService.createAccount(new Member("   ", "firstname", "lastname"), "aValidPassword"))
                .as("blank email is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.testedService.createAccount(new Member("a".repeat(100) + "@mail.com", "firstname", "lastname"), "aValidPassword"))
                .as("too long email is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.testedService.createAccount(new Member("bad email@mail.com", "firstname", "lastname"), "aValidPassword"))
                .as("wrong email is rejected")
                .isInstanceOf(ConstraintViolationException.class);

        // firstname
        assertThatThrownBy(()
                -> this.testedService.createAccount(new Member("email@mail.com", null, "lastname"), "aValidPassword"))
                .as("null firstname is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.testedService.createAccount(new Member("email@mail.com", "   ", "lastname"), "aValidPassword"))
                .as("blank firstname is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.testedService.createAccount(new Member("email@mail.com", "a".repeat(101), "lastname"), "aValidPassword"))
                .as("too long firstname is rejected")
                .isInstanceOf(ConstraintViolationException.class);

        // lastname
        assertThatThrownBy(()
                -> this.testedService.createAccount(new Member("email@mail.com", "firstname", null), "aValidPassword"))
                .as("null lastname is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.testedService.createAccount(new Member("email@mail.com", "firstname", "    "), "aValidPassword"))
                .as("blank lastname is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.testedService.createAccount(new Member("email@mail.com", "firstname", "a".repeat(101)), "aValidPassword"))
                .as("too long lastname is rejected")
                .isInstanceOf(ConstraintViolationException.class);

        // Testing password
        assertThatThrownBy(()
                -> this.testedService.createAccount(new Member("email@mail.com", "firstname", "lastname"), null))
                .as("null password is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.testedService.createAccount(new Member("email@mail.com", "firstname", "lastname"), "123"))
                .as("too small password is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.testedService.createAccount(new Member("email@mail.com", "firstname", "lastname"), "a".repeat(151)))
                .as("too long password is rejected")
                .isInstanceOf(ConstraintViolationException.class);
    }
}
