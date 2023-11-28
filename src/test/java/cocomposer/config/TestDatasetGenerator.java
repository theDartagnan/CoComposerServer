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
package cocomposer.config;

import cocomposer.model.Composition;
import cocomposer.model.CompositionElement;
import cocomposer.model.CompositionRepository;
import cocomposer.model.Member;
import cocomposer.model.MemberCredential;
import cocomposer.model.MemberCredentialRepository;
import cocomposer.model.MemberRepository;
import java.util.Arrays;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 *
 * @author Remi Venant
 */
public class TestDatasetGenerator {

    private static final Log LOG = LogFactory.getLog(TestDatasetGenerator.class);

    private final MemberRepository memberRepo;

    private final MemberCredentialRepository memberCredRepo;

    private final PasswordEncoder passwordEncoder;

    private final CompositionRepository compoRepo;

    private boolean init;

    private TestInstances testInstances;

    public TestDatasetGenerator(MemberRepository memberRepo,
            MemberCredentialRepository memberCredRepo, PasswordEncoder passwordEncoder,
            CompositionRepository compoRepo) {
        this.memberRepo = memberRepo;
        this.memberCredRepo = memberCredRepo;
        this.passwordEncoder = passwordEncoder;
        this.compoRepo = compoRepo;
    }

    public void clear() {
        LOG.info("Clear all test dataset in db");
        this.compoRepo.deleteAll();
        this.memberCredRepo.deleteAll();
        this.memberRepo.deleteAll();
        this.init = false;
    }

    public boolean isInit() {
        return init;
    }

    public TestInstances getTestInstances() {
        return this.init ? testInstances : null;
    }

    public void createDataset(boolean withCompo) {
        LOG.info("INIT TEST DATASET");
        if (this.init) {
            this.clear();
        }
        this.testInstances = new TestInstances();
        this.createMembersWithCred(testInstances);
        if (withCompo) {
            this.createComposWithMembers(testInstances);
        }
        this.init = true;
        LOG.info("TEST DATASET READY");
    }

    public void createOnlyMembersDataset() {
        this.createDataset(false);
    }

    private void createMembersWithCred(TestInstances ti) {
        LOG.info("CREATE TEST MEMBERS AND CREDS");
        ti.admin = this.memberRepo.save(createMemberAsAdmin("admin@collamap.com", "John", "TheAdmin"));
        ti.mem1 = this.memberRepo.save(new Member("mem1@collamap.com", "Paul", "TheMember"));
        ti.mem2 = this.memberRepo.save(new Member("mem2@collamap.com", "JAne", "TheMember"));
        ti.credAdmin = this.memberCredRepo.save(new MemberCredential(ti.admin, this.passwordEncoder.encode("pwd-admin")));
        ti.credMem1 = this.memberCredRepo.save(new MemberCredential(ti.mem1, this.passwordEncoder.encode("pwd-mem1")));
        ti.credMem2 = this.memberCredRepo.save(new MemberCredential(ti.mem2, this.passwordEncoder.encode("pwd-mem2")));
    }

    private void createComposWithMembers(TestInstances ti) {
        LOG.info("CREATE TEST COMPO WITH MEMBER ACCESS");

        ti.compAdmin1 = this.compoRepo.save(createComposition(
                "Compo Admin 1", false, ti.admin,
                createElement("CA1_1", "rect", null, 10, 10, Map.of("width", 100, "height", 200)),
                createElement("CA1_2", "text", null, 10, 10, Map.of("text", "Coucou"))
        ));
        ti.compMem1_1 = this.compoRepo.save(createComposition(
                "Compo Member 1 - 1", false, ti.mem1,
                createElement("CM1-1_1", "rect", null, 10, 10, Map.of("width", 100, "height", 200)),
                createElement("CM1-1_2", "text", null, 10, 10, Map.of("text", "Coucou"))
        ));
        ti.compMem1_2 = this.compoRepo.save(createComposition(
                "Compo Member 1 - 2", true, ti.mem1,
                createElement("CM1-2_1", "rect", null, 10, 10, Map.of("width", 100, "height", 200)),
                createElement("CM1-2_2", "text", null, 10, 10, Map.of("text", "Coucou"))
        ));
        ti.compMem2_1 = this.compoRepo.save(createComposition(
                "Compo Member 2 - 1", true, ti.mem2,
                createElement("CM2-1_1", "rect", null, 10, 10, Map.of("width", 100, "height", 200)),
                createElement("CM2-1_2", "text", null, 10, 10, Map.of("text", "Coucou"))
        ));

        // Add map members
        // compMem1_1 : admin and mem2 are guests
        long updated = this.compoRepo.findAndPushGuestById(ti.compMem1_1.getId(), ti.admin);
        assert updated == 1 : "compo Mem1 - 1 should have admin as guest";
        updated = this.compoRepo.findAndPushGuestById(ti.compMem1_1.getId(), ti.mem2);
        assert updated == 1 : "compo Mem1 - 1 should have mem2 as guest";

        // compMem1_2 : mem2 is guest
        updated = this.compoRepo.findAndPushGuestById(ti.compMem1_2.getId(), ti.mem2);
        assert updated == 1 : "compo Mem1 - 2 should have mem2 as guest";

        // comMem2_1 : admin is guest
        updated = this.compoRepo.findAndPushGuestById(ti.compMem2_1.getId(), ti.admin);
        assert updated == 1 : "compo Mem2 - 1 should have admin as guest";
    }

    public class TestInstances {

        private Member admin, mem1, mem2;
        private MemberCredential credAdmin, credMem1, credMem2;
        private Composition compAdmin1, compMem1_1, compMem1_2, compMem2_1;

        public Member getAdmin() {
            return admin;
        }

        public Member getMem1() {
            return mem1;
        }

        public Member getMem2() {
            return mem2;
        }

        public MemberCredential getCredAdmin() {
            return credAdmin;
        }

        public MemberCredential getCredMem1() {
            return credMem1;
        }

        public MemberCredential getCredMem2() {
            return credMem2;
        }

        public Composition getCompAdmin1() {
            return compAdmin1;
        }

        public Composition getCompMem1_1() {
            return compMem1_1;
        }

        public Composition getCompMem1_2() {
            return compMem1_2;
        }

        public Composition getCompMem2_1() {
            return compMem2_1;
        }

    }

    public static Member createMemberAsAdmin(String email, String firstname, String lastname) {
        Member m = new Member(email, firstname, lastname);
        m.setAdminFlag(Boolean.TRUE);
        return m;
    }

    public static Composition createComposition(String title, boolean collaborative, Member owner, CompositionElement... elements) {
        Composition compo = new Composition(title, collaborative, owner);
        compo.setElements(Arrays.asList(elements));
        return compo;
    }

    public static CompositionElement createElement(String id, String elementType, String style, double x, double y, Map<String, Object> extraProp) {
        CompositionElement ce = new CompositionElement(id, elementType, style, x, y);
        ce.setExtraProperties(extraProp);
        return ce;
    }
}
