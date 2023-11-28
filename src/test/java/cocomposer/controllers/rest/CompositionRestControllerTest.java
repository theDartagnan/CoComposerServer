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
import cocomposer.services.CompositionServiceImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
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
@ActiveProfiles({"mongo-test", "secu-logs"})
@Import(TestDatasetConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CompositionRestControllerTest {

    private static final Log LOG = LogFactory.getLog(CompositionRestControllerTest.class);

    private AutoCloseable mocks;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestDatasetGenerator testDataset;

    @MockBean
    private CompositionServiceImpl compositionSvc;

    private MockMvc mvc;

    public CompositionRestControllerTest() {
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
     * Test of getMyCompositions method, of class CompositionRestController.
     */
    @Test
    @WithUserDetails(value = "mem1@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testGetMyCompositions() throws Exception {
        mvc.perform(get("/api/v1/rest/compositions").accept(MediaType.APPLICATION_JSON))
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
        Mockito.verify(compositionSvc, Mockito.times(1)).getUserCompositions(ArgumentMatchers.any());
        Mockito.verify(compositionSvc, Mockito.times(1)).getUserCompositions(this.testDataset.getTestInstances().getMem1().getId());
    }

    /**
     * Test of createComposition method, of class CompositionRestController.
     */
//    @Test
//    public void testCreateComposition() {
//        System.out.println("createComposition");
//        Composition compositionInfo = null;
//        CoComposerMemberDetails currentUser = null;
//        CompositionRestController instance = null;
//        Composition expResult = null;
//        Composition result = instance.createComposition(compositionInfo, currentUser);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
    /**
     * Test of getComposition method, of class CompositionRestController.
     */
//    @Test
//    public void testGetComposition() {
//        System.out.println("getComposition");
//        String compoId = "";
//        CoComposerMemberDetails currentUser = null;
//        CompositionRestController instance = null;
//        Composition expResult = null;
//        Composition result = instance.getComposition(compoId, currentUser);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
    /**
     * Test of patchComposition method, of class CompositionRestController.
     */
//    @Test
//    public void testPatchComposition() {
//        System.out.println("patchComposition");
//        String compoId = "";
//        CompositionRestController.CompositionPatching compositionInfo = null;
//        CompositionRestController instance = null;
//        CompositionRestController.CompositionPatching expResult = null;
//        CompositionRestController.CompositionPatching result = instance.patchComposition(compoId, compositionInfo);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
    /**
     * Test of deleteComposition method, of class CompositionRestController.
     */
//    @Test
//    public void testDeleteComposition() {
//        System.out.println("deleteComposition");
//        String compoId = "";
//        CompositionRestController instance = null;
//        instance.deleteComposition(compoId);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
}
