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
import cocomposer.model.CompositionElement;
import cocomposer.model.CompositionRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;

/**
 *
 * @author Remi Venant
 */
@ExtendWith(MockitoExtension.class)
@DataMongoTest
@Import({MongoConfiguration.class, PasswordEncoderTestConfig.class, TestDatasetConfig.class})
@ActiveProfiles("mongo-test")
public class CompositionElementServiceImplTest {

    private static final Log LOG = LogFactory.getLog(CompositionElementServiceImplTest.class);

    private AutoCloseable mocks;

    @Autowired
    private CompositionRepository compoRepo;

    @Autowired
    private TestDatasetGenerator testDataset;

    private CompositionElementServiceImpl testedService;

    public CompositionElementServiceImplTest() {
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
        this.testedService = new CompositionElementServiceImpl(compoRepo);
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
     * Test of addElement method, of class CompositionElementServiceImpl.
     */
    @Test
    public void testAddElement() {
        String compoId = this.testDataset.getTestInstances().getCompMem1_1().getId();

        // Attempt to add an existing element
        final CompositionElement dupElem = new CompositionElement(
                this.testDataset.getTestInstances().getCompMem1_1().getElements().get(0).getId(),
                "circle", null, 0, 0);
        assertThatThrownBy(()
                -> this.testedService.addElement(compoId, dupElem))
                .as("duplicated element is rejected")
                .isInstanceOf(DuplicateKeyException.class);

        // Attempt to add new element
        final CompositionElement elemToCreate = new CompositionElement(
                "NEW-ID-COMP", "circle", null, 0, 0);
        final CompositionElement returnedElem = this.testedService.addElement(compoId, elemToCreate);
        assertThat(returnedElem).as("Returned elem mathces").isEqualTo(elemToCreate);

        final Composition dbCompo = this.compoRepo.findById(compoId).get();
        assertThat(dbCompo.getElements()).as("Element is in db").contains(elemToCreate);
    }

    /**
     * Test of updateElement method, of class CompositionElementServiceImpl.
     */
    @Test
    public void testUpdateElement() {
        String compoId = this.testDataset.getTestInstances().getCompMem1_1().getId();

        // Attempt to add an new element
        final CompositionElement elemToCreate = new CompositionElement(
                "NEW-ID-COMP", "circle", null, 0, 0);
        assertThatThrownBy(()
                -> this.testedService.updateElement(compoId, elemToCreate))
                .as("unknown element is rejected")
                .isInstanceOf(NoSuchElementException.class);

        final CompositionElement updatedElem = new CompositionElement(
                this.testDataset.getTestInstances().getCompMem1_1().getElements().get(0).getId(),
                "circle", "color:red;", 10, 10);
        updatedElem.addExtraProperties("r", 100);
        final CompositionElement returnedElem = this.testedService.updateElement(compoId, updatedElem);
        assertThat(returnedElem).as("Returned elem mathces").isEqualTo(updatedElem);

        final Composition dbCompo = this.compoRepo.findById(compoId).get();
        final CompositionElement dbElem = dbCompo.getElements().stream().filter((e) -> e.getId().equals(updatedElem.getId()))
                .findFirst().orElse(null);
        assertThat(dbElem).as("Element present in db").isNotNull();
        assertThat(dbElem).extracting("elementType", "style", "x", "y")
                .as("All base properties updated")
                .containsExactly(updatedElem.getElementType(), updatedElem.getStyle(),
                        updatedElem.getX(), updatedElem.getY());
        assertThat(dbElem.getExtraProperties()).as("All extra prop match")
                .containsExactlyEntriesOf(updatedElem.getExtraProperties());
    }

    /**
     * Test of updateElementPosition method, of class
     * CompositionElementServiceImpl.
     */
    @Test
    public void testUpdateElementPosition() {
        String compoId = this.testDataset.getTestInstances().getCompMem1_1().getId();
        String elemId = this.testDataset.getTestInstances().getCompMem1_1().getElements().get(0).getId();
        double x = 1543;
        double y = 5643;
        
        this.testedService.updateElementPosition(compoId, elemId, x, y);
        final Composition dbCompo = this.compoRepo.findById(compoId).get();
        final CompositionElement dbElem = dbCompo.getElements().stream().filter((e) -> e.getId().equals(elemId))
                .findFirst().orElse(null);
        assertThat(dbElem).as("Element present in db").isNotNull();
        assertThat(dbElem).extracting("x", "y")
                .as("X and Y properties updated")
                .containsExactly(x ,y);
        
    }

    /**
     * Test of deleteElement method, of class CompositionElementServiceImpl.
     */
    @Test
    public void testDeleteElement() {
        String compoId = this.testDataset.getTestInstances().getCompMem1_1().getId();

        // Attempt to remove an inexisting element
        assertThatThrownBy(()
                -> this.testedService.deleteElement(compoId, "UKN-ELEM-ID"))
                .as("unknown element is rejected")
                .isInstanceOf(NoSuchElementException.class);

        String elemId = this.testDataset.getTestInstances().getCompMem1_1().getElements().get(0).getId();
        this.testedService.deleteElement(compoId, elemId);
        
        final Composition dbCompo = this.compoRepo.findById(compoId).get();
        final Optional<CompositionElement> dbElem = dbCompo.getElements().stream().filter((e) -> e.getId().equals(elemId))
                .findFirst();
        assertThat(dbElem).as("Element absent from db").isEmpty();
    }

}
