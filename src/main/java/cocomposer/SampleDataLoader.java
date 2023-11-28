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
package cocomposer;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 *
 * @author Remi Venant
 */
@Profile("sample-data")
@Component
public class SampleDataLoader implements CommandLineRunner {

    private static final Log LOG = LogFactory.getLog(SampleDataLoader.class);
    
    private final MemberRepository memberRepo;
    
    private final MemberCredentialRepository memberCredRepo;
    
    private final PasswordEncoder passwordEncoder;
    
    private final CompositionRepository compoRepo;
    
    @Autowired
    public SampleDataLoader(MemberRepository memberRepo, MemberCredentialRepository memberCredRepo, PasswordEncoder passwordEncoder, CompositionRepository compoRepo) {
        this.memberRepo = memberRepo;
        this.memberCredRepo = memberCredRepo;
        this.passwordEncoder = passwordEncoder;
        this.compoRepo = compoRepo;
    }
    
    @Override
    public void run(String... args) throws Exception {
        LOG.info("LOAD SAMPLE DATA");
        this.clear();
        final MemberInstances members = this.createMembersWithCred();
        this.createComposWithMembers(members);
        LOG.info("END LOAD SAMPLE DATA");
    }
    
    private void clear() {
        LOG.info("Clear all database");
        this.compoRepo.deleteAll();
        this.memberCredRepo.deleteAll();
        this.memberRepo.deleteAll();
    }
    
    private MemberInstances createMembersWithCred() {
        LOG.info("Create test members and credentials");
        MemberInstances mi = new MemberInstances();
        mi.admin = this.memberRepo.save(createMemberAsAdmin("admin@collamap.com", "John", "TheAdmin"));
        mi.mem1 = this.memberRepo.save(new Member("mem1@collamap.com", "Paul", "TheMember"));
        mi.mem2 = this.memberRepo.save(new Member("mem2@collamap.com", "JAne", "TheMember"));
        this.memberCredRepo.save(new MemberCredential(mi.admin, this.passwordEncoder.encode("pwd-admin")));
        this.memberCredRepo.save(new MemberCredential(mi.mem1, this.passwordEncoder.encode("pwd-mem1")));
        this.memberCredRepo.save(new MemberCredential(mi.mem2, this.passwordEncoder.encode("pwd-mem2")));
        return mi;
    }
    
    private void createComposWithMembers(MemberInstances members) {
        LOG.info("Create test composition with member access");
        Composition compAdmin1 = this.compoRepo.save(createComposition(
                "Compo Admin 1", false, members.admin,
                createElement("CA1_1", "rect", null, 10, 10, Map.of("width", 100, "height", 200)),
                createElement("CA1_2", "text", null, 10, 10, Map.of("text", "Coucou"))
        ));
        Composition compMem1_1 = this.compoRepo.save(createComposition(
                "Compo Member 1 - 1", false, members.mem1,
                createElement("CM1-1_1", "rect", null, 10, 10, Map.of("width", 100, "height", 200)),
                createElement("CM1-1_2", "text", null, 10, 10, Map.of("text", "Coucou"))
        ));
        Composition compMem1_2 = this.compoRepo.save(createComposition(
                "Compo Member 1 - 2", true, members.mem1,
                createElement("CM1-2_1", "rect", null, 10, 10, Map.of("width", 100, "height", 200)),
                createElement("CM1-2_2", "text", null, 10, 10, Map.of("text", "Coucou"))
        ));
        Composition compMem2_1 = this.compoRepo.save(createComposition(
                "Compo Member 2 - 1", true, members.mem2,
                createElement("CM2-1_1", "rect", null, 10, 10, Map.of("width", 100, "height", 200)),
                createElement("CM2-1_2", "text", null, 10, 10, Map.of("text", "Coucou"))
        ));

        // Add map members
        // compMem1_1 : admin and mem2 are guests
        long updated = this.compoRepo.findAndPushGuestById(compMem1_1.getId(), members.admin);
        assert updated == 1 : "compo Mem1 - 1 should have admin as guest";
        updated = this.compoRepo.findAndPushGuestById(compMem1_1.getId(), members.mem2);
        assert updated == 1 : "compo Mem1 - 1 should have mem2 as guest";

        // compMem1_2 : mem2 is guest
        updated = this.compoRepo.findAndPushGuestById(compMem1_2.getId(), members.mem2);
        assert updated == 1 : "compo Mem1 - 2 should have mem2 as guest";

        // comMem2_1 : admin is guest
        updated = this.compoRepo.findAndPushGuestById(compMem2_1.getId(), members.admin);
        assert updated == 1 : "compo Mem2 - 1 should have admin as guest";
    }
    
    private static Member createMemberAsAdmin(String email, String firstname, String lastname) {
        Member m = new Member(email, firstname, lastname);
        m.setAdminFlag(Boolean.TRUE);
        return m;
    }
    
    private static Composition createComposition(String title, boolean collaborative, Member owner, CompositionElement... elements) {
        Composition compo = new Composition(title, collaborative, owner);
        compo.setElements(Arrays.asList(elements));
        return compo;
    }
    
    private static CompositionElement createElement(String id, String elementType, String style, double x, double y, Map<String, Object> extraProp) {
        CompositionElement ce = new CompositionElement(id, elementType, style, x, y);
        ce.setExtraProperties(extraProp);
        return ce;
    }
    
    private static class MemberInstances {
        
        private Member admin, mem1, mem2;
    }
}
