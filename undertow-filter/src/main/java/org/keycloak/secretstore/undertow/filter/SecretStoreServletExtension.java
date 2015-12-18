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

import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;

import javax.servlet.ServletContext;

/**
 * @author Juraci Paixão Kröhling
 */
public class SecretStoreServletExtension implements ServletExtension {
    private final MsgLogger logger = MsgLogger.LOGGER;

    @Override
    public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
        if (deploymentInfo.isAuthenticationMechanismPresent("KEYCLOAK")) {
            boolean enabled = Boolean.valueOf(servletContext.getInitParameter("org.keycloak.secretstore.enabled"));
            if (enabled) {
                logger.secretStoreEnabled();
                deploymentInfo.addOuterHandlerChainWrapper(AgentHttpHandler::new);
            } else {
                logger.secretStoreNotEnabled();
            }
        }
    }
}
