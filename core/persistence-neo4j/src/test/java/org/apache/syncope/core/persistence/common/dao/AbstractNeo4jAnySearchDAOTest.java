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

import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(Configuration.class)
public abstract class AbstractNeo4jAnySearchDAOTest {
    @Autowired
    protected RealmSearchDAO realmSearchDAO;
    @Autowired
    protected DynRealmDAO dynRealmDAO;
    @Autowired
    protected UserDAO userDAO;
    @Autowired
    protected GroupDAO groupDAO;
    @Autowired
    protected AnyObjectDAO anyObjectDAO;
    @Autowired
    protected PlainSchemaDAO schemaDAO;
    @Autowired
    protected EntityFactory entityFactory;
    @Autowired
    protected AnyUtilsFactory anyUtilsFactory;
    @Autowired
    protected PlainAttrValidationManager validator;
    @Autowired
    protected Neo4jOperations neo4jTemplate;
    @Autowired
    protected Neo4jClient neo4jClient;
    @Autowired
    protected AnyTypeDAO anyTypeDAO;
    @Autowired
    protected AnyTypeClassDAO anyTypeClassDAO;
    @Autowired
    protected AnySearchDAO searchDAO;
}
