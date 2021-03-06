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

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Juraci Paixão Kröhling
 */
@ApplicationScoped
public class ApplicationResources {
    @Inject
    @SecretStore
    Session session;
    private Map<BoundStatements, PreparedStatement> statements = new HashMap<>(BoundStatements.values().length);

    @PostConstruct
    public void buildStatements() {
        for (BoundStatements statement : BoundStatements.values()) {
            statements.put(statement, session.prepare(statement.getValue()));
        }
    }

    @Produces
    @NamedStatement
    public BoundStatement produceStatementByName(InjectionPoint injectionPoint) {
        NamedStatement annotation = injectionPoint.getAnnotated().getAnnotation(NamedStatement.class);
        BoundStatements stmtName = annotation.value();
        return statements.get(stmtName).bind();
    }
}
