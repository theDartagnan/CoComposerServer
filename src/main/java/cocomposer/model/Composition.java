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

import cocomposer.model.views.CompositionViews;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

/**
 *
 * @author Remi Venant
 */
@Document(collection = "compositions")
public class Composition {

    @JsonView(CompositionViews.Normal.class)
    @Id
    private String id;

    // REQUIRE MANUAL VALIDATION OF CONTENT
    @JsonView(CompositionViews.Normal.class)
    @NotBlank
    @Size(min = 5, max = 150)
    private String title;

    @JsonView(CompositionViews.Normal.class)
    private boolean collaborative;

    @JsonView(CompositionViews.Details.class)
    @Valid
    private List<CompositionElement> elements;

    @JsonView(CompositionViews.Details.class)
    @NotNull
    @DocumentReference
    private Member owner;

    @JsonView(CompositionViews.Details.class)
    @DocumentReference(lazy = true)
    private List<Member> guests;

    @JsonView(CompositionViews.Details.class)
    @LastModifiedDate
    private LocalDateTime updateDatetime;

    protected Composition() {
    }

    public Composition(String title, boolean collaborative, Member owner) {
        this.title = title;
        this.collaborative = collaborative;
        this.owner = owner;
    }

    public String getId() {
        return id;
    }

    protected void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCollaborative() {
        return collaborative;
    }

    public void setCollaborative(boolean collaborative) {
        this.collaborative = collaborative;
    }

    public List<CompositionElement> getElements() {
        return this.elements == null ? List.of() : Collections.unmodifiableList(this.elements);
    }

    public void setElements(List<CompositionElement> elements) {
        this.elements = elements;
    }

    public Member getOwner() {
        return owner;
    }

    public void setOwner(Member owner) {
        this.owner = owner;
    }

    public List<Member> getGuests() {
        return this.guests == null ? List.of() : Collections.unmodifiableList(this.guests);
    }

    public void setGuests(List<Member> guests) {
        this.guests = guests;
    }

    // Proposed only for testing purpose
    protected void addGuest(Member editor) {
        if (this.guests == null) {
            this.guests = new ArrayList<>();
            this.guests.add(editor);
        } else if (!this.guests.contains(editor)) {
            this.guests.add(editor);
        }
    }

    public LocalDateTime getUpdateDatetime() {
        return updateDatetime;
    }

    public void setUpdateDatetime(LocalDateTime updateDatetime) {
        this.updateDatetime = updateDatetime;
    }

    @Override
    public String toString() {
        return "Composition{" + "title=" + title + ", collaborative=" + collaborative + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.id);
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
        final Composition other = (Composition) obj;
        return Objects.equals(this.id, other.id);
    }

    public static Composition asComposition(String mapId) {
        Composition compo = new Composition();
        compo.setId(mapId);
        return compo;
    }
}
