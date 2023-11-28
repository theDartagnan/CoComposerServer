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
import cocomposer.configuration.MongoConfiguration;
import cocomposer.model.Member;
import cocomposer.model.MemberCredential;
import cocomposer.model.MemberCredentialRepository;
import cocomposer.model.MemberRepository;
import cocomposer.security.authentification.AllowedPasswordEncoder;
import cocomposer.security.authentification.EvolutivePasswordEncoder;
import java.util.NoSuchElementException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.mockito.Mockito;
import static org.mockito.Mockito.times;

/**
 *
 * @author Remi Venant
 */
@ExtendWith(MockitoExtension.class)
@DataMongoTest
@Import({MongoConfiguration.class, PasswordEncoderTestConfig.class})
@ActiveProfiles("mongo-test")
public class AccountServiceImplTest {

    private static final Log LOG = LogFactory.getLog(AccountServiceImplTest.class);

    private AutoCloseable mocks;

    @Autowired
    private MemberCredentialRepository memberCredRepo;

    @Autowired
    private MemberRepository memberRepo;

    @Autowired
    private EvolutivePasswordEncoder passwordEncoder;

    @Mock
    private CompositionService compoSvc;

    @InjectMocks
    private AccountServiceImpl testedService;

    public AccountServiceImplTest() {
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
        this.testedService = new AccountServiceImpl(this.memberCredRepo,
                this.memberRepo, this.passwordEncoder, this.compoSvc);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
            mocks = null;
        }
        this.memberCredRepo.deleteAll();
        this.memberRepo.deleteAll();
    }

    @Test
    public void testGetAccountIsFunctionnal() {
        Member m = this.memberRepo.save(new Member("member@mail.com", "firstname", "lastname"));

        Member account = this.testedService.getAccount(m.getId());
        assertThat(account).isEqualTo(m);
    }

    @Test
    public void testGetAccountIsFunctionnalWithCase() {
        Member m = this.memberRepo.save(new Member("member@mail.com", "firstname", "lastname"));

        Member account = this.testedService.getAccount(m.getId().toUpperCase());
        assertThat(account).isEqualTo(m);
    }

    @Test
    public void testGetAccountNoAccountFoundEx() {
        String wrongId = "a".repeat(24);

        assertThatThrownBy(()
                -> this.testedService.getAccount(wrongId))
                .as("unknown account id is rejected")
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    public void testCreateAccountIsFunctionnal() {
        Member account = this.testedService.createAccount(new Member("myemail@mail.com", "myfirstnmae", "mylastname"), "adupperupperpassword");
        assertThat(account).isNotNull();
        assert account != null;
        assertThat(account.getId()).isNotNull();

        assertThat(this.memberRepo.count()).isEqualTo(1);
        assertThat(this.memberCredRepo.count()).isEqualTo(1);
    }

    @Test
    public void testUpdateAccountIsFunctionnal() {
        String memId = this.memberRepo.save(new Member("myemail@mail.com", "myfirstnmae2", "mylastname2")).getId();

        Member returnUpdatedAccount = this.testedService.updateAccount(memId, new Member("myemail2@mail.com", "myfirstname2", "mylastname2"));
        assertThat(returnUpdatedAccount).as("Returned updated member ok").isNotNull().extracting("email", "firstname", "lastname")
                .containsExactly("myemail2@mail.com", "myfirstname2", "mylastname2");

        Member savedUpdatedAccount = this.memberRepo.findById(memId).get();
        assertThat(savedUpdatedAccount).as("savec updated member ok").isNotNull().extracting("email", "firstname", "lastname")
                .containsExactly("myemail2@mail.com", "myfirstname2", "mylastname2");

        returnUpdatedAccount = this.testedService.updateAccount(memId, new Member(null, null, "mylastname3"));
        assertThat(returnUpdatedAccount).as("Returned updated member ok").isNotNull().extracting("email", "firstname", "lastname")
                .containsExactly("myemail2@mail.com", "myfirstname2", "mylastname3");

        savedUpdatedAccount = this.memberRepo.findById(memId).get();
        assertThat(savedUpdatedAccount).as("savec updated member ok").isNotNull().extracting("email", "firstname", "lastname")
                .containsExactly("myemail2@mail.com", "myfirstname2", "mylastname3");
    }

    @Test
    public void testDeleteAccountIsFunctionnal() {
        Member member = this.memberRepo.save(new Member("myemail@mail.com", "myfirstnmae2", "mylastname2"));
        this.memberCredRepo.save(new MemberCredential(member, "hashedPassword"));
        final String memberId = member.getId();

        this.testedService.deleteAccount(memberId);
        Mockito.verify(this.compoSvc, times(1)).deleteAllOwnedCompositionsFromUser(memberId);
        assertThat(this.memberCredRepo.findByMemberId(memberId)).isEmpty();
        assertThat(this.memberRepo.findById(memberId)).isEmpty();
    }

    @Test
    public void testUpdateAccountPasswordIsFunctional() {
        // Create the password hash for the current password
        final String currentPwd = "MyOldSuperPassword";
        final String currentPwdHash = this.passwordEncoder.encodeWithAllowedAlgorithm(currentPwd, AllowedPasswordEncoder.TST);

        // Create the password hash for the new password
        final String newPwd = "MyNewSuperPassword";

        // Create member with his cred using the currentPwd
        Member member = this.memberRepo.save(new Member("myemail@mail.com", "myfirstnmae2", "mylastname2"));
        this.memberCredRepo.save(new MemberCredential(member, currentPwdHash));
        final String memberId = member.getId();

        // Update password
        this.testedService.updateAccountPassword(memberId, currentPwd, newPwd);

        // Retrieve cred and check its properties
        MemberCredential cred = this.memberCredRepo.findByMemberId(memberId).get();
        assertThat(cred).as("Credential cryptosys properly updated in database").isNotNull();
        assertThat(cred.getEncodedPassword()).as("Credential hash properly updated in database")
                .isNotNull()
                .satisfies(hash -> this.passwordEncoder.matches(newPwd, hash));
    }
}
