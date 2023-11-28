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

import cocomposer.model.views.MemberViews;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 *
 * @author Remi Venant
 */
@Document(collection = "members")
public class Member {

    @JsonView(MemberViews.Normal.class)
    @Id
    private String id;

    // REQUIRE MANUAL VALIDATION OF CONTENT
    @JsonView(MemberViews.Normal.class)
    @NotBlank
    @Email
    @Size(min = 1, max = 100)
    @Indexed(unique = true)
    private String email;

    // REQUIRE MANUAL VALIDATION OF CONTENT
    @JsonView(MemberViews.Normal.class)
    @NotBlank
    @Size(min = 1, max = 100)
    private String firstname;

    // REQUIRE MANUAL VALIDATION OF CONTENT
    @JsonView(MemberViews.Normal.class)
    @NotBlank
    @Size(min = 1, max = 100)
    private String lastname;

    @JsonView(MemberViews.Administrative.class)
    private Boolean adminFlag;

    protected Member() {
    }

    public Member(String email, String firstname, String lastname) {
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
    }

    public String getId() {
        return id;
    }

    protected void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    @JsonView(MemberViews.Normal.class)
    public boolean isAdmin() {
        return Boolean.TRUE.equals(this.adminFlag);
    }

    public void setAdminFlag(Boolean adminFlag) {
        this.adminFlag = adminFlag;
    }

    @Override
    public String toString() {
        return "Member{" + "email=" + email + ", firstname=" + firstname + ", lastname=" + lastname + ", adminFlag=" + adminFlag + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Member other = (Member) obj;
        return Objects.equals(this.id, other.id);
    }

    public static Member asMember(String userId) {
        Member m = new Member();
        m.setId(userId);
        return m;
    }
}
