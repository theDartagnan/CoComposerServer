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
package cocomposer.security.authentication;

import cocomposer.config.TestDatasetConfig;
import cocomposer.config.TestDatasetGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 *
 * @author Remi Venant
 */
@ActiveProfiles({"mongo-test", "secu-logs", "secu-csrf", "no-ext-broker"})
@Import(TestDatasetConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LoginLogoutWithCSRF {

    private static final Log LOG = LogFactory.getLog(LoginLogoutWithCSRF.class);

    private AutoCloseable mocks;

    @Value("${server.servlet.session.cookie.name:JSESSIONID}")
    private String sessionCookieName;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestDatasetGenerator testDataset;

    private MockMvc mvc;

    private ObjectMapper jsonMapper;

    public LoginLogoutWithCSRF() {
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
    public void loginWhenNoCsrfTokenThenFail() throws Exception {
        final Map authentication = Map.of("username", "mem1@collamap.com", "password", "pwd-mem1");
        this.mvc.perform(post("/api/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.jsonMapper.writeValueAsString(authentication)))
                .andExpect(status().isForbidden())
                .andExpect(cookie().doesNotExist(sessionCookieName));
    }
    
    @Test
    public void loginWhenInvalidCredentialAndValidCsrfTokenThenFail() throws Exception {
        final Map authentication = Map.of("username", "mem1@collamap.com", "password", "pwd-mem2");
        this.mvc.perform(post("/api/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.jsonMapper.writeValueAsString(authentication)))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist(sessionCookieName));
    }
    
    @Test
    @WithUserDetails(value = "mem1@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void loginWhenValidCredentialAndValidCsrfTokenButAlreadyAuthenticatedThenFail() throws Exception {
        final Map authentication = Map.of("username", "mem2@collamap.com", "password", "pwd-mem2");
        this.mvc.perform(post("/api/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.jsonMapper.writeValueAsString(authentication)))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/rest/accounts/myself"));
    }

    @Test
    public void loginWhenValidCsrfTokenThenSuccess() throws Exception {
        final Map authentication = Map.of("username", "mem1@collamap.com", "password", "pwd-mem1");
        this.mvc.perform(post("/api/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.jsonMapper.writeValueAsString(authentication)))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/rest/accounts/myself"));
        // On ne peut pas tester si un cookie de session est positionné ici car le mock ne passe pas par la chaine de Spring security
    }

    @Test
    @WithUserDetails(value = "mem1@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void logoutWhenValidCsrfTokenThenSuccess() throws Exception {
        this.mvc.perform(post("/api/logout")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists(sessionCookieName))
                .andExpect(cookie().maxAge(sessionCookieName, 0));
    }
    
    final static String TOO_LONG_MAIL =  "mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm" + "@mail.com";
    @ParameterizedTest
    @ValueSource(strings = {"", LoginLogoutWithCSRF.TOO_LONG_MAIL, "nomail"})
    public void loginWhenValidCsrfTokenWithViolatingUsernameThenFail(String badMail) throws Exception {
        Map authentication = Map.of("username", badMail, "password", "pwd-mem1");
        this.mvc.perform(post("/api/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.jsonMapper.writeValueAsString(authentication)))
                .andExpect(status().isBadRequest())
                .andExpect(cookie().doesNotExist(sessionCookieName));
    }
    
    final static String TOO_LONG_PWD =  "mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm";
    @ParameterizedTest
    @ValueSource(strings = {"", "short", LoginLogoutWithCSRF.TOO_LONG_PWD, "bad'char"})
    public void loginWhenValidCsrfTokenWithViolatingPasswordThenFail(String badPass) throws Exception {
        Map authentication = Map.of("username", "user@mail.com", "password", badPass);
        this.mvc.perform(post("/api/login")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.jsonMapper.writeValueAsString(authentication)))
                .andExpect(status().isBadRequest())
                .andExpect(cookie().doesNotExist(sessionCookieName));
    }
}
