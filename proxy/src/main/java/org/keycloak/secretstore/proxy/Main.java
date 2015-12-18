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
package org.keycloak.secretstore.proxy;

import io.undertow.Handlers;
import io.undertow.Undertow;
import org.jboss.weld.environment.se.events.ContainerInitialized;
import org.keycloak.secretstore.proxy.internal.BindTo;
import org.keycloak.secretstore.proxy.internal.Port;
import org.keycloak.secretstore.undertow.filter.AgentHttpHandler;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Juraci Paixão Kröhling
 */
@Singleton
public class Main {
    @Inject
    TokenReplacingProxyHandler handler;

    @Inject @Port
    int port;

    @Inject @BindTo
    String bind;

    void start(@Observes ContainerInitialized ignored) {
        Undertow server = Undertow.builder()
                .addHttpListener(port, bind)
                .setHandler(Handlers.proxyHandler(handler))
                .build();
        server.start();
    }
}
