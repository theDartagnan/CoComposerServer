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

import cocomposer.model.CompositionElement;
import cocomposer.model.CompositionRepository;
import jakarta.validation.ConstraintViolationException;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 *
 * @author Remi Venant
 */
@Service
@Validated
public class CompositionElementServiceImpl implements CompositionElementService {

    private static final Log LOG = LogFactory.getLog(CompositionServiceImpl.class);

    private final CompositionRepository compoRepo;

    @Autowired
    public CompositionElementServiceImpl(CompositionRepository compoRepo) {
        this.compoRepo = compoRepo;
    }

    @Override
    public CompositionElement addElementPersonnal(String compoId, CompositionElement elementInfo) throws AccessDeniedException, ConstraintViolationException, DuplicateKeyException, NoSuchElementException {
        // Find a proper candidate for an id
        String elementKey;
        do {
            elementKey = UUID.randomUUID().toString();
        } while (this.compoRepo.existsByIdAndElementsId(compoId, elementKey));
        elementInfo.setId(elementKey);
        // Add the element. If the update cannot be made, composition does not exist
        long res = this.compoRepo.findAndPushElementById(compoId, elementInfo);
        if (res < 1) {
            throw new NoSuchElementException("Composition not found");
        }
        return elementInfo;
    }

    @Override
    public CompositionElement addElementCollaborative(String compoId, CompositionElement elementInfo) throws AccessDeniedException, ConstraintViolationException, DuplicateKeyException, NoSuchElementException {
        CompositionElement elem = this.addElementPersonnal(compoId, elementInfo);
        // TODO: Send information on websocket
        return elem;
    }

    @Override
    public CompositionElement updateElementPersonnal(String compoId, CompositionElement elementInfo) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException {
        // TODO : add a test where elementInfo.id is null or blank

        // Update the element. If the update cannot be made, composition or element does not exist
        long res = this.compoRepo.findAndSetElementByIdAndElementsId(compoId, elementInfo);
        if (res < 1) {
            // Check the existence of compo and elementInfo to make the distinction between missing data or no modification
            if (this.compoRepo.existsByIdAndElementsId(compoId, elementInfo.getId())) {
                return elementInfo;
            } else {
                throw new NoSuchElementException("Composition or element in composition not found");
            }
        }
        
        return elementInfo;
    }
    
    @Override
    public CompositionElement updateElementCollaborative(String compoId, CompositionElement elementInfo) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException {
        CompositionElement updatedElement = this.updateElementPersonnal(compoId, elementInfo);
        // TODO: Send information on websocket
        return updatedElement;
    }

    @Override
    public void updateElementPositionPersonnal(String compoId, String elementId, double x, double y) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException {
        // Set the element position. If the update cannot be made, composition or element does not exist
        long res = this.compoRepo.findAndSetElementPositionByIdAndElementsId(compoId, elementId, x, y);
        if (res < 1) {
            // Check the existence of compo and elementInfo to make the distinction between missing data or no modification
            if (!this.compoRepo.existsByIdAndElementsId(compoId, elementId)) {
                throw new NoSuchElementException("Composition or element in composition not found");
            }
        }
    }

    @Override
    public void updateElementPositionCollaborative(String compoId, String elementId, double x, double y) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException {
        this.updateElementPositionPersonnal(compoId, elementId, x, y);
        // TODO: Send information on websocket
    }

    @Override
    public void deleteElementPersonnal(String compoId, String elementId) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException {
        // Delete the element. If the update cannot be made, composition or element does not exist
        long res = this.compoRepo.findAndPullElementById(compoId, elementId);
        if (res < 1) {
            throw new NoSuchElementException("Composition or element in composition not found");
        }
    }

    @Override
    public void deleteElementCollaborative(String compoId, String elementId) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException {
        this.deleteElementPersonnal(compoId, elementId);
        // TODO: Send information on websocket
    }

}
