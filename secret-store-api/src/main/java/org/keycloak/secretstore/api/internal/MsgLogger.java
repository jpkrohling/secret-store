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

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.*;

/**
 * @author Juraci Paixão Kröhling
 */
@MessageLogger(projectCode = "SECSTORE")
@ValidIdRange(min = 160000, max = 169999)
public interface MsgLogger {
    MsgLogger LOGGER = Logger.getMessageLogger(MsgLogger.class, MsgLogger.class.getPackage().getName());

    @LogMessage(level = Logger.Level.FATAL)
    @Message(id = 160000, value = "Failed to initialize Cassandra's schema for Secret Store. Reason")
    void failedToInitializeSchema(@Cause Throwable t);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 160001, value = "Secret Store received a response from Keycloak that doesn't include a bearer token.")
    void invalidResponseFromServer();

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 160002, value = "Secret Store a bearer response from Keycloak without the token itself!")
    void invalidBearerTokenFromServer();

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 160003, value = "Secret Store received an error response from Keycloak: %s")
    void errorResponseFromServer(String errorMessage);

}
