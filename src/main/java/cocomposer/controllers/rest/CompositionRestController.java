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

import cocomposer.model.Composition;
import cocomposer.model.views.CompositionViews;
import cocomposer.security.authentification.CoComposerMemberDetails;
import cocomposer.services.CompositionService;
import cocomposer.services.MemberCompositionSummariesCollection;
import com.fasterxml.jackson.annotation.JsonView;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Remi Venant
 */
@RestController
@RequestMapping("/api/v1/rest/compositions")
public class CompositionRestController {
    private static final Log LOG = LogFactory.getLog(CompositionRestController.class);
    
    private final CompositionService compositionSvc;

    @Autowired
    public CompositionRestController(CompositionService compositionSvc) {
        this.compositionSvc = compositionSvc;
    }

    @GetMapping
    public MemberCompositionSummariesCollection getMyCompositions(@AuthenticationPrincipal CoComposerMemberDetails currentUser) {
        LOG.info("Access getMyCompositions");
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return this.compositionSvc.getUserCompositions(currentUser.getUsername());
    }

    @PostMapping
    @JsonView(CompositionViews.Normal.class)
    public Composition createComposition(@RequestBody Composition compositionInfo,
            @AuthenticationPrincipal CoComposerMemberDetails currentUser) {
        if (compositionInfo == null) {
            throw new IllegalArgumentException("Missing creation data");
        }
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return this.compositionSvc.createComposition(currentUser.getUsername(), compositionInfo);
    }

    @GetMapping("{compoId:[abcdef0-9]{24}}")
    @JsonView(CompositionViews.Details.class)
    public Composition getComposition(@PathVariable String compoId, @AuthenticationPrincipal CoComposerMemberDetails currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return this.compositionSvc.getComposition(compoId, currentUser.getUsername());
    }

    // Update title or collaborative indicator
    @PatchMapping("{compoId:[abcdef0-9]{24}}")
    public CompositionPatching patchComposition(@PathVariable String compoId, @RequestBody CompositionPatching compositionInfo) {
        if (compositionInfo.title != null && compositionInfo.collaborative != null) {
            throw new IllegalArgumentException("Cannot patch title and collaborative at the same time");
        }
        if (compositionInfo.title != null) {
            final String updatedTitle = this.compositionSvc.updateCompositionTitle(compoId, compositionInfo.title);
            return new CompositionPatching(updatedTitle, null);
        }
        if (compositionInfo.collaborative != null) {
            final boolean updatedCollaborative = this.compositionSvc.updateCompositionCollaborative(compoId, compositionInfo.collaborative);
            return new CompositionPatching(null, updatedCollaborative);
        }
        throw new IllegalArgumentException("Missing composition information to update.");
    }

    @DeleteMapping("{compoId:[abcdef0-9]{24}}")
    public void deleteComposition(@PathVariable String compoId) {
        this.compositionSvc.deleteComposition(compoId);
    }

    public static record CompositionPatching(String title, Boolean collaborative) {

    }
}
