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
package org.keycloak.secretstore.api.internal;

/**
 * @author Juraci Paixão Kröhling
 */
public enum BoundStatements {
    REVOKE_BY_ID("revoke-token-by-id", "DELETE FROM secretstore.tokens WHERE id = ?"),
    GET_BY_ID("get-token-by-id", "SELECT * FROM secretstore.tokens WHERE id = ?"),
    GET_BY_PRINCIPAL("get-token-by-principal", "SELECT * FROM secretstore.tokens WHERE principal = ?"),
    CREATE("create-token",
            "INSERT INTO secretstore.tokens " +
                    "(id, refreshToken, secret, principal, attributes, expiresAt, createdAt, updatedAt) " +
                    "VALUES " +
                    "(?, ?, ?, ?, ?, ?, ?, ?)"),
    UPDATE("update-token",
            "UPDATE secretstore.tokens SET " +
                    "attributes = ?, " +
                    "expiresAt = ? " +
                    "WHERE " +
                    "id = ?");

    private String name;
    private String value;

    BoundStatements(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
