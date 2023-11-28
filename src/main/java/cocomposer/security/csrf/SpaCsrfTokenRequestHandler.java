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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

/**
 *
 * @author Remi Venant
 */
public class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {

    private static final Log LOG = LogFactory.getLog(CsrfCookieFilter.class);

    private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<org.springframework.security.web.csrf.CsrfToken> deferredCsrfToken) {
        /*
        * Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection of
        * the CsrfToken when it is rendered in the response body.
         */
        this.delegate.handle(request, response, deferredCsrfToken);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, org.springframework.security.web.csrf.CsrfToken csrfToken) {
        /*
        * If the request contains a request header, use CsrfTokenRequestAttributeHandler
        * to resolve the CsrfToken. This applies when a single-page application includes
        * the header value automatically, which was obtained via a cookie containing the
        * raw CsrfToken.
         */
        if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
            LOG.debug("Handle CSRF for header for request " + request.getRequestURI());
            return super.resolveCsrfTokenValue(request, csrfToken);
        }
        /*
        * In all other cases (e.g. if the request contains a request parameter), use
        * XorCsrfTokenRequestAttributeHandler to resolve the CsrfToken. This applies
        * when a server-side rendered form includes the _csrf request parameter as a
        * hidden input, or when it is provided in a STOMP header....
         */
        return this.delegate.resolveCsrfTokenValue(request, csrfToken);
    }

}
