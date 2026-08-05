/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.persistence.common.dao;

import java.io.IOException;
import java.io.InputStream;
import javax.cache.CacheManager;
import javax.cache.Caching;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.content.ContentLoader;
import org.apache.syncope.core.persistence.neo4j.PersistenceContext;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.ImplementationLookup;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.mockito.Mockito;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;

@Import(PersistenceContext.class)
@org.springframework.context.annotation.Configuration
public class Configuration {
    @Bean
    public ConfParamOps confParamOps() {
        return Mockito.mock(ConfParamOps.class);
    }

    @Bean
    public DomainOps domainOps() {
        return Mockito.mock(DomainOps.class);
    }

    @Bean
    public ConnectorManager connectorManager() {
        return Mockito.mock(ConnectorManager.class);
    }

    @Bean
    public ImplementationLookup implementationLookup() {
        return Mockito.mock(ImplementationLookup.class);
    }

    @Bean
    public SecurityProperties securityProperties() {
        SecurityProperties props = new SecurityProperties();
        props.setAdminUser("admin");
        props.setAnonymousUser("anonymous");
        props.setAesSecretKey("1234567890123456");
        return props;
    }

    @Bean
    @Qualifier("MasterDriver")
    public Driver masterDriver() {
        return GraphDatabase.driver(
                "bolt://%s:7687".formatted(System.getProperty("NEO4J_CONTAINER_IP", "localhost")),
                AuthTokens.none()
        );
    }

    @Bean(name = "MasterContentXML")
    public InputStream masterContentXML(final ResourceLoader resourceLoader) throws IOException {
        return resourceLoader.getResource("classpath:domains/MasterContent.xml").getInputStream();
    }


    @Bean
    public CacheManager cacheManager() {
        return Caching.getCachingProvider().getCacheManager();
    }

    @Bean
    public SmartInitializingSingleton testInitializer(final ContentLoader contentLoader) {
        return () -> {
            AuthContextUtils.callAsAdmin(SyncopeConstants.MASTER_DOMAIN, () -> {
                try {
                    contentLoader.load(SyncopeConstants.MASTER_DOMAIN);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to load MasterContent.xml into Neo4j", e);
                }
                return null;
            });
        };
    }
}
