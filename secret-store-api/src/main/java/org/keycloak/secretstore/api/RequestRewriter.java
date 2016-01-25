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
package org.keycloak.secretstore.api;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;
import org.keycloak.secretstore.api.internal.MsgLogger;
import org.keycloak.secretstore.common.AuthServerRequestExecutor;
import org.keycloak.secretstore.common.AuthServerUrl;
import org.keycloak.secretstore.common.RealmName;
import org.keycloak.secretstore.common.ZonedDateTimeAdapter;

import javax.annotation.security.PermitAll;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.net.URLEncoder;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * @author Juraci Paixão Kröhling
 */
@Singleton
@PermitAll
public class RequestRewriter {
    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");

    private final MsgLogger logger = MsgLogger.LOGGER;

    @Inject
    @RealmName
    String realmName;

    @Inject
    @AuthServerUrl
    String authServerUrl;

    @Inject
    AuthServerRequestExecutor authServerRequestExecutor;

    @Inject
    TokenService tokenService;

    public HttpServerExchange rewrite(HttpServerExchange httpServerExchange) throws Exception {
        // steps:
        // - check and get client/secret for the secret-store
        // - check if the request contains an agent credential
        // - check if the credential exists and is valid
        // - lookup the token for the credential
        // - convert the refresh token into an access token
        // - set the Authorization header for Keycloak

        HeaderValues authorizationHeaders = httpServerExchange.getRequestHeaders().get("Authorization");
        if (authorizationHeaders == null || authorizationHeaders.size() < 1) {
            // nothing to do, credentials not provided
            logger.noAuthorizationHeader();
            return httpServerExchange;
        }

        String authorizationHeader = authorizationHeaders.getFirst();
        String[] authorizationHeaderParts = authorizationHeader.trim().split("\\s+");

        if (authorizationHeaderParts.length != 2) {
            logger.authorizationHeaderInvalid(authorizationHeader);
            return httpServerExchange;
        }

        if (!authorizationHeaderParts[0].equalsIgnoreCase("Basic")) {
            logger.noBasicAuth();
            return httpServerExchange;
        }

        String authorization = new String(Base64.getDecoder().decode(authorizationHeaderParts[1]));
        String[] parts = authorization.split(":");
        String keyAsString = parts[0];
        String secret = parts[1];

        if (keyAsString == null || keyAsString.isEmpty()) {
            // nothing to do, credentials not provided
            logger.keyIsEmpty();
            return httpServerExchange;
        }

        if (!UUID_PATTERN.matcher(keyAsString).matches()) {
            // not an UUID, can't be a token
            logger.notLikeUUID(keyAsString);
            return httpServerExchange;
        }

        UUID key;
        try {
            key = UUID.fromString(keyAsString);
        } catch (Throwable t) {
            // not an UUID, can't be a token
            logger.notAnUUID(keyAsString);
            return httpServerExchange;
        }

        Token token = tokenService.validate(key, secret);

        if (token == null) {
            logger.tokenNotFound(keyAsString);
            httpServerExchange.setStatusCode(403);
            httpServerExchange.endExchange();
            return null;
        }

        if (null != token.getExpiresAt() && ZonedDateTime.now().isAfter(token.getExpiresAt())) {
            logger.tokenExpired(token.getId().toString(), token.getExpiresAt().toString(), ZonedDateTime.now().toString());
            httpServerExchange.setStatusCode(403);
            httpServerExchange.endExchange();
            return null;
        }

        String bearerToken = getBearerToken(token);
        if (bearerToken == null) {
            logger.cannotGetBearerToken(token.getId().toString());
            httpServerExchange.setStatusCode(403);
            httpServerExchange.endExchange();
            return null;
        }

        logger.tokenReplaced(token.getId().toString(), bearerToken);
        httpServerExchange.getRequestHeaders().remove("Authorization");
        httpServerExchange.getRequestHeaders().put(new HttpString("Authorization"), "Bearer " + bearerToken);

        token.getAttributes().forEach((k, v) -> {
            httpServerExchange.getRequestHeaders().remove(k);
            httpServerExchange.getRequestHeaders().put(new HttpString(k), v);
        });

        return httpServerExchange;
    }

    private String getBearerToken(Token token) throws Exception {
        String tokenUrl = authServerUrl
                + "/realms/"
                + URLEncoder.encode(realmName, "UTF-8")
                + "/protocol/openid-connect/token";

        String urlParameters = "scope=offline_access&grant_type=refresh_token&refresh_token=" +
                URLEncoder.encode(token.getRefreshToken(), "UTF-8");

        String sResponse = authServerRequestExecutor.execute(tokenUrl, urlParameters, "POST");
        JsonReader jsonReader = Json.createReader(new StringReader(sResponse));
        JsonObject object = jsonReader.readObject();
        if (object.get("error") != null) {
            String error = object.getString("error");
            logger.errorResponseFromServer(error);
            return null;
        }

        String tokenType = object.getString("token_type");
        String bearerToken = object.getString("access_token");

        if (null == tokenType || tokenType.isEmpty() || !tokenType.equalsIgnoreCase("bearer")) {
            logger.invalidResponseFromServer();
            return null;
        }

        if (null == bearerToken || bearerToken.isEmpty()) {
            logger.invalidBearerTokenFromServer();
            return null;
        }

        return bearerToken;
    }
}
