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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class Neo4jAnySearchDAOGeminiTest extends AbstractNeo4jAnySearchDAOTest {
    static Stream<Arguments> searchData() {
        return Stream.of(
                // Test 1: User search by Role and Realm scope[cite: 5]
                Arguments.of(
                        "/",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.role("GymLeader"),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        List.of("brock", "lt_surge", "misty"),
                        3
                ),
                // Test 2: AnyObject search by Attribute condition (Moves with power >= 200)[cite: 5]
                Arguments.of(
                        "/",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.and(
                                SearchCondBuilder.anyType("MOVE"),
                                SearchCondBuilder.attr("power", AttrCond.Type.GE, "200")
                        ),
                        Pageable.unpaged(),
                        AnyTypeKind.ANY_OBJECT,
                        List.of("Self-Destruct", "Explosion"),
                        2
                ),
                // Test 3: Group search using Attr ISNOTNULL[cite: 5]
                Arguments.of(
                        "/kanto/league/gyms/cerulean",
                        false,
                        Set.of("/kanto/league/gyms/cerulean"),
                        SearchCondBuilder.attrNotNull("email"),
                        Pageable.unpaged(),
                        AnyTypeKind.GROUP,
                        List.of("Sailors", "Swimmers", "Women"),
                        3
                ),
                // Test 4: MemberCond on Group identifying specific member overlaps (briana)[cite: 5]
                Arguments.of(
                        "/",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.member("dcb880fc-a5d7-446a-a444-095b85db0352"),
                        Pageable.unpaged(),
                        AnyTypeKind.GROUP,
                        List.of("Women", "Swimmers"),
                        2
                ),
                // Test 5: Pagination on larger datasets (Electric Pokémon)[cite: 5]
                Arguments.of(
                        "/kanto/league/gyms/vermilion",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.and(
                                SearchCondBuilder.anyType("POKEMON"),
                                SearchCondBuilder.attr("aType", AttrCond.Type.EQ, "Electric")
                        ),
                        PageRequest.of(0, 5, Sort.by(Sort.Order.asc("name"))),
                        AnyTypeKind.ANY_OBJECT,
                        List.of("gregorys_electrike", "gregorys_flaaffy", "gregorys_pikachu",
                            "hortons_electrode_1", "hortons_electrode_2"),
                        14
                )
        );
    }

    @ParameterizedTest
    @MethodSource("searchData")
    public void testSearchAndCountOperations(
            final String basePath,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final Pageable pageable,
            final AnyTypeKind kind,
            final List<String> expectedNames,
            final long expectedCount) {

        final Realm base = realmSearchDAO.findByFullPath(basePath).orElseThrow();

        AuthContextUtils.callAsAdmin(
                SyncopeConstants.MASTER_DOMAIN, () -> {
                    // Evaluate standard search execution and paging behavior[cite: 4]
                    List<? extends Any> result = searchDAO.search(base, recursive, adminRealms, cond, pageable, kind);

                    // Evaluate native count performance[cite: 4]
                    long count = searchDAO.count(base, recursive, adminRealms, cond, kind);

                    assertEquals(
                            expectedCount,
                            count,
                            "The total count does not match the expected sum based on MasterContent.xml."
                    );

                    List<String> names = result.stream().map(e -> {
                        if (e instanceof AnyObject) {
                            return ((AnyObject) e).getName();
                        }
                        if (e instanceof Group) {
                            return ((Group) e).getName();
                        }
                        if (e instanceof User) {
                            return ((User) e).getUsername();
                        }
                        return "";
                    }).toList();

                    for (String expectedName : expectedNames) {
                        assertTrue(
                                names.contains(expectedName),
                                "Missing expected result name: " + expectedName
                        );
                    }
                    return null;
                });
    }

    @Test
    public void testInvalidSearchParametersOnMultiplePlainSchemas() {
        final Realm realm = realmSearchDAO.findByFullPath("/").orElseThrow();

        AuthContextUtils.callAsAdmin(
                SyncopeConstants.MASTER_DOMAIN, () -> {
                    SyncopeClientException exception = assertThrows(
                            SyncopeClientException.class,
                            () -> searchDAO.search(
                                    realm,
                                    true,
                                    Set.of("/"),
                                    SearchCondBuilder.anyType("POKEMON"),
                                    Pageable.unpaged(Sort.by(
                                            Sort.Order.desc("hp"),
                                            Sort.Order.asc("spe"))),
                                    AnyTypeKind.ANY_OBJECT)
                    );

                    assertEquals(ClientExceptionType.InvalidSearchParameters, exception.getType());
                    assertTrue(
                        exception.getMessage().contains("Order by more than one attribute is not allowed"),
                "Exception should describe that ordering by multiple plain"
                        + " schema attributes is not allowed[cite: 4]."
                    );
                    return null;
                }
        );
    }
}
