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

import cocomposer.config.PasswordEncoderTestConfig;
import cocomposer.config.TestDatasetConfig;
import cocomposer.config.TestDatasetGenerator;
import cocomposer.configuration.MongoConfiguration;
import cocomposer.model.Composition;
import cocomposer.model.CompositionRepository;
import cocomposer.model.CompositionSummary;
import cocomposer.model.MemberRepository;
import cocomposer.services.websocket.CompositionWSService;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 *
 * @author Remi Venant
 */
@ExtendWith(MockitoExtension.class)
@DataMongoTest
@Import({MongoConfiguration.class, PasswordEncoderTestConfig.class, TestDatasetConfig.class})
@ActiveProfiles("mongo-test")
public class CompositionServiceImplTest {

    private static final Log LOG = LogFactory.getLog(CompositionServiceImplTest.class);

    private AutoCloseable mocks;

    @Autowired
    private MemberRepository memberRepo;

    @Autowired
    private CompositionRepository compoRepo;

    @Autowired
    private TestDatasetGenerator testDataset;

    @Mock
    private CompositionWSService compositionWSSvc;

    private CompositionServiceImpl testedService;

    public CompositionServiceImplTest() {
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
        this.testedService = new CompositionServiceImpl(compoRepo, memberRepo, compositionWSSvc);
        this.testDataset.createDataset(true);
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
     * Test of getUserCompositions method, of class CompositionServiceImpl.
     */
    @Test
    public void testGetUserCompositions() {
        final MemberCompositionSummariesCollection memCompo = this.testedService
                .getUserCompositions(this.testDataset.getTestInstances().getMem2().getId());
        assertThat(memCompo).as("MemberCompos is not null").isNotNull();
        assertThat(memCompo.ownedCompositions()).as("OwnedCompo not null and of proper size").hasSize(1);
        assertThat(memCompo.guestCompositions()).as("Guest compo not null and of proper size").hasSize(2);
    }

    /**
     * Test of getComposition method, of class CompositionServiceImpl.
     */
    @Test
    public void testGetComposition() {
        final Composition compoFromOwner = this.testedService.getComposition(
                this.testDataset.getTestInstances().getCompMem1_1().getId(),
                this.testDataset.getTestInstances().getMem1().getId());
        assertThat(compoFromOwner).as("Compo found").isNotNull();
        assertThat(compoFromOwner.getGuests()).as("Guest of proper size").hasSize(2);

        final Composition compoFromGuest = this.testedService.getComposition(
                this.testDataset.getTestInstances().getCompMem2_1().getId(),
                this.testDataset.getTestInstances().getAdmin().getId());
        assertThat(compoFromGuest).as("Compo found").isNotNull();
        assertThat(compoFromGuest.getGuests()).as("Guest of proper size").hasSize(1);
        assertThat(compoFromGuest.getGuests()).as("Guest is admin")
                .containsExactly(this.testDataset.getTestInstances().getAdmin());

        final Composition compoFromNewGuest = this.testedService.getComposition(
                this.testDataset.getTestInstances().getCompMem2_1().getId(),
                this.testDataset.getTestInstances().getMem1().getId());
        assertThat(compoFromNewGuest).as("Compo found").isNotNull();
        assertThat(compoFromNewGuest.getGuests()).as("Guest of proper size").hasSize(2);
        assertThat(compoFromNewGuest.getGuests()).as("Guest is admin")
                .contains(this.testDataset.getTestInstances().getMem1());
    }

    /**
     * Test of createComposition method, of class CompositionServiceImpl.
     */
    @Test
    public void testCreateComposition() {
        final Composition compoToCreate = new Composition("New Compo", true, null);

        final Composition compo = this.testedService.createComposition(
                this.testDataset.getTestInstances().getMem1().getId(),
                compoToCreate);
        assertThat(compo).as("Return compo not null").isNotNull();
        assertThat(compo.getId()).as("Return compo id not null").isNotNull();
        assertThat(compo.getOwner()).as("Compo has an owner").isNotNull();
        assertThat(compo.getOwner().getId()).as("Compo has the proper owner")
                .isEqualTo(this.testDataset.getTestInstances().getMem1().getId());

        Optional<Composition> dbCompo = this.compoRepo.findById(compo.getId());
        assertThat(dbCompo).as("compo is in db").isNotEmpty();

        //Unknown user fails creating compo
        assertThatThrownBy(()
                -> this.testedService.createComposition("a".repeat(24), compoToCreate))
                .as("unknown account id is rejected")
                .isInstanceOf(NoSuchElementException.class);
    }

    /**
     * Test of updateCompositionTitle method, of class CompositionServiceImpl.
     */
    @Test
    public void testUpdateCompositionTitle() {
        final String compoId = this.testDataset.getTestInstances().getCompMem1_1().getId();
        final String newTitle = "new title";

        String returnedTitle = this.testedService.updateCompositionTitlePersonnal(compoId, newTitle);
        assertThat(returnedTitle).as("Returned title matches").isEqualTo(newTitle);

        Optional<Composition> dbCompo = this.compoRepo.findById(compoId);
        assertThat(dbCompo.get().getTitle()).as("Compo title in db matches").isEqualTo(newTitle);
    }

    /**
     * Test of updateCompositionCollaborative method, of class
     * CompositionServiceImpl.
     */
    @Test
    public void testUpdateCompositionCollaborative() {
        final String compoId = this.testDataset.getTestInstances().getCompMem1_1().getId();
        final boolean collaborative = !this.testDataset.getTestInstances().getCompMem1_1().isCollaborative();

        boolean returnedCollab = this.testedService.updateCompositionCollaborativePersonnal(compoId, collaborative);
        assertThat(returnedCollab).as("Returned title matches").isEqualTo(collaborative);

        Optional<Composition> dbCompo = this.compoRepo.findById(compoId);
        assertThat(dbCompo.get().isCollaborative()).as("Compo collab in db matches").isEqualTo(collaborative);
    }

    /**
     * Test of deleteComposition method, of class CompositionServiceImpl.
     */
    @Test
    public void testDeleteComposition() {
        final String compoId = this.testDataset.getTestInstances().getCompMem1_1().getId();
        this.testedService.deleteComposition(compoId);

        Optional<Composition> dbCompo = this.compoRepo.findById(compoId);
        assertThat(dbCompo).as("Compo removed from db").isEmpty();
    }

    /**
     * Test of deleteAllOwnedCompositionsFromUser method, of class
     * CompositionServiceImpl.
     */
    @Test
    public void testDeleteAllOwnedCompositionsFromUser() {
        final String userId = this.testDataset.getTestInstances().getMem1().getId();

        this.testedService.deleteAllOwnedCompositionsFromUser(userId);

        Stream<CompositionSummary> ownerCompos = this.compoRepo.findSummaryByOwner(this.testDataset.getTestInstances().getMem1());
        assertThat(ownerCompos).as("No more owner compo in db").isEmpty();
    }

}
