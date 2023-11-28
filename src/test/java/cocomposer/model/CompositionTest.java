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

import cocomposer.configuration.MongoConfiguration;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.test.context.ActiveProfiles;

/**
 *
 * @author Remi Venant
 */
@DataMongoTest
@Import(MongoConfiguration.class)
@ActiveProfiles("mongo-test")
public class CompositionTest {

    private static final Log LOG = LogFactory.getLog(CompositionTest.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    public CompositionTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
        this.mongoTemplate.remove(new BasicQuery("{}"), Composition.class);
        this.mongoTemplate.remove(new BasicQuery("{}"), Member.class);
    }

    @Test
    public void testCreation() {
        Member member = this.mongoTemplate.save(new Member("member1@mail.com", "firstname", "lastname"));
        Composition cToCreate = new Composition("a title", false, member);
        Composition createdCompo = this.mongoTemplate.save(cToCreate);
        assertThat(createdCompo.getId()).as("entity created has an id").isNotNull();
        assertThat(createdCompo).extracting("title")
                .isEqualTo(cToCreate.getTitle());
        Member owner = createdCompo.getOwner();
        assertThat(owner).isEqualTo(member);
        assertThat(createdCompo.getGuests()).as("no editor but list present").isNotNull().isEmpty();
        assertThat(createdCompo.getElements()).as("no elements but list present").isNotNull().isEmpty();
    }

    @Test
    public void testAddToGuests() {
        Member member = this.mongoTemplate.save(new Member("member1@mail.com", "firstname", "lastname"));
        Composition composition = this.mongoTemplate.save(new Composition("a title", false, member));

        Member guest1 = this.mongoTemplate.save(new Member("member2@mail.com", "firstname", "lastname"));
        Member guest2 = this.mongoTemplate.save(new Member("member3@mail.com", "firstname", "lastname"));

        composition.addGuest(guest1);
        composition.addGuest(guest2);
        composition = this.mongoTemplate.save(composition);

        LOG.info("-".repeat(10) + " PLEASE CHECK LOG FOR DOCUMENT CREATION " + "-".repeat(10));

        Composition testedCompo = this.mongoTemplate.findById(composition.getId(), Composition.class);
        assertThat(testedCompo).as("Composition retrieved ok").isNotNull();
        assert testedCompo != null;
        LOG.info("-".repeat(10) + " PLEASE CHECK LOG FOR ONLY ONE FIND REQUEST ON compositions collection" + "-".repeat(10));

        List<Member> guests = testedCompo.getGuests();
        assertThat(guests).as("Proper guests retrieve").isNotNull().hasSize(2);
        LOG.info("-".repeat(10) + " PLEASE CHECK LOG FOR ONLY ONE 1 REQUEST ON members collection " + "-".repeat(10));
    }

    @Test
    public void testAddElements() {
        Member member = this.mongoTemplate.save(new Member("member1@mail.com", "firstname", "lastname"));
        Composition composition = this.mongoTemplate.save(new Composition("a title", false, member));

        final ArrayList<CompositionElement> elements = new ArrayList<>();
        CompositionElement elem = new CompositionElement("ctest-1", "rect", null, 0, 0);
        elem.addExtraProperties("width", 100);
        elem.addExtraProperties("height", 200);
        elements.add(elem);
        elem = new CompositionElement("ctest-2", "text", "color: red;", 10, 20);
        elem.addExtraProperties("text", "coucou");
        elements.add(elem);

        composition.setElements(elements);
        this.mongoTemplate.save(composition);

        Composition retrievedComposition = this.mongoTemplate.findById(composition.getId(), Composition.class);
        assertThat(retrievedComposition).as("Composition retrieved ok").isNotNull();
        List<CompositionElement> retrievedElements = retrievedComposition.getElements();

        LOG.info("-".repeat(10) + " PLEASE CHECK LOG FOR ONLY ONE FIND REQUEST ON compositions collection" + "-".repeat(10));
        assertThat(retrievedElements).isNotNull().hasSize(elements.size());
        assertThat(retrievedElements).as("Element are the good ones").containsExactlyInAnyOrder(elements.toArray(new CompositionElement[elements.size()]));

        assertThat(retrievedElements).as("Element have proper extra properties").allSatisfy((retrievedElem) -> {
            CompositionElement originalElem = elements.stream().filter((e) -> e.getId().equals(retrievedElem.getId())).findFirst().orElse(null);
            assertThat(originalElem).as("Element is one the elements given").isNotNull();
            assertThat(retrievedElem.getExtraProperties()).as("Element extra props are those given").containsExactlyEntriesOf(originalElem.getExtraProperties());
        });
    }

    @Test
    public void testTitleValidation() {
        Member member = this.mongoTemplate.save(new Member("member1@mail.com", "firstname", "lastname"));
        assertThatThrownBy(()
                -> this.mongoTemplate.save(new Composition(null, false, member)))
                .as("Null title is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.mongoTemplate.save(new Composition("", false, member)))
                .as("Empty title is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.mongoTemplate.save(new Composition("    ", false, member)))
                .as("Blank title is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.mongoTemplate.save(new Composition("abcd", false, member)))
                .as("Title less than 5 chars is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.mongoTemplate.save(new Composition("a".repeat(151), false, member)))
                .as("Title longer than 150 chars is rejected")
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    public void testOwnerValidation() {
        assertThatThrownBy(()
                -> this.mongoTemplate.save(new Composition("Proper title", false, null)))
                .as("Null owner is rejected")
                .isInstanceOf(ConstraintViolationException.class);
    }
    
    @Test
    public void testElementValidation() {
        Member member = this.mongoTemplate.save(new Member("member1@mail.com", "firstname", "lastname"));
        Composition composition = this.mongoTemplate.save(new Composition("a title", false, member));
        final ArrayList<CompositionElement> elements = new ArrayList<>();
        CompositionElement elem;
        
        // Null element type
        elements.clear();
        elem = new CompositionElement("aaa", null, null, 0, 0);
        elements.add(elem);
        composition.setElements(elements);
        assertThatThrownBy(()
                -> this.mongoTemplate.save(composition))
                .as("Null element type is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        
        // Null element id
        elements.clear();
        elem = new CompositionElement(null, "rect", null, 0, 0);
        elements.add(elem);
        composition.setElements(elements);
        assertThatThrownBy(()
                -> this.mongoTemplate.save(composition))
                .as("Null element id is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        
        //empty element id
        elements.clear();
        elem = new CompositionElement("", "rect", null, 0, 0);
        elements.add(elem);
        composition.setElements(elements);
        assertThatThrownBy(()
                -> this.mongoTemplate.save(composition))
                .as("Null element id is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        
        //wrong format element id
        elements.clear();
        elem = new CompositionElement("aa aa", "rect", null, 0, 0);
        elements.add(elem);
        composition.setElements(elements);
        assertThatThrownBy(()
                -> this.mongoTemplate.save(composition))
                .as("Null element id is rejected")
                .isInstanceOf(ConstraintViolationException.class);
    }

}
