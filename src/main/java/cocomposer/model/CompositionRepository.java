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

import java.util.Collection;
import java.util.stream.Stream;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

/**
 *
 * @author Remi Venant
 */
public interface CompositionRepository extends MongoRepository<Composition, String> {

    boolean existsByIdAndOwnerId(String compoId, String ownerId);
    
    @Query(value = "{id: ?0, $or: [{owner: ?#{new org.bson.types.ObjectId([1])} }, {guests: ?#{new org.bson.types.ObjectId([1])} } ]}", exists = true)
    boolean existsByIdAndOwnerOrGuestsId(String compoId, String userId);
    
    @Query(value = "{id: ?0, $or: [{owner: ?#{new org.bson.types.ObjectId([1])} }, {collaborative: true, guests: ?#{new org.bson.types.ObjectId([1])} } ]}", exists = true)
    boolean canUserEditCompo(String compoId, String guestId);
    
    boolean existsByIdAndElementsId(String compoId, String elementId);

    Stream<CompositionSummary> findSummaryByOwner(Member owner);

    Stream<CompositionSummary> findSummaryByGuests(Member guest);
    
    Stream<IdOnly> findCompositionIdsByOwner(Member owner);

    long deleteByIdIn(Collection<String> compoIds);
    
    @Update("{ '$set' : { 'title' : ?1 } }")
    long findAndSetTitleById(String compoId, String title);
    
    @Update("{ '$set' : { 'collaborative' : ?1 } }")
    long findAndSetCollaborativeById(String compoId, boolean collaborative);

    /**
     * Add a guest to the composition
     *
     * @param compoId
     * @param editor
     * @return 1 if the map has been found with the proper owner and updated, 0
     * otherwise
     */
    @Update("{ '$addToSet' : { 'guests' : ?#{new org.bson.types.ObjectId([1].id)} } }")
    long findAndPushGuestById(String compoId, Member editor);
    
    /**
     * Add an element to the composition. Does not assert unicity of element id!
     *
     * @param compoId
     * @param element
     * @return 1 if the map has been found with the proper owner and updated, 0
     * otherwise
     */
    @Update("{ '$push' : { 'elements' : ?1 } }")
    long findAndPushElementById(String compoId, CompositionElement element);
    
    @Query("{id: ?0, 'elements.id': ?#{[1].id} }")
    @Update("{ '$set': { 'elements.$': ?1 } }")
    long findAndSetElementByIdAndElementsId(String compoId, CompositionElement element);
    
    @Query("{id: ?0, 'elements.id': ?1 }")
    @Update("{ '$set': { 'elements.$.x': ?2, 'elements.$.y': ?3 } }")
    long findAndSetElementPositionByIdAndElementsId(String compoId, String elementId, double x, double y);
    
    @Update("{ '$pull' : { 'elements' : { id: ?1 } } }")
    long findAndPullElementById(String compoId, String elementId);
}
