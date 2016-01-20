<?xml version="1.0" encoding="UTF-8"?>
<!--

    Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
    and other contributors as indicated by the @author tags.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:ds="urn:jboss:domain:datasources:3.0" xmlns:ra="urn:jboss:domain:resource-adapters:3.0" xmlns:ejb3="urn:jboss:domain:ejb3:3.0"
                xmlns:logging="urn:jboss:domain:logging:3.0" xmlns:undertow="urn:jboss:domain:undertow:3.0" xmlns:tx="urn:jboss:domain:transactions:3.0"
                version="2.0" exclude-result-prefixes="xalan ds ra ejb3 logging undertow tx">

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no" />
    <xsl:strip-space elements="*" />

    <xsl:template match="/*[name()='server']/*[name()='system-properties']/*[name()='property'][@name='keycloak.import']">
        <property>
            <xsl:attribute name="name">keycloak.import</xsl:attribute>
            <xsl:attribute name="value">${keycloak.import:${jboss.home.dir}/standalone/configuration/secret-store-realm-for-dev.json}</xsl:attribute>
        </property>
    </xsl:template>

    <xsl:template name="secure-deployment">
        <xsl:param name="deployment.name" />
        <xsl:param name="credential.secret" />
        <secure-deployment>
            <xsl:attribute name="name"><xsl:value-of select="$deployment.name"/></xsl:attribute>
            <realm>secret-store</realm>
            <resource>secret-store</resource>
            <use-resource-role-mappings>true</use-resource-role-mappings>
            <enable-cors>true</enable-cors>
            <enable-basic-auth>true</enable-basic-auth>
            <credential name="secret"><xsl:value-of select="$credential.secret"/></credential>
        </secure-deployment>
    </xsl:template>

    <!-- //*[local-name()='secure-deployment'] is an xPath's 1.0 way of saying of xPath's 2.0 prefix-less selector //*:secure-deployment  -->
    <xsl:template match="//*[*[local-name()='secure-deployment']]">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
            <xsl:call-template name="secure-deployment">
                <xsl:with-param name="deployment.name" select="'secret-store-examples-filter-on-backend.war'" />
                <xsl:with-param name="credential.secret" select="*[local-name()='secure-deployment']/*[local-name()='credential' and @name='secret']/text()"/>
            </xsl:call-template>
            <xsl:call-template name="secure-deployment">
                <xsl:with-param name="deployment.name" select="'secret-store-examples-client-behind-proxy.war'" />
                <xsl:with-param name="credential.secret" select="*[local-name()='secure-deployment']/*[local-name()='credential' and @name='secret']/text()"/>
            </xsl:call-template>
        </xsl:copy>
    </xsl:template>

    <!-- copy everything else as-is -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*" />
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
