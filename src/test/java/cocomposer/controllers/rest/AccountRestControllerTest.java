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

import cocomposer.config.TestDatasetConfig;
import cocomposer.config.TestDatasetGenerator;
import cocomposer.controllers.rest.AccountRestController.MemberCreation;
import cocomposer.controllers.rest.AccountRestController.PasswordUpdate;
import cocomposer.model.Member;
import cocomposer.security.authentication.LoginLogoutWithCSRF;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 *
 * @author Remi Venant
 */
@ActiveProfiles({"mongo-test", "secu-logs", "secu-csrf", "no-ext-broker"})
@Import(TestDatasetConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AccountRestControllerTest {

    private static final Log LOG = LogFactory.getLog(LoginLogoutWithCSRF.class);

    private AutoCloseable mocks;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestDatasetGenerator testDataset;

    private MockMvc mvc;

    private ObjectMapper jsonMapper;

    public AccountRestControllerTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
        mocks = null;
        mocks = MockitoAnnotations.openMocks(this);
        this.testDataset.createDataset(true);
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        this.jsonMapper = new ObjectMapper();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
            mocks = null;
        }
        this.testDataset.clear();
    }

    @Test
    public void createAccountWhenValidCsrfTokenWithThenSuccess() throws Exception {
        MemberCreation memCrea = new AccountRestController.MemberCreation(new Member("user@mail.com", "firstname", "lastname"), "properPassword");
        this.mvc.perform(post("/api/v1/rest/accounts")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.jsonMapper.writeValueAsString(memCrea)))
                .andExpect(status().isOk());
    }
    
    @Test
    public void createAccountWhenInValidCsrfTokenThenFailed() throws Exception {
        MemberCreation memCrea = new AccountRestController.MemberCreation(new Member("user@mail.com", "firstname", "lastname"), "properPassword");
        this.mvc.perform(post("/api/v1/rest/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.jsonMapper.writeValueAsString(memCrea)))
                .andExpect(status().isForbidden());
    }
    
    @Test
    @WithUserDetails(value = "mem1@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void createAccountWhenValidCsrfTokenButAuthenticatedThenFailed() throws Exception {
        MemberCreation memCrea = new AccountRestController.MemberCreation(new Member("user@mail.com", "firstname", "lastname"), "properPassword");
        this.mvc.perform(post("/api/v1/rest/accounts")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.jsonMapper.writeValueAsString(memCrea)))
                .andExpect(status().isForbidden());
    }

    final static String TOO_LONG_PWD = "mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm";

    @ParameterizedTest
    @ValueSource(strings = {"", "short", AccountRestControllerTest.TOO_LONG_PWD, "bad'char"})
    public void createAccountWhenValidCsrfTokenWithViolatingPasswordThenFail(String pwd) throws Exception {
        MemberCreation memCrea = new AccountRestController.MemberCreation(new Member("user@mail.com", "firstname", "lastname"), pwd);
        this.mvc.perform(post("/api/v1/rest/accounts")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.jsonMapper.writeValueAsString(memCrea)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void changeAccountPasswordWhenValidCsrfTokenNoAuthenticatedThenFail() throws Exception {
        String validUserId = this.testDataset.getTestInstances().getMem1().getId();
        PasswordUpdate memCrea = new PasswordUpdate("pwd-mem1bad", "future-password");
        this.mvc.perform(put("/api/v1/rest/accounts/" + validUserId + "/password")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.jsonMapper.writeValueAsString(memCrea)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "mem1@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void changeAccountPasswordWhenAuthenticatedButInvalidCsrfTokenThenFail() throws Exception {
        String validUserId = this.testDataset.getTestInstances().getMem1().getId();
        PasswordUpdate memCrea = new PasswordUpdate("pwd-mem1", "future-password");
        this.mvc.perform(put("/api/v1/rest/accounts/" + validUserId + "/password")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.jsonMapper.writeValueAsString(memCrea)))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "short", AccountRestControllerTest.TOO_LONG_PWD, "bad'char"})
    @WithUserDetails(value = "mem1@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void changeAccountPasswordWhenValidCsrfTokenWithViolatingCurrentPasswordThenFail(String pwd) throws Exception {
        String validUserId = this.testDataset.getTestInstances().getAdmin().getId();
        PasswordUpdate memCrea = new PasswordUpdate(pwd, "validFuturePassword");
        this.mvc.perform(put("/api/v1/rest/accounts/" + validUserId + "/password")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.jsonMapper.writeValueAsString(memCrea)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "short", AccountRestControllerTest.TOO_LONG_PWD, "bad'char"})
    @WithUserDetails(value = "mem1@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void changeAccountPasswordWhenValidCsrfTokenWithViolatingFuturPasswordThenFail(String pwd) throws Exception {
        String validUserId = this.testDataset.getTestInstances().getAdmin().getId();
        PasswordUpdate memCrea = new PasswordUpdate("validFuturePassword", pwd);
        this.mvc.perform(put("/api/v1/rest/accounts/" + validUserId + "/password")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.jsonMapper.writeValueAsString(memCrea)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = "mem1@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void changeAccountPasswordWhenValidCsrfTokenWithBadUserIdThenFail() throws Exception {
        String validUserId = this.testDataset.getTestInstances().getAdmin().getId();
        PasswordUpdate memCrea = new PasswordUpdate("pwd-admin", "future-password");
        this.mvc.perform(put("/api/v1/rest/accounts/" + validUserId + "/password")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.jsonMapper.writeValueAsString(memCrea)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = "mem1@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void changeAccountPasswordWhenValidCsrfTokenWithBadCurrentPasswordThenFail() throws Exception {
        String validUserId = this.testDataset.getTestInstances().getMem1().getId();
        PasswordUpdate memCrea = new PasswordUpdate("pwd-mem1bad", "future-password");
        this.mvc.perform(put("/api/v1/rest/accounts/" + validUserId + "/password")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.jsonMapper.writeValueAsString(memCrea)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = "mem1@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void changeAccountPasswordWhenValidRequirementsThenSuccess() throws Exception {
        String validUserId = this.testDataset.getTestInstances().getMem1().getId();
        PasswordUpdate memCrea = new PasswordUpdate("pwd-mem1", "future-password");
        this.mvc.perform(put("/api/v1/rest/accounts/" + validUserId + "/password")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.jsonMapper.writeValueAsString(memCrea)))
                .andExpect(status().isNoContent());
    }
}
