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
package cocomposer.model;

import cocomposer.config.PasswordEncoderTestConfig;
import cocomposer.config.TestDatasetConfig;
import cocomposer.config.TestDatasetGenerator;
import cocomposer.configuration.MongoConfiguration;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 *
 * @author Remi Venant
 */
@DataMongoTest
@Import({MongoConfiguration.class, PasswordEncoderTestConfig.class, TestDatasetConfig.class})
@ActiveProfiles("mongo-test")
public class CompositionRepositoryTest {

    private static final Log LOG = LogFactory.getLog(CompositionRepositoryTest.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CompositionRepository testedRepo;

    @Autowired
    private TestDatasetGenerator testDataset;

    public CompositionRepositoryTest() {
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
    public void testExistsByIdAndOwnerId() {
        assertThat(this.testedRepo.existsByIdAndOwnerId(
                this.testDataset.getTestInstances().getCompAdmin1().getId(),
                this.testDataset.getTestInstances().getAdmin().getId()))
                .as("Compo admin1 of admin exists").isTrue();
        assertThat(this.testedRepo.existsByIdAndOwnerId(
                this.testDataset.getTestInstances().getCompAdmin1().getId(),
                this.testDataset.getTestInstances().getMem1().getId()))
                .as("Compo admin1 of member 1 does not exist").isFalse();
    }

    @Test
    public void testExistsByIdAndOwnerOrGuestsId() {
        assertThat(this.testedRepo.existsByIdAndOwnerOrGuestsId(
                this.testDataset.getTestInstances().getCompAdmin1().getId(),
                this.testDataset.getTestInstances().getAdmin().getId()))
                .as("Compo admin1 of owner admin exists").isTrue();
        assertThat(this.testedRepo.existsByIdAndOwnerOrGuestsId(
                this.testDataset.getTestInstances().getCompAdmin1().getId(),
                this.testDataset.getTestInstances().getMem1().getId()))
                .as("Compo admin1 of guest member 1 does not exist").isFalse();
        assertThat(this.testedRepo.existsByIdAndOwnerOrGuestsId(
                this.testDataset.getTestInstances().getCompMem1_2().getId(),
                this.testDataset.getTestInstances().getMem1().getId()))
                .as("Compo Mem1_2 of owner member 1 exists").isTrue();
        assertThat(this.testedRepo.existsByIdAndOwnerOrGuestsId(
                this.testDataset.getTestInstances().getCompMem1_2().getId(),
                this.testDataset.getTestInstances().getMem2().getId()))
                .as("Compo Mem1_2 of guest member 2 exists").isTrue();
        assertThat(this.testedRepo.existsByIdAndOwnerOrGuestsId(
                this.testDataset.getTestInstances().getCompMem1_2().getId(),
                this.testDataset.getTestInstances().getAdmin().getId()))
                .as("Compo Mem1_2 of guest admin does not exist").isFalse();
    }
    
    @Test
    public void testCanUserEditCompo() {
        assertThat(this.testedRepo.canUserEditCompo(
                this.testDataset.getTestInstances().getCompAdmin1().getId(),
                this.testDataset.getTestInstances().getAdmin().getId()))
                .as("Compo admin1 of owner admin can edit compo (owner even if not collaborative)").isTrue();
        assertThat(this.testedRepo.canUserEditCompo(
                this.testDataset.getTestInstances().getCompMem1_1().getId(),
                this.testDataset.getTestInstances().getMem1().getId()))
                .as("Compo Mem1_1 of owner mem 1 exists (owner, even if not collaborative)").isTrue();
        assertThat(this.testedRepo.canUserEditCompo(
                this.testDataset.getTestInstances().getCompMem1_2().getId(),
                this.testDataset.getTestInstances().getMem1().getId()))
                .as("Compo Mem1_2 of owner mem 1 exists (owner and collaborative)").isTrue();
        
        assertThat(this.testedRepo.canUserEditCompo(
                this.testDataset.getTestInstances().getCompAdmin1().getId(),
                this.testDataset.getTestInstances().getMem1().getId()))
                .as("Compo admin1 of mem 1 cannot edit compo (not collaborative and not guest nor owner)").isFalse();
        assertThat(this.testedRepo.canUserEditCompo(
                this.testDataset.getTestInstances().getCompMem1_2().getId(),
                this.testDataset.getTestInstances().getAdmin().getId()))
                .as("Compo Mem1_2 of guest admin cannot edit compo (collaborative but not guest nor owner)").isFalse();
        assertThat(this.testedRepo.canUserEditCompo(
                this.testDataset.getTestInstances().getCompMem1_1().getId(),
                this.testDataset.getTestInstances().getMem2().getId()))
                .as("Compo Mem1_1 of guest mem 2 cannot edit compo (guest but not collaborative)").isFalse();
        
        assertThat(this.testedRepo.canUserEditCompo(
                this.testDataset.getTestInstances().getCompMem1_2().getId(),
                this.testDataset.getTestInstances().getMem2().getId()))
                .as("Compo Mem1_2 of guest mem 2 can edit compo (guest and collaborative").isTrue();
    }

    @Test
    public void testExistsByIdAndElementsId() {
        assertThat(this.testedRepo.existsByIdAndElementsId(
                this.testDataset.getTestInstances().getCompMem1_1().getId(),
                this.testDataset.getTestInstances().getCompMem1_1().getElements().get(0).getId()))
                .as("Compo Mem1_1 with proper element id exists").isTrue();
        assertThat(this.testedRepo.existsByIdAndElementsId(
                this.testDataset.getTestInstances().getCompMem1_1().getId(),
                "bad-elem-id"))
                .as("Compo Mem1_1 with bad element id does not exist").isFalse();

    }

    @Test
    public void testFindSummaryByOwner() {
        List<CompositionSummary> sums = this.testedRepo.findSummaryByOwner(
                this.testDataset.getTestInstances().getMem1()).toList();
        assertThat(sums).as("2 summaries retrieved for owner Mem 1").hasSize(2);
        assertThat(sums).as("2 summaries properly given").allSatisfy((sum) -> {
            assertThat(sum.id()).isNotBlank();
            assertThat(sum.title()).isNotBlank();
        });
        assertThat(sums).as("2 summaries have proper id").extracting("id").containsExactlyInAnyOrder(
                this.testDataset.getTestInstances().getCompMem1_1().getId(),
                this.testDataset.getTestInstances().getCompMem1_2().getId());
    }

    @Test
    public void testFindSummaryByGuests() {
        List<CompositionSummary> sums = this.testedRepo.findSummaryByGuests(
                this.testDataset.getTestInstances().getAdmin()).toList();
        assertThat(sums).as("2 summaries retrieved for guest admin").hasSize(2);
        assertThat(sums).as("2 summaries properly given").allSatisfy((sum) -> {
            assertThat(sum.id()).isNotBlank();
            assertThat(sum.title()).isNotBlank();
        });
        assertThat(sums).as("2 summaries have proper id").extracting("id").containsExactlyInAnyOrder(
                this.testDataset.getTestInstances().getCompMem1_1().getId(),
                this.testDataset.getTestInstances().getCompMem2_1().getId());
    }

    @Test
    public void testFindCompositionIdsByOwner() {
        List<IdOnly> ids = this.testedRepo.findCompositionIdsByOwner(
                this.testDataset.getTestInstances().getMem1()).toList();
        assertThat(ids).as("2 ids retrieved for owner Mem 1").hasSize(2);
        assertThat(ids).as("2 ids have proper id").extracting("id").containsExactlyInAnyOrder(
                this.testDataset.getTestInstances().getCompMem1_1().getId(),
                this.testDataset.getTestInstances().getCompMem1_2().getId());
    }

    @Test
    public void testDeleteByIdIn() {
        long initRepoSize = this.testedRepo.count();
        // We volontary set a list with a duplicated id
        this.testedRepo.deleteByIdIn(List.of(
                this.testDataset.getTestInstances().getCompMem1_1().getId(),
                this.testDataset.getTestInstances().getCompMem1_1().getId(),
                this.testDataset.getTestInstances().getCompMem1_2().getId()
        ));
        long newRepoSize = this.testedRepo.count();
        assertThat(newRepoSize).as("2 compositions have been removed").isEqualTo(initRepoSize - 2);
    }

    @Test
    public void testFindAndSetTitleById() {
        String newTitle = "New Compo Title";
        long res = this.testedRepo.findAndSetTitleById(
                this.testDataset.getTestInstances().getCompMem1_1().getId(),
                newTitle);
        assertThat(res).as("Title properly updated").isEqualTo(1);
        assertThat(this.testedRepo.findById(this.testDataset.getTestInstances().getCompMem1_1().getId()))
                .as("Title in the db document is the new one")
                .get().extracting("title").isEqualTo(newTitle);

        res = this.testedRepo.findAndSetTitleById(
                this.testDataset.getTestInstances().getCompMem1_1().getId(),
                newTitle);
        assertThat(res).as("Same title not properly updated").isEqualTo(0);
    }

    @Test
    public void testFindAndSetCollaborativeById() {
        boolean newCollaborative = true;
        long res = this.testedRepo.findAndSetCollaborativeById(
                this.testDataset.getTestInstances().getCompMem1_1().getId(),
                newCollaborative);
        assertThat(res).as("Collaborative properly updated").isEqualTo(1);
        assertThat(this.testedRepo.findById(this.testDataset.getTestInstances().getCompMem1_1().getId()))
                .as("Collaborative in the db document is the new one")
                .get().extracting("collaborative").isEqualTo(newCollaborative);

        res = this.testedRepo.findAndSetCollaborativeById(
                this.testDataset.getTestInstances().getCompMem1_1().getId(),
                newCollaborative);
        assertThat(res).as("Same collaborative not properly updated").isEqualTo(0);
    }

    @Test
    public void testFindAndPushGuestById() {
        final String compoId = this.testDataset.getTestInstances().getCompAdmin1().getId();
        int originalGuestNumber = this.testedRepo.findById(compoId).get()
                .getGuests().size();
        long res = this.testedRepo.findAndPushGuestById(compoId, this.testDataset.getTestInstances().getMem1());
        assertThat(res).as("Guest properly added").isEqualTo(1);
        List<Member> newGuests = this.testedRepo.findById(compoId).get().getGuests();
        assertThat(newGuests)
                .as("Guest list has proper number").hasSize(originalGuestNumber + 1)
                .as("Guest are properly defined in db").containsExactlyInAnyOrder(this.testDataset.getTestInstances().getMem1());

        res = this.testedRepo.findAndPushGuestById(compoId, this.testDataset.getTestInstances().getMem1());
        assertThat(res).as("Same Guest not properly added").isEqualTo(0);
    }

    @Test
    public void testFindAndPushElementById() {
        final String compoId = this.testDataset.getTestInstances().getCompAdmin1().getId();
        CompositionElement newCE = new CompositionElement("ID-NEW-ELEM", "rect", null, 0, 0);
        int originalElementNumber = this.testedRepo.findById(compoId).get()
                .getElements().size();

        long res = this.testedRepo.findAndPushElementById(compoId, newCE);
        assertThat(res).as("Element properly added").isEqualTo(1);
        List<CompositionElement> newElements = this.testedRepo.findById(compoId).get().getElements();

        assertThat(newElements)
                .as("Element list has proper number").hasSize(originalElementNumber + 1)
                .as("Elements contains new element").contains(newCE);

        res = this.testedRepo.findAndPushElementById(compoId, newCE);
        assertThat(res).as("Same Element properly added twice").isEqualTo(1);
    }
    
//    @Test
//    public void testFindAndPushElementByIdKOOnBadElement() {
//        final String compoId = this.testDataset.getTestInstances().getCompAdmin1().getId();
//        
//        final CompositionElement newCE = new CompositionElement(null, "rect", null, 0, 0);
//        assertThatThrownBy(()
//                -> this.testedRepo.findAndPushElementById(compoId, newCE))
//                .as("Null element id is rejected")
//                .isInstanceOf(ConstraintViolationException.class);
//        
//        final CompositionElement newCE2 = new CompositionElement("aa##aa", "rect", null, 0, 0);
//        assertThatThrownBy(()
//                -> this.testedRepo.findAndPushElementById(compoId, newCE2))
//                .as("Bad element id is rejected")
//                .isInstanceOf(ConstraintViolationException.class);
//    }

    @Test
    public void testFindAndSetElementByIdAndElementsId() {
        final String compoId = this.testDataset.getTestInstances().getCompAdmin1().getId();

        // Test updating existing component
        final CompositionElement elementToUpdate = new CompositionElement(
                this.testDataset.getTestInstances().getCompAdmin1().getElements().get(0).getId(),
                "circle", "style1", 14, 15
        );
        elementToUpdate.addExtraProperties("r", 34.45);

        long res = this.testedRepo.findAndSetElementByIdAndElementsId(compoId, elementToUpdate);
        assertThat(res).as("Element properly updated").isEqualTo(1);
        List<CompositionElement> elements = this.testedRepo.findById(compoId).get().getElements();
        CompositionElement updatedElement = elements.stream().filter((e) -> e.getId().equals(elementToUpdate.getId())).findFirst().get();
        assertThat(updatedElement).as("Element values properly updated")
                .extracting("elementType", "style", "x", "y")
                .containsExactly(elementToUpdate.getElementType(), elementToUpdate.getStyle(), elementToUpdate.getX(), elementToUpdate.getY());
        assertThat(updatedElement.getExtraProperties()).as("Element extra props properly updated")
                .containsExactlyEntriesOf(elementToUpdate.getExtraProperties());

        // Test update new component
        CompositionElement newCE = new CompositionElement("ID-NEW-ELEM", "rect", null, 0, 0);
        res = this.testedRepo.findAndSetElementByIdAndElementsId(compoId, newCE);
        assertThat(res).as("Unknown element not properly updated").isEqualTo(0);

    }

    @Test
    public void testFindAndSetElementPositionByIdAndElementsId() {
        final String compoId = this.testDataset.getTestInstances().getCompAdmin1().getId();
        final String elemId = this.testDataset.getTestInstances().getCompAdmin1().getElements().get(0).getId();
        final double newX = 1567;
        final double newY = 2678;
        // Test updating position of existing component
        long res = this.testedRepo.findAndSetElementPositionByIdAndElementsId(compoId, elemId, newX, newY);
        assertThat(res).as("Element position properly updated").isEqualTo(1);
        List<CompositionElement> elements = this.testedRepo.findById(compoId).get().getElements();
        CompositionElement updatedElement = elements.stream().filter((e) -> e.getId().equals(elemId)).findFirst().get();
        assertThat(updatedElement).as("Element values properly updated")
                .extracting("x", "y")
                .containsExactly(newX, newY);
        // Test update new component
        res = this.testedRepo.findAndSetElementPositionByIdAndElementsId(compoId, "NEW_ELEM_ID", newX, newY);
        assertThat(res).as("Unknown Element position properly updated").isEqualTo(0);
    }

    @Test
    public void testFindAndPullElementById() {
        final String compoId = this.testDataset.getTestInstances().getCompAdmin1().getId();
        final String elemId = this.testDataset.getTestInstances().getCompAdmin1().getElements().get(0).getId();
        final int originalElemNumber = this.testDataset.getTestInstances().getCompAdmin1().getElements().size();

        // Test pulling position of existing component
        long res = this.testedRepo.findAndPullElementById(compoId, elemId);
        assertThat(res).as("Element properly removed").isEqualTo(1);
        List<CompositionElement> elements = this.testedRepo.findById(compoId).get().getElements();
        assertThat(elements)
                .as("New element list has a proper size").hasSize(originalElemNumber - 1)
                .as("NEw element list does not cotains old element").extracting("id").doesNotContain(elemId);
    }

}
