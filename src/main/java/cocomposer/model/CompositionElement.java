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
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author Remi Venant
 */
public class CompositionElement {

    @JsonView(CompositionViews.Details.class)
    @NotBlank
    @Pattern(regexp = "[\\-\\w\\#]+", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String id;

    @JsonView(CompositionViews.Details.class)
    @NotBlank
    private String elementType;

    @JsonView(CompositionViews.Details.class)
    private String style;

    @JsonView(CompositionViews.Details.class)
    private double x;

    @JsonView(CompositionViews.Details.class)
    private double y;

    private Map<String, Object> extraProperties;

    protected CompositionElement() {
    }

    public CompositionElement(String id, String elementType, String style, double x, double y) {
        this.id = id;
        this.elementType = elementType;
        this.style = style;
        this.x = x;
        this.y = y;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getElementType() {
        return elementType;
    }

    protected void setElementType(String elementType) {
        this.elementType = elementType;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    @JsonAnyGetter
    @JsonView(CompositionViews.Details.class)
    public Map<String, Object> getExtraProperties() {
        return extraProperties;
    }

    public void setExtraProperties(Map<String, Object> extraProperties) {
        this.extraProperties = extraProperties;
    }

    @JsonAnySetter
    public void addExtraProperties(String key, Object value) {
        if (this.extraProperties == null) {
            this.extraProperties = new HashMap<>();
        }
        Object correctedValue;
        if (value == null || value instanceof String || value instanceof Integer || value instanceof Float || value instanceof Double || value instanceof Boolean) {
            correctedValue = value;
        } else {
            correctedValue = value.toString();
        }
        this.extraProperties.put(key, correctedValue);
    }

    @Override
    public String toString() {
        return "CompositionElement{" + "style=" + style + ", x=" + x + ", y=" + y + ", num extraProperties=" + (extraProperties == null ? "null" : extraProperties.size()) + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
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
        final CompositionElement other = (CompositionElement) obj;
        return Objects.equals(this.id, other.id);
    }

}
