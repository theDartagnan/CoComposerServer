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
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 *
 * @author Remi Venant
 */
public class CoComposerMemberDetails extends User implements Serializable {

    private final String email;

    public CoComposerMemberDetails(Member member, MemberCredential credential) {
        super(member.getId(), credential.getEncodedPassword(), computeAuthoritiesFromMember(member));
        this.email = member.getEmail();
    }

    public CoComposerMemberDetails(Member member, MemberCredential credential, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked) {
        super(member.getId(), credential.getEncodedPassword(), enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, computeAuthoritiesFromMember(member));
        this.email = member.getEmail();
    }

    private static Collection<? extends GrantedAuthority> computeAuthoritiesFromMember(Member member) {
        if (member.isAdmin()) {
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"));
        } else {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return "CoComposerMemberDetails{" + "email=" + email + ", username=" + this.getUsername() + '}';
    }

}
