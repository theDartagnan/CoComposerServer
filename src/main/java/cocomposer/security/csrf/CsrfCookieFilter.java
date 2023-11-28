/*
 * Copyright (C) 2023 Remi Venant.
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
package cocomposer.security.csrf;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 *
 * @author Remi Venant
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    private static final Log LOG = LogFactory.getLog(CsrfCookieFilter.class);

    public CsrfCookieFilter() {

    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Récupère le deffered token de la requête
        CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
        // Provoque la résolution du token : comme celui-ci est dans un cookie, il n'y a pas de chute de perf (pas d'accès session)
        // la résolution du token provoquera la création d'un nouveau au besoin 
        csrfToken.getToken();

        filterChain.doFilter(request, response);
    }
}
