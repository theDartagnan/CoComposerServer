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
package cocomposer.security.authentification;

import cocomposer.model.Member;
import cocomposer.model.MemberCredential;
import java.util.Objects;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 *
 * @author Remi Venant
 */
public class CoComposerDetailsService implements UserDetailsService {

    private static final Log LOG = LogFactory.getLog(CoComposerDetailsService.class);

    private final MongoTemplate mongoTemplate;

    public CoComposerDetailsService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Fetch member in db
        final Member member = this.findMemberByEmail(email);
        LOG.info("Fetch member for email " + email + " : " + Objects.toString(member));
        if (member != null) {
            // if member found fetch memberCred
            final MemberCredential cred = this.findMemberCredential(member);
            LOG.info("Fetch cred for email " + email + " : " + Objects.toString(cred));
            if (cred != null) {
                // if member cred found, create user details.
                return new CoComposerMemberDetails(member, cred);
            }
        }
        throw new UsernameNotFoundException("User not found or no credential");
    }

    private Member findMemberByEmail(String email) {
        if (email == null || email.isEmpty()) {
            return null;
        }
        try {
            // Get member by email, may return null if not found
            return this.mongoTemplate
                    .findOne(Query.query(Criteria.where("email").is(email)), Member.class);
        } catch (Exception ex) {
            LOG.error("Error fetching member with mail \"" + email + "\"", ex);
            return null;
        }
    }

    private MemberCredential findMemberCredential(Member member) {
        if (member == null || member.getId() == null) {
            return null;
        }
        try {
            // Get memberCred by member. May return null
            return this.mongoTemplate
                    .findOne(Query.query(Criteria.where("member").is(member)), MemberCredential.class);
        } catch (Exception ex) {
            LOG.error("Error fetching memberCred with member \"" + member.getEmail() + "\"", ex);
            return null;
        }
    }
}
