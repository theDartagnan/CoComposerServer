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
package cocomposer.model.compositionOrder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.LocalDateTime;

/**
 *
 * @author Remi Venant
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "orderType", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CompositionTitleChangedOrder.class, name = "compositiontitleChanged"),
    @JsonSubTypes.Type(value = CompositionCollaborativeChangedOrder.class, name = "compositionCollaborativeChanged"),
    @JsonSubTypes.Type(value = CompositionDeletedOrder.class, name = "compositionDeleted"),
    @JsonSubTypes.Type(value = ElementAddedOrder.class, name = "elementAdded"),
    @JsonSubTypes.Type(value = ElementChangedOrder.class, name = "elementChanged"),
    @JsonSubTypes.Type(value = ElementPositionChangedOrder.class, name = "elementPositionChanged"),
    @JsonSubTypes.Type(value = ElementDeletedOrder.class, name = "elementDeleted"),})
public class CompositionOrder {

    private String orderType;

    private String compositionId;

    private String authorEmail;

    private LocalDateTime orderDatetime;

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getCompositionId() {
        return compositionId;
    }

    public void setCompositionId(String compositionId) {
        this.compositionId = compositionId;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public LocalDateTime getOrderDatetime() {
        return orderDatetime;
    }

    public void setOrderDatetime(LocalDateTime orderDatetime) {
        this.orderDatetime = orderDatetime;
    }

}
