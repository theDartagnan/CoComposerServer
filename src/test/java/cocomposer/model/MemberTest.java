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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;
import org.springframework.data.mongodb.core.query.BasicQuery;

/**
 *
 * @author Remi Venant
 */
@DataMongoTest
@Import(MongoConfiguration.class)
@ActiveProfiles("mongo-test")
public class MemberTest {

    private static final Log LOG = LogFactory.getLog(MemberTest.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    public MemberTest() {
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
        this.mongoTemplate.remove(new BasicQuery("{}"), Member.class);
    }

    @Test
    public void MemberPersistence() {
        Member memToSave = new Member("email@mail.com", "firstname", "lastname");
        Member savecMem = this.mongoTemplate.save(memToSave);
        assertThat(savecMem.getId()).as("entity created has an id").isNotNull();
        assertThat(savecMem).extracting("email", "firstname", "lastname", "adminFlag")
                .containsExactly(memToSave.getEmail(), memToSave.getFirstname(), memToSave.getLastname(), null);
        assertThat(savecMem.isAdmin()).isFalse();
    }

    /**
     * Test email validation
     */
    @Test
    public void testEmailValidation() {
        assertThatThrownBy(()
                -> this.mongoTemplate.save(new Member("email@mail .com", "firstname", "lastname")))
                .as("Invalid email is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.mongoTemplate.save(new Member("email<Injection>@mail.com", "firstname", "lastname")))
                .as("Invalid email is rejected (2)")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.mongoTemplate.save(new Member("e".repeat(100) + "email@mail.com", "firstname", "lastname")))
                .as("Too long email is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.mongoTemplate.save(new Member("", "firstname", "lastname")))
                .as("empty email is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.mongoTemplate.save(new Member("       ", "firstname", "lastname")))
                .as("blanck email is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.mongoTemplate.save(new Member(null, "firstname", "lastname")))
                .as("null email is rejected")
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    public void testUniqueEmail() {
        this.mongoTemplate.save(new Member("email@mail.com", "firstname", "lastname"));
        this.mongoTemplate.save(new Member("email2@mail.com", "firstname", "lastname"));
        assertThatThrownBy(()
                -> this.mongoTemplate.save(new Member("email@mail.com", "firstname", "lastname")))
                .as("Duplicated mail rejected")
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    public void testFirstnameValidation() {
        assertThatThrownBy(()
                -> this.mongoTemplate.save(new Member("email@mail.com", "e".repeat(101), "lastname")))
                .as("Too long firstname is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.mongoTemplate.save(new Member("email@mail.com", "", "lastname")))
                .as("Empty firstname is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.mongoTemplate.save(new Member("email@mail.com", "   ", "lastname")))
                .as("blanck firstname is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.mongoTemplate.save(new Member("email@mail.com", null, "lastname")))
                .as("Null firstname is rejected")
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    public void testLastnameValidation() {
        assertThatThrownBy(()
                -> this.mongoTemplate.save(new Member("email@mail.com", "firstname", "e".repeat(101))))
                .as("Too long lastname is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.mongoTemplate.save(new Member("email@mail.com", "firstname", "")))
                .as("Empty lastname is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.mongoTemplate.save(new Member("email@mail.com", "firstname", "   ")))
                .as("blanck lastname is rejected")
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(()
                -> this.mongoTemplate.save(new Member("email@mail.com", "firstname", null)))
                .as("Null lastname is rejected")
                .isInstanceOf(ConstraintViolationException.class);
    }

}
