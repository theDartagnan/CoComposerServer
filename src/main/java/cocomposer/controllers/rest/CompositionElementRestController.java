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
package cocomposer.controllers.rest;

import cocomposer.model.CompositionElement;
import cocomposer.model.views.CompositionViews;
import cocomposer.services.CompositionElementService;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.HashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Remi Venant
 */
@RestController
@RequestMapping("/api/v1/rest/compositions/{compoId:[abcdef0-9]{24}}/elements")
public class CompositionElementRestController {

    private static final Log LOG = LogFactory.getLog(CompositionElementRestController.class);

    private final CompositionElementService compoElemSvc;

    @Autowired
    public CompositionElementRestController(CompositionElementService compoElemSvc) {
        this.compoElemSvc = compoElemSvc;
    }

    @PostMapping
    @JsonView(CompositionViews.Details.class)
    public CompositionElement createElement(@PathVariable String compoId, @RequestBody CompositionElement elementInfo) {
        if (elementInfo == null) {
            throw new IllegalArgumentException("Missing creation data");
        }
        return this.compoElemSvc.addElementPersonnal(compoId, elementInfo);
    }

    @PutMapping("{elemId:[\\-\\w\\#]{4,50}}")
    @JsonView(CompositionViews.Details.class)
    public CompositionElement updateElement(@PathVariable String compoId, @PathVariable String elemId, @RequestBody CompositionElement elementInfo) {
        if (elementInfo == null) {
            throw new IllegalArgumentException("Missing update data");
        }
        CompositionElement correctedElementInfo = elementInfo;
        if (elementInfo.getId() == null) {
            correctedElementInfo = new CompositionElement(elemId, elementInfo.getElementType(), elementInfo.getStyle(), elementInfo.getX(), elementInfo.getY());
            if (elementInfo.getExtraProperties() != null) {
                HashMap<String, Object> extraProp = new HashMap<>(elementInfo.getExtraProperties());
                correctedElementInfo.setExtraProperties(extraProp);
            }
        } else if (!elemId.equals(elementInfo.getId())) {
            throw new IllegalArgumentException("Unconsistent update data with element id");
        }
        return this.compoElemSvc.updateElementPersonnal(compoId, correctedElementInfo);
    }

    @PutMapping("{elemId:[\\-\\w\\#]{4,50}}/position")
    public ElementPositon updateElementPosition(@PathVariable String compoId, @PathVariable String elemId, @RequestBody ElementPositon position) {
        if (position == null) {
            throw new IllegalArgumentException("Missing position data");
        }
        this.compoElemSvc.updateElementPositionPersonnal(compoId, elemId, position.x, position.y);
        return position;
    }

    @DeleteMapping("{elemId:[\\-\\w\\#]{4,50}}")
    public ResponseEntity deleteElement(@PathVariable String compoId, @PathVariable String elemId) {
        this.compoElemSvc.deleteElementPersonnal(compoId, elemId);
        return ResponseEntity.noContent().build();
    }

    public static record ElementPositon(double x, double y) {

    }
}
