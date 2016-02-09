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
package org.keycloak.secretstore.itest.smoke

import org.jboss.arquillian.junit.Arquillian
import org.junit.Test
import org.junit.runner.RunWith

import java.time.ZoneOffset
import java.time.ZonedDateTime

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

@RunWith(Arquillian.class)
class SecretStoreITest extends BaseSmokeTest {

    @Test
    void createTokenBasedOnCurrentUserCredentials() {
        def response = client.post(path: "/secret-store/v1/tokens/create")
        assertEquals(200, response.status)
        assertNotNull(response.data.key)
        assertNotNull(response.data.secret)
        assertNotNull(response.data.principal)
        assertNotNull(response.data.createdAt)
        assertNotNull(response.data.updatedAt)
    }

    @Test
    void setExpiresAt() {
        def response = client.post(path: "/secret-store/v1/tokens/create")
        def token = response.data

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC)
        response = client.put(
                path: "/secret-store/v1/tokens/${token.key}",
                body: [expiresAt: now.toString()]
        )
        ZonedDateTime persisted = ZonedDateTime.parse(response.data.expiresAt).withZoneSameInstant(ZoneOffset.UTC)
        assertEquals("Expires At should have been set", now, persisted)
    }

    @Test
    void setExtraAttribute() {
        def response = client.post(path: "/secret-store/v1/tokens/create")
        def token = response.data

        response = client.put(
                path: "/secret-store/v1/tokens/${token.key}",
                body: [attributes: [name: "My lovely token"]]
        )
        assertEquals("Extra attribute should have been set", "My lovely token", response.data.attributes.name)
    }
}