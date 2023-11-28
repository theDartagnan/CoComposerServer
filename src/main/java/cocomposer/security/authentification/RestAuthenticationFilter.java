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
package cocomposer.security.authentification;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 *
 * @author Remi Venant
 */
public class RestAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private static final Log LOG = LogFactory.getLog(RestAuthenticationFilter.class);

    public RestAuthenticationFilter() {
        this.setPostOnly(true);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        //Check content type
        List<MediaType> requestMediaTypes = MediaType.parseMediaTypes(request.getContentType());
        if (requestMediaTypes.stream().noneMatch((mt) -> mt.isCompatibleWith(MediaType.APPLICATION_JSON))) {
            try {
                LOG.debug("Bad media type. Media types : " + requestMediaTypes.toString());
                response.sendError(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
            } catch (IOException ex) {
                LOG.warn("Error while sending error to client: " + ex.getMessage());
            }
            return null;
        }
        //Deserialize content
        try {
            ObjectMapper om = new ObjectMapper();
            LoginCredentials lc = om.readValue(request.getInputStream(), LoginCredentials.class);
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(lc.username, lc.password);
            return this.getAuthenticationManager().authenticate(token);
        } catch (IOException ex) {
            throw new AuthenticationCredentialsNotFoundException("Bad credential format");
        }
    }
    
    public static record LoginCredentials(String username, String password) {
        
    }
}
