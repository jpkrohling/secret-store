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

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

@RunWith(Arquillian.class)
class SecretStoreITest extends BaseSmokeTest {

    @Test
    void createTokenBasedOnCurrentPersonaCredentials() {
        def response = client.post(path: "/secret-store/v1/tokens/create")
        assertEquals(200, response.status)
        assertNotNull(response.data.key)
        assertNotNull(response.data.secret)
    }
}