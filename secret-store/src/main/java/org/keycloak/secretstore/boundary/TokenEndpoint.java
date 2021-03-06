/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.secretstore.boundary;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.secretstore.api.Token;
import org.keycloak.secretstore.api.TokenService;
import org.keycloak.secretstore.common.UsernamePasswordConverter;
import org.keycloak.secretstore.common.ZonedDateTimeAdapter;
import org.keycloak.secretstore.entity.TokenCreateUpdateRequest;
import org.keycloak.secretstore.entity.TokenResponse;
import org.keycloak.secretstore.entity.TokenErrorResponse;

/**
 * @author Juraci Paixão Kröhling
 */
@Path("tokens")
@Stateless
@PermitAll
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TokenEndpoint {
    @SuppressWarnings("EjbEnvironmentInspection")
    @Resource
    SessionContext sessionContext;

    @Context
    HttpServletRequest request;

    @Context
    ServletContext servletContext;

    @Inject
    TokenService tokenService;

    @Context
    UriInfo uriInfo;

    @Inject
    UsernamePasswordConverter usernamePasswordConverter;

    @Inject
    ZonedDateTimeAdapter zonedDateTimeAdapter;

    @GET
    @Path("/")
    public Response listMyTokens() {
        String principal = sessionContext.getCallerPrincipal().getName();
        List<Token> tokens = tokenService.getByPrincipalForDistribution(principal);
        List<TokenResponse> response = tokens
                .stream()
                .map(TokenResponse::new)
                .collect(Collectors.toList());
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{tokenId}")
    public Response revoke(@PathParam("tokenId") String tokenId) {
        String principal = sessionContext.getCallerPrincipal().getName();
        Token token = tokenService.getByIdForTrustedConsumers(UUID.fromString(tokenId));
        if (token != null && principal.equals(token.getPrincipal())) {
            tokenService.revoke(UUID.fromString(tokenId));
            return Response.noContent().build();
        } else {
            // either the token really was not found, or doesn't belong to the user
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * This endpoint is called when Keycloak redirects the logged in user to our application.
     *
     * @return a response with a TokenCreateResponse as entity.
     */
    @GET
    @Path("create")
    public Response createFromRedirect() {
        KeycloakPrincipal principal = (KeycloakPrincipal) sessionContext.getCallerPrincipal();
        RefreshableKeycloakSecurityContext kcSecurityContext = (RefreshableKeycloakSecurityContext)
                principal.getKeycloakSecurityContext();

        String refreshToken = kcSecurityContext.getRefreshToken();
        Token token = create(refreshToken);
        try {
            String redirectTo = System.getProperty("secretstore.redirectTo");

            if (null == redirectTo || redirectTo.isEmpty()) {
                return Response.ok("Redirect URL was not specified but token was created.").build();
            }

            URI location;
            if (redirectTo.toLowerCase().startsWith("http")) {
                location = new URI(redirectTo);
            } else {
                URI uri = uriInfo.getAbsolutePath();
                String newPath = redirectTo.replace("{tokenId}", token.getId().toString());
                location = new URI(
                        uri.getScheme(),
                        uri.getUserInfo(),
                        uri.getHost(),
                        uri.getPort(),
                        newPath,
                        uri.getQuery(),
                        uri.getFragment()
                );
            }

            return Response.seeOther(location).build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return Response.ok("Could not redirect back to the original URL, but token was created.").build();
        }
    }

    /**
     * This endpoint is called when a client makes a REST call with basic auth as APPLICATION_FORM_URLENCODED, without
     * parameters.
     *
     * @return a response with a TokenCreateResponse as entity
     */
    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createFromBasicAuth() throws Exception {
        return doCreateFromBasicAuth(null);
    }

    /**
     * This endpoint is called when a client makes a REST call with basic auth as application/json, with
     * extra parameters as JSON
     *
     * @return a response with a TokenCreateResponse as entity
     */
    @POST
    @Path("create")
    public Response createFromBasicAuth(TokenCreateUpdateRequest createRequest) throws Exception {
        return doCreateFromBasicAuth(createRequest);
    }

    @PUT
    @Path("{tokenId}")
    public Response update(@PathParam("tokenId") String tokenId, TokenCreateUpdateRequest request) {
        Token token = tokenService.getByIdForTrustedConsumers(UUID.fromString(tokenId));
        if (!token.getPrincipal().equals(sessionContext.getCallerPrincipal().getName())) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new TokenErrorResponse("Token not found for principal."))
                    .build();
        }
        return Response.ok(new TokenResponse(update(token, request))).build();
    }

    private Response doCreateFromBasicAuth(TokenCreateUpdateRequest createRequest) throws Exception {
        String userAuthorizationHeader = request.getHeader("Authorization");
        String[] authorizationHeaderParts = userAuthorizationHeader.trim().split("\\s+");
        if (authorizationHeaderParts.length != 2) {
            TokenErrorResponse errorResponse = new TokenErrorResponse("Invalid authorization details.");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
        }

        if (!authorizationHeaderParts[0].equalsIgnoreCase("Basic")) {
            TokenErrorResponse errorResponse = new TokenErrorResponse("Only 'Basic' authentication is supported.");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
        }

        String authorization = new String(Base64.getDecoder().decode(authorizationHeaderParts[1]));
        String[] parts = authorization.split(":");
        String username = parts[0];
        String password = parts[1];

        if (username == null || username.isEmpty()) {
            TokenErrorResponse errorResponse = new TokenErrorResponse("Username is not provided.");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
        }

        String refreshToken = usernamePasswordConverter.getOfflineToken(username, password);
        Token token = create(refreshToken);
        token = update(token, createRequest);
        return Response.ok(new TokenResponse(token)).build();
    }

    private Token create(String refreshToken) {
        String principal = sessionContext.getCallerPrincipal().getName();
        Token token = new Token(null, refreshToken, principal);

        String parametersToPersist = System.getProperty("secretstore.parametersToPersist");
        if (null != parametersToPersist && !parametersToPersist.isEmpty()) {
            String[] parameters = parametersToPersist.split(",");
            for (String parameter : parameters) {
                parameter = parameter.trim();
                String parameterValue = request.getHeader(parameter);
                if (null == parameterValue || parameterValue.isEmpty()) {
                    parameterValue = request.getParameter(parameter);
                }

                if (null != parameterValue && !parameterValue.isEmpty()) {
                    token.addAttribute(parameter, parameterValue);
                }
            }
        }
        return tokenService.create(token);
    }

    private Token update(Token token, TokenCreateUpdateRequest request) {
        if (null == request) {
            // nothing to update
            return token;
        }

        boolean changed = false;
        if (null != request.getAttributes()) {
            token.setAttributes(request.getAttributes());
            changed = true;
        }

        if (null != request.getExpiresAt() && !request.getExpiresAt().isEmpty()) {
            token.setExpiresAt(zonedDateTimeAdapter.convertToEntityAttribute(request.getExpiresAt()));
            changed = true;
        }

        if (changed) {
            token = tokenService.update(token);
        }
        return token;
    }
}
