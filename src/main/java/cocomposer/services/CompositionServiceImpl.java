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

import cocomposer.model.Composition;
import cocomposer.model.CompositionRepository;
import cocomposer.model.IdOnly;
import cocomposer.model.Member;
import cocomposer.model.MemberRepository;
import jakarta.validation.ConstraintViolationException;
import java.util.Collection;
import java.util.NoSuchElementException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 *
 * @author Remi Venant
 */
@Service
@Validated
public class CompositionServiceImpl implements CompositionService {

    private static final Log LOG = LogFactory.getLog(CompositionServiceImpl.class);

    private final CompositionRepository compoRepo;

    private final MemberRepository memberRepo;

    @Autowired
    public CompositionServiceImpl(CompositionRepository compoRepo, MemberRepository memberRepo) {
        this.compoRepo = compoRepo;
        this.memberRepo = memberRepo;
    }

    @Override
    public MemberCompositionSummariesCollection getUserCompositions(String userId) throws AccessDeniedException, ConstraintViolationException {
        // Encapsulate userId in member instance to access queries
        final Member member = Member.asMember(userId);
        // Execute the different queries to expose all compositions the user belongs to
        return new MemberCompositionSummariesCollection(
                this.compoRepo.findSummaryByOwner(member).toList(),
                this.compoRepo.findSummaryByGuests(member).toList()
        );
    }

    @Override
    public Composition getComposition(String compoId, String userId) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException {
        // Update the composition with the user as guest if it is not the case. Assume the composition exists
        if (!this.compoRepo.existsByIdAndOwnerOrGuestsId(compoId, userId)) {
            this.compoRepo.findAndPushGuestById(compoId, Member.asMember(userId));
        }
        // Retrieve and return the composition. Throw exception if it does not exist
        return this.compoRepo.findById(compoId)
                .orElseThrow(() -> new NoSuchElementException("Composition does not exist or is not available to the user"));

    }

    @Override
    public Composition createComposition(String ownerUserId, Composition compositionInfo) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException {
        // Retrieve and Check that owner exists
        final Member owner = this.memberRepo.findById(ownerUserId).orElseThrow(() -> new NoSuchElementException("Unexisting user"));
        // Create proper composition to ensure properties are propery set
        Composition composition = new Composition(compositionInfo.getTitle(), compositionInfo.isCollaborative(), owner);
        // Create the composition. May raise ConstraintViolationException
        composition = this.compoRepo.save(composition);
        return composition;
    }

    @Override
    public String updateCompositionTitle(String compoId, String newTitle) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException {
        // Attempt to set the title
        long res = this.compoRepo.findAndSetTitleById(compoId, newTitle);
        if (res < 1) {
            throw new NoSuchElementException("Unknown composition");
        }
        return newTitle;
    }

    @Override
    public boolean updateCompositionCollaborative(String compoId, boolean collaborative) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException {
        // Attempt to set the collaborative indicator
        long res = this.compoRepo.findAndSetCollaborativeById(compoId, collaborative);
        if (res < 1) {
            throw new NoSuchElementException("Unknown composition");
        }
        return collaborative;
    }

    @Override
    public void deleteComposition(String compoId) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException {
        // Check the composition exists)
        if (!this.compoRepo.existsById(compoId)) {
            throw new NoSuchElementException("Unknown composition");
        }

        // TODO: Inform and disconnect active users on this map before deleting it
        // Delete the map
        this.compoRepo.deleteById(compoId);
    }

    @Override
    public long deleteAllOwnedCompositionsFromUser(String userId) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException {
        // Retrieve all owned composition ids
        final Collection<String> ownedCompositionIds = this.compoRepo
                .findCompositionIdsByOwner(Member.asMember(userId)).map(IdOnly::id).toList();
        // if collection empty, stop here
        if (ownedCompositionIds.isEmpty()) {
            return 0L;
        }

        // Create a collection of ConceptMap wrapper
        //final Collection<Composition> ownedCompositions = ownedCompositionIds.stream().map(Composition::asComposition).toList();
        // TODO: Inform and disconnect active users from any of thes maps
        // Delete the maps
        return this.compoRepo.deleteByIdIn(ownedCompositionIds);
    }

}
