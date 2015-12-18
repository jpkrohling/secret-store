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

import com.datastax.driver.core.Session;
import org.keycloak.secretstore.common.internal.CassandraSessionCallable;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

/**
 * @author Juraci Paixão Kröhling
 */
@Startup
@Singleton
@ApplicationScoped
@PermitAll
public class SecretStoreSchemaInitializer {
    @Inject
    CassandraSessionCallable cassandraSessionCallable;
    MsgLogger logger = MsgLogger.LOGGER;
    private Future<Session> sessionFuture;

    @Resource
    private ManagedExecutorService executor;

    @PostConstruct
    public void init() {
        if (null == executor) {
            // we have no executor, we are probably not in an EE environment
            try {
                FutureTask<Session> task = new FutureTask<>(cassandraSessionCallable);
                task.run();

                sessionFuture = task;
            } catch (Exception e) {
                throw new RuntimeException("Could not connect to Cassandra", e);
            }
            return;
        }

        // we are in an EE environment, delegate it to the container
        sessionFuture = executor.submit(cassandraSessionCallable);
    }

    @Produces
    @ApplicationScoped
    @SecretStore
    public Session getSession() {
        try {
            Session session = sessionFuture.get();
            InputStream input = getClass().getResourceAsStream("/secret-store.cql");
            try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
                String content = buffer.lines().collect(Collectors.joining("\n"));
                for (String cql : content.split("(?m)^-- #.*$")) {
                    if (!cql.startsWith("--")) {
                        session.execute(cql);
                    }
                }
            } catch (Exception e) {
                logger.failedToInitializeSchema(e);
            }
            return session;
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Could not get the initialized session.");
        }
    }
}