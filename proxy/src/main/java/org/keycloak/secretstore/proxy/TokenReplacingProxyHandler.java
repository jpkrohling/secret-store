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

import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.UndertowClient;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;
import io.undertow.server.handlers.proxy.ProxyCallback;
import io.undertow.server.handlers.proxy.ProxyClient;
import io.undertow.server.handlers.proxy.ProxyConnection;
import io.undertow.util.AttachmentKey;
import org.keycloak.secretstore.api.RequestRewriter;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

/**
 * @author Juraci Paixão Kröhling
 */
public class TokenReplacingProxyHandler implements ProxyClient {
    private static final ProxyClient.ProxyTarget TARGET = new ProxyClient.ProxyTarget() {
    };
    private final AttachmentKey<ClientConnection> clientAttachmentKey = AttachmentKey.create(ClientConnection.class);
    private final UndertowClient client;
    @Inject
    RequestRewriter requestRewriter;

    public TokenReplacingProxyHandler() {
        client = UndertowClient.getInstance();
    }

    @Override
    public ProxyClient.ProxyTarget findTarget(HttpServerExchange exchange) {
        return TARGET;
    }

    @Override
    public void getConnection(ProxyClient.ProxyTarget target, HttpServerExchange exchange, ProxyCallback<ProxyConnection> callback, long timeout, TimeUnit timeUnit) {
        try {
            exchange = requestRewriter.rewrite(exchange);
        } catch (Exception e) {
            callback.failed(exchange);
            e.printStackTrace();
        }

        ClientConnection existing = exchange.getConnection().getAttachment(clientAttachmentKey);
        if (existing != null) {
            if (existing.isOpen()) {
                //this connection already has a client, re-use it
                callback.completed(exchange, new ProxyConnection(existing, exchange.getRequestURI()));
                return;
            } else {
                exchange.getConnection().removeAttachment(clientAttachmentKey);
            }
        }
        try {
            client.connect(new ConnectNotifier(callback, exchange), new URI(exchange.getRequestURI()), exchange.getIoThread(), exchange.getConnection().getByteBufferPool(), OptionMap.EMPTY);
        } catch (URISyntaxException e) {
            callback.failed(exchange);
            e.printStackTrace();
        }
    }

    private final class ConnectNotifier implements ClientCallback<ClientConnection> {
        private final ProxyCallback<ProxyConnection> callback;
        private final HttpServerExchange exchange;

        private ConnectNotifier(ProxyCallback<ProxyConnection> callback, HttpServerExchange exchange) {
            this.callback = callback;
            this.exchange = exchange;
        }

        @Override
        public void completed(final ClientConnection connection) {
            final ServerConnection serverConnection = exchange.getConnection();
            //we attach to the connection so it can be re-used
            serverConnection.putAttachment(clientAttachmentKey, connection);
            serverConnection.addCloseListener(serverConnection1 -> IoUtils.safeClose(connection));
            connection.getCloseSetter().set(channel -> serverConnection.removeAttachment(clientAttachmentKey));
            callback.completed(exchange, new ProxyConnection(connection, ""));
        }

        @Override
        public void failed(IOException e) {
            callback.failed(exchange);
        }
    }

}
