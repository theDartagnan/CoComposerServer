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
import cocomposer.model.Composition;
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
public class CompositionServiceTest {

    private static final Log LOG = LogFactory.getLog(CompositionService.class);

    @Autowired
    private TestDatasetGenerator testDataset;

    @Autowired
    private CompositionService testedService;

    public CompositionServiceTest() {
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

    @Test
    @WithUserDetails(value = "admin@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testGetUserCompositionsOKForAdmin() {
        MemberCompositionSummariesCollection result = this.testedService.getUserCompositions(this.testDataset.getTestInstances().getMem1().getId());
        assertThat(result).isNotNull();
    }

    @Test
    @WithUserDetails(value = "mem1@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testGetUserCompositionsOKForSelf() {
        MemberCompositionSummariesCollection result = this.testedService.getUserCompositions(this.testDataset.getTestInstances().getMem1().getId());
        assertThat(result).isNotNull();
    }

    @Test
    @WithUserDetails(value = "mem1@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testGetUserCompositionsKONotForSelf() {
        assertThatThrownBy(()
                -> this.testedService.getUserCompositions(this.testDataset.getTestInstances().getMem2().getId()))
                .as("No admin and not self rejected")
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithUserDetails(value = "admin@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testGetCompositionOkForAdmin() {
        final String compoId = this.testDataset.getTestInstances().getCompMem2_1().getId();
        final String userId = this.testDataset.getTestInstances().getMem1().getId();
        Composition result = this.testedService.getComposition(compoId, userId);
        assertThat(result).isNotNull();
    }

    @Test
    @WithUserDetails(value = "mem1@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testGetCompositionOkForSelf() {
        final String compoId = this.testDataset.getTestInstances().getCompMem2_1().getId();
        final String userId = this.testDataset.getTestInstances().getMem1().getId();
        Composition result = this.testedService.getComposition(compoId, userId);
        assertThat(result).isNotNull();
    }

    @Test
    @WithUserDetails(value = "mem1@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testGetCompositionKoForNotSelf() {
        final String compoId = this.testDataset.getTestInstances().getCompMem2_1().getId();
        final String userId = this.testDataset.getTestInstances().getMem2().getId();

        assertThatThrownBy(()
                -> this.testedService.getComposition(compoId, userId))
                .as("No admin and not self rejected")
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithUserDetails(value = "admin@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testCreateCompositionOkForAdmin() {
        final String compoOwnerId = this.testDataset.getTestInstances().getMem1().getId();
        Composition compoToCreate = new Composition("compo title", true, null);
        Composition result = this.testedService.createComposition(compoOwnerId, compoToCreate);
        assertThat(result).isNotNull();
    }

    @Test
    @WithUserDetails(value = "mem1@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testCreateCompositionOkForSelf() {
        final String compoOwnerId = this.testDataset.getTestInstances().getMem1().getId();
        Composition compoToCreate = new Composition("compo title", true, null);
        Composition result = this.testedService.createComposition(compoOwnerId, compoToCreate);
        assertThat(result).isNotNull();
    }

    @Test
    @WithUserDetails(value = "mem1@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testCreateCompositionKoForNotSelf() {
        final String compoOwnerId = this.testDataset.getTestInstances().getMem2().getId();
        Composition compoToCreate = new Composition("compo title", true, null);
        assertThatThrownBy(()
                -> this.testedService.createComposition(compoOwnerId, compoToCreate))
                .as("No admin and not self rejected")
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithUserDetails(value = "admin@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testUpdateCompositionTitleOkForAdmin() {
        String compoId = this.testDataset.getTestInstances().getCompMem1_2().getId();
        String newTitle = "NEW TITLE";
        String updatedTitle = this.testedService.updateCompositionTitlePersonnal(compoId, newTitle);
        assertThat(updatedTitle).isEqualTo(newTitle);
    }

    @Test
    @WithUserDetails(value = "mem1@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testUpdateCollaborativeCollaborativeCompositionTitleOkForOwner() {
        String compoId = this.testDataset.getTestInstances().getCompMem1_2().getId();
        String newTitle = "NEW TITLE";
        String updatedTitle = this.testedService.updateCompositionTitleCollaborative(compoId, newTitle);
        assertThat(updatedTitle).isEqualTo(newTitle);
    }

    @Test
    @WithUserDetails(value = "mem1@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testUpdatePersonnalCollaborativeCompositionTitleOkForOwner() {
        String compoId = this.testDataset.getTestInstances().getCompMem1_2().getId();
        String newTitle = "NEW TITLE";
        assertThatThrownBy(()
                -> this.testedService.updateCompositionTitlePersonnal(compoId, newTitle))
                .as("No admin and not owner rejected")
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithUserDetails(value = "mem2@collamap.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void testUpdateCompositionTitleKoForOther() {
        String compoId = this.testDataset.getTestInstances().getCompMem1_2().getId();
        String newTitle = "NEW TITLE";
        assertThatThrownBy(()
                -> this.testedService.updateCompositionTitlePersonnal(compoId, newTitle))
                .as("No admin and not owner rejected")
                .isInstanceOf(AccessDeniedException.class);
    }

    public void testUpdateCompositionCollaborative() {
        fail("The test case is a prototype.");
    }

    public void testDeleteComposition() {
        fail("The test case is a prototype.");
    }

    public void testDeleteAllOwnedCompositionsFromUser() {
        fail("The test case is a prototype.");
    }

}
