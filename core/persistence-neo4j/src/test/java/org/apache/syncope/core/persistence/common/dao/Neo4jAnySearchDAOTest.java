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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
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
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.DynRealmMembership;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(Configuration.class)
@DirtiesContext
public class Neo4jAnySearchDAOTest {
    private static final Logger LOGGER = Logger.getLogger(Neo4jAnySearchDAOTest.class.getName());

    @Autowired
    private RealmSearchDAO realmSearchDAO;

    @Autowired
    private DynRealmDAO dynRealmDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private PlainSchemaDAO schemaDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

    @Autowired
    private PlainAttrValidationManager validator;

    @Autowired
    private Neo4jOperations neo4jTemplate;

    @Autowired
    private Neo4jClient neo4jClient;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Autowired
    private AnySearchDAO searchDAO;

    @BeforeAll
    public static void setupDynamicRealm() {

    }

    static Stream<Arguments> inputs() {
        return Stream.of(
        // [1] base is an internal realm in the hierarchy tree but the user doesn't
        // have permission to read it (adminRealms contain base's children only)
        Arguments.of(
                "/scranton",
                false,
                Set.of("/scranton/sales", "/scranton/accounting"),
                SearchCondBuilder.attr("title", AttrCond.Type.EQ, "ceo"),
                Pageable.unpaged(),
                AnyTypeKind.USER,
                List.of(),
                false
        ),
        // [2] base is a leaf node in the hierarchy tree, the user has permissions to read the whole subtree
        // whose root is a parent of base, and a match filling almost the full page is expected, at least one
        // mathing item is skipped
        Arguments.of(
                "/scranton/sales",
                true,
                Set.of("/scranton"),
                SearchCondBuilder.membership("4afd4117-d792-4f9e-8485-0720f7e39285"),
                PageRequest.of(1, 2, Sort.by("username")),
                AnyTypeKind.USER,
                List.of("mcurie"),
                false
        ),
        // [3] base is a leaf node, the number of matching items overflows the size of the page, page is sorted
        Arguments.of(
            "/scranton/accounting",
            false,
            Set.of("/scranton/accounting", "/scranton/sales"),
            SearchCondBuilder.anyType("PRINTER"),
            PageRequest.of(0, 1, Sort.by("name").ascending()),
            AnyTypeKind.ANY_OBJECT,
            List.of("printer 4"),
            true
        ),
        // [4] base is an internal node
        Arguments.of(
                "/scranton/",
                false,
                Set.of("/"),
                SearchCondBuilder.attr("gender", AttrCond.Type.ILIKE, "f"),
                Pageable.unpaged(),
                AnyTypeKind.USER,
                List.of("pbeesly", "amartin", "mcurie", "plapin", "kkapoor"),
                false
        ));
    };

    @ParameterizedTest
    @MethodSource("inputs")
    public void test(
            final String basePath,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final Pageable pageable,
            final AnyTypeKind kind,
            final List<String> expectedIds,
            final boolean sorted
    ) {
        final Realm base = realmSearchDAO.findByFullPath(basePath).orElseThrow();
        AuthContextUtils.callAs(
                SyncopeConstants.MASTER_DOMAIN, "mscott", Set.of(), () -> {
                    List<? extends Any> result = searchDAO.search(base, recursive, adminRealms, cond, pageable, kind);
                    List<String> ids = result.stream().map(e -> switch (e) {
                        case AnyObject o -> o.getName();
                        case Group g -> g.getName();
                        case User u -> u.getUsername();
                        default -> throw new IllegalArgumentException("unexpected type: " + e.getClass().getName());
                    }).toList();
                    if (sorted) {
                        Assertions.assertIterableEquals(expectedIds, ids);
                    } else {
                        Assertions.assertEquals(expectedIds.size(), result.size());
                        Assertions.assertTrue(expectedIds.containsAll(ids));
                    }
                    return null;
                });
    }

    @Test
    public void testRelationshipMappings() {
        final Realm root = realmSearchDAO.findByFullPath("/").orElseThrow();
        final Set<String> adminRealms = Set.of(SyncopeConstants.ROOT_REALM);
        AuthContextUtils.callAsAdmin(SyncopeConstants.MASTER_DOMAIN, () -> {
            SearchCond printAccessCond = SearchCondBuilder.and(
                    SearchCondBuilder.auxClass("SalesRecord"),
                    SearchCondBuilder.attr("numberOfSales", AttrCond.Type.GT, "1000")
            );

            List<User> usersWithPrintAccess = searchDAO.search(
                    root,
                    true,
                    adminRealms,
                    printAccessCond,
                    Pageable.ofSize(10),
                    AnyTypeKind.USER
            );

            // Expected: Dwight Schrute, Jim Halpert, Michael Scott
            Assertions.assertEquals(2, usersWithPrintAccess.size(), "Should find exactly 3 users linked to printer 1");

            return null;
        });
    }

    private Optional<DynRealm> createDynRealm(final String key, final String fiql) {
        if (dynRealmDAO.findById(key).isEmpty()) {
            DynRealm dyn = entityFactory.newEntity(DynRealm.class);
            dyn.setKey(key);
            DynRealmMembership membership = entityFactory.newEntity(DynRealmMembership.class);
            membership.setDynRealm(dyn);
            membership.setAnyType(anyTypeDAO.getUser());
            membership.setFIQLCond(fiql);
            dyn.add(membership);
            return Optional.of(dynRealmDAO.save(dyn));
        }
        return Optional.empty();
    }

    private void addToDynRealm(final DynRealm realm, final User user) {
        neo4jClient.query(
                        "MATCH (u:SyncopeUser {username: $username}) "
                                + "MATCH (d:DynRealm {id: $dynRealmId}) "
                                + "MERGE (u)-[:DYN_REALM_MEMBERSHIP]->(d)"
                ).bind(user.getUsername()).to("username")
                .bind(realm.getKey()).to("dynRealmId")
                .run();
    }

    private Optional<User> createUser(final Realm realm, final String username, final String email) {
        if (userDAO.findByUsername(username).isEmpty()) {
            User user = entityFactory.newEntity(User.class);
            user.setUsername(username);
            user.setRealm(realm);
            PlainAttr emailAttr = new PlainAttr();
            emailAttr.setSchema("email");
            emailAttr.add(validator, email);
            user.add(emailAttr);
            return Optional.of(userDAO.save(user));
        }
        return Optional.empty();
    }

    private Optional<User> createUser(final Realm realm, final String username) {
        if (userDAO.findByUsername(username).isEmpty()) {
            User user = entityFactory.newEntity(User.class);
            user.setUsername(username);
            user.setRealm(realm);
            return Optional.of(userDAO.save(user));
        }
        return Optional.empty();
    }

    private Optional<Group> createGroup(final Realm realm, final String name) {
        if (groupDAO.findByName(name).isEmpty()) {
            Group group = entityFactory.newEntity(Group.class);
            group.setName(name);
            group.setRealm(realm);
            return Optional.of(groupDAO.save(group));
        }
        return Optional.empty();
    }

    private User addToGroup(final User user, final Group group) {
        UMembership membership = entityFactory.newEntity(UMembership.class);
        membership.setRightEnd(group);
        user.add(membership);
        return userDAO.save(user);
    }
}
