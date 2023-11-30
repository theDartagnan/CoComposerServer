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
import cocomposer.model.CompositionElement;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;

/**
 *
 * @author Remi Venant
 */
@ActiveProfiles({"mongo-test", "no-ext-broker"})
@Import(TestDatasetConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CompositionElementServiceTest {

    private static final Log LOG = LogFactory.getLog(CompositionService.class);

    @Autowired
    private TestDatasetGenerator testDataset;

    @Autowired
    private CompositionElementService testedService;

    public CompositionElementServiceTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
        this.testDataset.createDataset(true);
    }

    @AfterEach
    public void tearDown() {
        this.testDataset.clear();
    }

    // We test a compo where admin is not guest: compMem1_2
    @Test
    @WithUserDetails(value = "admin@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testAddElementOkForAdmin() {
        String compoId = this.testDataset.getTestInstances().getCompMem1_2().getId();
        CompositionElement newElement = new CompositionElement("NEW-ELEM-1", "rect", null, 10, 30);

        CompositionElement res = this.testedService.addElementPersonnal(compoId, newElement);
        assertThat(res).isNotNull();
    }

    // We test a non collab compo: compMem1_1 with mem1 as owner
    @Test
    @WithUserDetails(value = "mem1@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testAddElementOkForOwner() {
        String compoId = this.testDataset.getTestInstances().getCompMem1_1().getId();
        CompositionElement newElement = new CompositionElement("NEW-ELEM-1", "rect", null, 10, 30);

        CompositionElement res = this.testedService.addElementPersonnal(compoId, newElement);
        assertThat(res).isNotNull();
    }

    // We test a collab compo with a non admin guest: compMem1_2 with mem2 as guest
    @Test
    @WithUserDetails(value = "mem2@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testAddElementCollaborativeOkForGuestAndCollaborativeMap() {
        String compoId = this.testDataset.getTestInstances().getCompMem1_2().getId();
        CompositionElement newElement = new CompositionElement("NEW-ELEM-1", "rect", null, 10, 30);

        CompositionElement res = this.testedService.addElementCollaborative(compoId, newElement);
        assertThat(res).isNotNull();
    }

    @Test
    @WithUserDetails(value = "mem2@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testAddElementPersonnalKoForGuestAndCollaborativeMap() {
        String compoId = this.testDataset.getTestInstances().getCompMem1_2().getId();
        CompositionElement newElement = new CompositionElement("NEW-ELEM-1", "rect", null, 10, 30);
        assertThatThrownBy(()
                -> this.testedService.addElementPersonnal(compoId, newElement))
                .as("personnal call for collaborative compo rejected")
                .isInstanceOf(AccessDeniedException.class);
    }

    // We test a non collab compo with a non admin guest: compMem1_1 with mem2 as guest
    @Test
    @WithUserDetails(value = "mem2@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testAddElementKOForGuestAndNotCollaborativeMap() {
        String compoId = this.testDataset.getTestInstances().getCompMem1_1().getId();
        CompositionElement newElement = new CompositionElement("NEW-ELEM-1", "rect", null, 10, 30);

        assertThatThrownBy(()
                -> this.testedService.addElementPersonnal(compoId, newElement))
                .as("No admin and guest for non collab compo rejected")
                .isInstanceOf(AccessDeniedException.class);
    }

    // We test a collab compo with a user not admin and not guest: comMem2_1 with mem1
    @Test
    @WithUserDetails(value = "mem1@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testAddElementKOForOthers() {
        String compoId = this.testDataset.getTestInstances().getCompMem2_1().getId();
        CompositionElement newElement = new CompositionElement("NEW-ELEM-1", "rect", null, 10, 30);

        assertThatThrownBy(()
                -> this.testedService.addElementPersonnal(compoId, newElement))
                .as("No admin and guest for non collab compo rejected")
                .isInstanceOf(AccessDeniedException.class);
    }

    public void testUpdateElement() {
        fail("The test case is a prototype.");
    }

    public void testUpdateElementPosition() {
        fail("The test case is a prototype.");
    }

    public void testDeleteElement() {
        fail("The test case is a prototype.");
    }

}
