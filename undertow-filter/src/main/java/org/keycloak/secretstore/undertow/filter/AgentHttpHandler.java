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
package org.keycloak.secretstore.undertow.filter;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.keycloak.secretstore.api.RequestRewriter;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

/**
 * @author Juraci Paixão Kröhling
 */
public class AgentHttpHandler implements HttpHandler {
    private HttpHandler next;

    public AgentHttpHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        if (httpServerExchange.isInIoThread()) {
            httpServerExchange.dispatch(this);
            return;
        }

        httpServerExchange = getRequestRewriter().rewrite(httpServerExchange);
        if (null == httpServerExchange) {
            return;
        }

        finish(httpServerExchange);
    }

    private void finish(HttpServerExchange httpServerExchange) throws Exception {
        next.handleRequest(httpServerExchange);
    }

    @SuppressWarnings("unchecked")
    private RequestRewriter getRequestRewriter() {
        Thread.currentThread().setContextClassLoader(AgentHttpHandler.class.getClassLoader());
        BeanManager bm = CDI.current().getBeanManager();
        Bean<RequestRewriter> bean = (Bean<RequestRewriter>) bm.getBeans(RequestRewriter.class).iterator().next();
        CreationalContext<RequestRewriter> ctx = bm.createCreationalContext(bean);
        return (RequestRewriter) bm.getReference(bean, RequestRewriter.class, ctx);
    }
}
