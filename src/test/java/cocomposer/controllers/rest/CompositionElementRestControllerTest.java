/*
 * Copyright (C) 2024 IUT Laval - Le Mans Université.
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
import cocomposer.model.CompositionElement;
import cocomposer.services.CompositionElementServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 *
 * @author Remi Venant
 */
@ActiveProfiles({"mongo-test", "secu-logs", "no-ext-broker"})
@Import(TestDatasetConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CompositionElementRestControllerTest {

    private static final Log LOG = LogFactory.getLog(CompositionElementRestControllerTest.class);

    private AutoCloseable mocks;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestDatasetGenerator testDataset;

    @MockBean
    private CompositionElementServiceImpl compositionElementSvc;

    private MockMvc mvc;

    public CompositionElementRestControllerTest() {
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
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
            mocks = null;
        }
        this.testDataset.clear();
    }

    /**
     * Test of updateElement method, of class CompositionRestController.
     */
    @Test
    @WithUserDetails(value = "mem1@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testUpdateElement() throws Exception {
        String compoId = this.testDataset.getTestInstances().getCompMem1_1().getId();
        CompositionElement element = this.testDataset.getTestInstances().getCompMem1_1().getElements().get(0);
        String elementId = element.getId();

        LOG.info("CompoId: " + compoId + " ElemId: " + elementId);

        CompositionElement updatedElement = new CompositionElement(
                elementId, element.getElementType(), "{color: red}", 20, 20);
        updatedElement.setExtraProperties(Map.of("width", 20, "height", 20));

        ObjectMapper om = new ObjectMapper();
        String updatedElementRep = om.writeValueAsString(updatedElement);

        LOG.info("Element to update: " + System.lineSeparator() + updatedElementRep);

        String elemUri = String.format("/api/v1/rest/compositions/%s/elements/%s", compoId, elementId);
        mvc.perform(put(elemUri)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedElementRep)
        )
                //                .andDo((result) -> {
                //                    int status = result.getResponse().getStatus();
                //                    String answer;
                //                    Exception ex = result.getResolvedException();
                //                    if (ex != null) {
                //                        LOG.info("Got Exception in response");
                //                        answer = String.format("<Ex (%s) : %s>", ex.getClass().getName(), ex.getMessage());
                //                    } else {
                //                        int contentLength = result.getResponse().getContentLength();
                //                        try {
                //                            answer = result.getResponse().getContentAsString();
                //                        } catch (UnsupportedEncodingException exContent) {
                //                            answer = "<Error : " + exContent.getMessage() + ">";
                //                        }
                //                        answer = String.format("(%d) : %s", contentLength, answer);
                //                    }
                //                    LOG.info(String.format("SRV ANS %d : %s", status, answer));
                //                })
                .andExpect(status().isOk());

        LOG.info("updateElementPersonnal should have been called once");
        Mockito.verify(compositionElementSvc, Mockito.times(1)).updateElementPersonnal(ArgumentMatchers.any(), ArgumentMatchers.any());
        LOG.info("updateElementPersonnal should have been called once with compoId");
        Mockito.verify(compositionElementSvc, Mockito.times(1)).updateElementPersonnal(eq(compoId), ArgumentMatchers.any());
    }
}
