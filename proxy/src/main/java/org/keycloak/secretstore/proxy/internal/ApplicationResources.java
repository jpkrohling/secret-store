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
package org.keycloak.secretstore.proxy.internal;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jboss.weld.environment.se.bindings.Parameters;
import org.keycloak.secretstore.common.AuthServerUrl;
import org.keycloak.secretstore.common.RealmName;
import org.keycloak.secretstore.common.RealmResourceName;
import org.keycloak.secretstore.common.RealmResourceSecret;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * @author Juraci Paixão Kröhling
 */
@ApplicationScoped
public class ApplicationResources {
    @Inject
    @Parameters
    String[] parameters;
    private CommandLine cmd;
    private Configuration configuration;

    @PostConstruct
    public void prepareCommandLineOptions() {
        Options options = new Options();
        options.addOption("c", "configuration", true, "Configuration file");
        options.addOption("r", "realm-name", true, "Realm name");
        options.addOption("rn", "realm-resource", true, "Resource name");
        options.addOption("rs", "realm-secret", true, "Resource secret");
        options.addOption("u", "auth-server-url", true, "Keycloak Server URL");
        options.addOption("b", "bind", true, "IP address to bind to");
        options.addOption("p", "port", true, "Port to bind to");

        try {
            cmd = new DefaultParser().parse(options, parameters, false);
        } catch (ParseException e) {
            throw new RuntimeException("Could not parse configuration file", e);
        }

        Reader configurationReader;
        if (cmd.hasOption("c")) {
            try {
                configurationReader = new FileReader(cmd.getOptionValue("c"));
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Could not load configuration file", e);
            }
        } else {
            configurationReader = new InputStreamReader(getClass().getResourceAsStream("/configuration.yml"));
        }

        YamlReader reader = new YamlReader(configurationReader);
        try {
            configuration = reader.read(Configuration.class);
        } catch (YamlException e) {
            throw new RuntimeException("Could not read configuration file", e);
        }

        if (cmd.hasOption("r")) {
            configuration.setRealmName(cmd.getOptionValue("r"));
        }

        if (cmd.hasOption("rn")) {
            configuration.setResourceName(cmd.getOptionValue("rn"));
        }

        if (cmd.hasOption("rs")) {
            configuration.setResourceSecret(cmd.getOptionValue("rs"));
        }

        if (cmd.hasOption("u")) {
            configuration.setAuthServerUrl(cmd.getOptionValue("u"));
        }

        if (cmd.hasOption("b")) {
            configuration.setBind(cmd.getOptionValue("b"));
        }

        if (cmd.hasOption("p")) {
            configuration.setPort(Integer.parseInt(cmd.getOptionValue("p")));
        }

    }

    @Alternative
    @Produces
    @RealmName
    public String getRealmName() {
        return configuration.getRealmName();
    }

    @Alternative
    @Produces
    @RealmResourceName
    public String getRealmResourceName() {
        return configuration.getResourceName();
    }

    @Alternative
    @Produces
    @RealmResourceSecret
    public String getRealmResourceSecret() {
        return configuration.getResourceSecret();
    }

    @Alternative
    @Produces
    @AuthServerUrl
    public String getAuthServerUrl() {
        return configuration.getAuthServerUrl();
    }

    @Produces
    @Port
    public int getPort() {
        return configuration.getPort();
    }

    @Produces
    @BindTo
    public String getBindTo() {
        return configuration.getBind();
    }

}
