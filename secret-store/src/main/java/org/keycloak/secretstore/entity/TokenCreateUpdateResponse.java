/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.secretstore.entity;

import java.time.ZonedDateTime;
import java.util.Map;

import org.keycloak.secretstore.api.Token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Juraci Paixão Kröhling
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenCreateUpdateResponse {
    private String key;
    private String secret;
    private ZonedDateTime expiresAt;
    private Map<String, String> attributes;

    public TokenCreateUpdateResponse(Token token) {
        this.key = token.getId().toString();
        this.secret = token.getSecret();
        this.attributes = token.getAttributes();
        this.expiresAt = token.getExpiresAt();
    }

    public TokenCreateUpdateResponse(String key, String secret, ZonedDateTime expiresAt,
                                     Map<String, String> attributes) {
        this.key = key;
        this.secret = secret;
        this.expiresAt = expiresAt;
        this.attributes = attributes;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public ZonedDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(ZonedDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
}
