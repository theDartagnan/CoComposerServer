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
import cocomposer.model.Member;
import cocomposer.model.MemberRepository;
import cocomposer.services.websocket.CompositionWSService;
import jakarta.validation.ConstraintViolationException;
import java.util.Collection;
import java.util.List;
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

    private final CompositionWSService compositionWSSvc;

    @Autowired
    public CompositionServiceImpl(CompositionRepository compoRepo, MemberRepository memberRepo,
            CompositionWSService compositionWSSvc) {
        this.compoRepo = compoRepo;
        this.memberRepo = memberRepo;
        this.compositionWSSvc = compositionWSSvc;
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
            if (this.compoRepo.findAndPushGuestById(compoId, Member.asMember(userId)) < 1) {
                LOG.warn("Guest not added!");
            }
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
    public String updateCompositionTitlePersonnal(String compoId, String newTitle) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException {
        // Attempt to set the title
        long res = this.compoRepo.findAndSetTitleById(compoId, newTitle);
        if (res < 1) {
            throw new NoSuchElementException("Unknown composition");
        }
        // update composition title handle by websocket controller
        return newTitle;
    }

    @Override
    public String updateCompositionTitleCollaborative(String compoId, String newTitle) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException {
        return this.updateCompositionTitlePersonnal(compoId, newTitle);
        // Topic messages handle by websocket controller
    }

    @Override
    public boolean updateCompositionCollaborativePersonnal(String compoId, boolean collaborative) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException {
        // Attempt to set the collaborative indicator
        long res = this.compoRepo.findAndSetCollaborativeById(compoId, collaborative);
        if (res < 1) {
            throw new NoSuchElementException("Unknown composition");
        }
        this.compositionWSSvc.informCompositionCollaborativeChanged(compoId, collaborative);
        return collaborative;
    }

    @Override
    public boolean updateCompositionCollaborativeCollaborative(String compoId, boolean collaborative) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException {
        boolean res = this.updateCompositionCollaborativePersonnal(compoId, collaborative);
        // Topic messages handle by websocket controller
        return res;
    }

    @Override
    public void deleteComposition(String compoId) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException {
        // Check the composition exists : retrieve it as we need the guest list)
        final Composition composition = this.compoRepo.findById(compoId)
                .orElseThrow(() -> new NoSuchElementException("Unknown composition"));

        // Inform and disconnect active users on this map before deleting it
        this.compositionWSSvc.informCompositionWillBeDeleted(composition);
        // Delete the map
        this.compoRepo.deleteById(compoId);
    }

    @Override
    public long deleteAllOwnedCompositionsFromUser(String userId) throws AccessDeniedException, ConstraintViolationException, NoSuchElementException {
        // Retrieve all owned composition
        final Collection<Composition> ownedCompositions = this.compoRepo
                .findCompositionsByOwner(Member.asMember(userId)).toList();
        if (ownedCompositions.isEmpty()) {
            return 0L;
        }
        final List<String> compositionIds = ownedCompositions.stream().map(Composition::getId).toList();
        //  Inform and disconnect active users from any of thes maps
        ownedCompositions.forEach((composition) -> this.compositionWSSvc.informCompositionWillBeDeleted(composition));
        // Delete the maps
        return this.compoRepo.deleteByIdIn(compositionIds);
    }

}
