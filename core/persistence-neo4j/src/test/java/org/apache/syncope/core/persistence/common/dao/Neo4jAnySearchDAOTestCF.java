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
import java.util.stream.Stream;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.DynRealmMembership;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(Configuration.class)
@DirtiesContext
public class Neo4jAnySearchDAOTestCF extends AbstractNeo4jAnySearchDAOTest {
    private static final boolean SKIP_TEST = true;
    private static boolean INIT_DYN_REALMS = false;

    @BeforeEach
    public void initDynRealms(final TestInfo testInfo) {
        if (INIT_DYN_REALMS) {
            return;
        }

        testInfo.getTestMethod().ifPresent(method -> {
            if ("test".equals(method.getName())) {
                AuthContextUtils.callAsAdmin(
                        SyncopeConstants.MASTER_DOMAIN,
                        () -> {
                            createGroundRealm();
                            createNonGymTrainerRealm();
                            createRockRealm();
                            createWaterRealm();
                            return null;
                        });
                INIT_DYN_REALMS = true;
            }
        });
    }

    static Stream<Arguments> coverageInputs() {
        return Stream.of(
                // [Possibile BUG]: viene generata una query sbagliata che induce il DAO a sollevare un'eccezione
                Arguments.of(
                        "/",
                        false,
                        Set.of("waterTypes"),
                        SearchCondBuilder.and(
                                SearchCondBuilder.attr("hp", AttrCond.Type.GT, "79"),
                                SearchCondBuilder.attr("hp", AttrCond.Type.LT, "92")
                        ),
                        Pageable.unpaged(),
                        AnyTypeKind.ANY_OBJECT,
                        List.of("mistys_golduck", "loreleis_dewgong", "dianas_golduck", "brianas_seaking_1",
                                "brianas_seaking_2"),
                        false,
                        5,
                        SKIP_TEST
                ),
                Arguments.of(
                        "/",
                        true,
                        Set.of("waterTypes", "/"),
                        SearchCondBuilder.and(
                                SearchCondBuilder.attr("hp", AttrCond.Type.GT, "79"),
                                SearchCondBuilder.attr("hp", AttrCond.Type.LT, "92")
                        ),
                        Pageable.unpaged(),
                        AnyTypeKind.ANY_OBJECT,
                        List.of("mistys_golduck", "loreleis_dewgong", "dianas_golduck", "brianas_seaking_1",
                                "brianas_seaking_2"),
                        false,
                        5,
                        SKIP_TEST
                ),
                // [Coverage INFO]
                // Non è possibile coprire la riga di codice membershipAttrConds()@L#897 perché il campo "id"
                // viene rimosso preventivamente in wrapQuery()@L#843.
                Arguments.of(
                        "/kanto",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.or(
                                SearchCondBuilder.any(
                                        "id",
                                        AttrCond.Type.EQ,
                                        "c9ce44bd-7c3f-1add-da38-5db590eeb067"
                                ),
                                SearchCondBuilder.and(
                                        SearchCondBuilder.any("name", AttrCond.Type.LIKE, "brocks_%"),
                                        SearchCondBuilder.attr("atk", AttrCond.Type.GT, "80")
                                )
                        ),
                        PageRequest.of(0, 10, Sort.by(Sort.Order.desc("weight"))),
                        AnyTypeKind.ANY_OBJECT,
                        List.of("brocks_rhyhorn", "brocks_graveler", "brocks_kabutops", "brocks_omastar"),
                        true,
                        4,
                        false
                ),
                Arguments.of(
                        "/kanto",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.and(
                                SearchCondBuilder.any("name", AttrCond.Type.LIKE, "brocks_%"),
                                SearchCondBuilder.attr("atk", AttrCond.Type.GT, "80")
                        ),
                        PageRequest.of(0, 10, Sort.by(Sort.Order.asc("id"))),
                        AnyTypeKind.ANY_OBJECT,
                        List.of("brocks_graveler", "brocks_kabutops", "brocks_rhyhorn"),
                        true,
                        3,
                        false
                ),
                Arguments.of(
                        "/kanto",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.and(
                                SearchCondBuilder.any("name", AttrCond.Type.LIKE, "brocks_%"),
                                SearchCondBuilder.attr("atk", AttrCond.Type.GT, "80")
                        ),
                        PageRequest.of(0, 10, Sort.by(Sort.Order.desc("weight"))),
                        AnyTypeKind.ANY_OBJECT,
                        List.of("brocks_rhyhorn", "brocks_graveler", "brocks_kabutops"),
                        true,
                        3,
                        false
                ),
                Arguments.of(
                        "/",
                        true,
                        Set.of("waterTypes", "anotherRealm"),
                        SearchCondBuilder.and(
                                SearchCondBuilder.attr("hp", AttrCond.Type.GT, "79"),
                                SearchCondBuilder.attr("hp", AttrCond.Type.LT, "92")
                        ),
                        Pageable.unpaged(),
                        AnyTypeKind.ANY_OBJECT,
                        List.of("mistys_golduck", "loreleis_dewgong", "dianas_golduck", "brianas_seaking_1",
                                "brianas_seaking_2"),
                        false,
                        5,
                        SKIP_TEST
                ),
                // [8] base è un nodo interno, recursive == true e ci si aspetta almeno una corrispondenza
                // Viene saltata almeno una pagina e quella selezionata viene riempita parzialmente, l'output deve
                // essere ordinato.
                Arguments.of(
                        "/",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.and(
                                SearchCondBuilder.dynRealm("waterTypes"),
                                SearchCondBuilder.and(
                                        SearchCondBuilder.attr("hp", AttrCond.Type.GT, "79"),
                                        SearchCondBuilder.attr("hp", AttrCond.Type.LT, "92")
                                )
                        ),
                        PageRequest.of(2, 2, Sort.by("name")),
                        AnyTypeKind.ANY_OBJECT,
                        List.of("mistys_golduck"),
                        false,
                        5,
                        false
                ),
                // [17] base è un nodo foglia, recursive == false e non ci si aspettano corrispondenze.
                // [26] base è un nodo interno, recursive == true e ci si aspetta almeno una corrispondeza.
                // Viene saltata almeno una pagina e quella corrente viene riempita completamente
                Arguments.of(
                        "/kanto",
                        true,
                        Set.of("/kanto"),
                        SearchCondBuilder.dynRealm("goodTrainers"),
                        PageRequest.of(6, 1, Sort.by(Sort.Order.desc("trainerId"))),
                        AnyTypeKind.USER,
                        List.of("brock"),
                        true,
                        8,
                        false
                ),
                Arguments.of(
                        "/kanto/league/gyms/cerulean",
                        false,
                        Set.of("/kanto/league/gyms/cerulean"),
                        SearchCondBuilder.not(SearchCondBuilder.any("email", AttrCond.Type.ISNOTNULL)),
                        Pageable.unpaged(),
                        AnyTypeKind.GROUP,
                        List.of("Sailors", "Swimmers", "Women"),
                        false,
                        3,
                        SKIP_TEST
                ),
                Arguments.of(
                        "/kanto/league/gyms/cerulean",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.any("creator", AttrCond.Type.ISNULL),
                        Pageable.unpaged(),
                        AnyTypeKind.GROUP,
                        List.of("Sailors", "Swimmers", "Women"),
                        false,
                        3,
                        false
                ),
                Arguments.of(
                        "/",
                        false,
                        Set.of("/"),
                        SearchCondBuilder.and(
                                SearchCondBuilder.anyType("MOVE"),
                                SearchCondBuilder.and(
                                        SearchCondBuilder.attrIsNull("power"),
                                        SearchCondBuilder.any("name", AttrCond.Type.ILIKE, "a%")
                                )
                        ),
                        Pageable.unpaged(),
                        AnyTypeKind.ANY_OBJECT,
                        List.of("Attract", "Agility", "Amnesia", "Aqua Ring"),
                        false,
                        4,
                        false

                ),
                Arguments.of(
                        "/",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.and(
                                SearchCondBuilder.dynRealm("waterTypes"),
                                SearchCondBuilder.or(
                                        SearchCondBuilder.attr("shiny", AttrCond.Type.EQ, "true"),
                                        SearchCondBuilder.and(
                                                SearchCondBuilder.attr(
                                                        "shiny",
                                                        AttrCond.Type.EQ,
                                                        "false"
                                                ),
                                                SearchCondBuilder.attr(
                                                        "weight",
                                                        AttrCond.Type.LE,
                                                        "10.0"
                                                )
                                        )
                                )
                        ),
                        PageRequest.of(0, 2, Sort.by("weight")),
                        AnyTypeKind.ANY_OBJECT,
                        List.of("parkers_horsea"),
                        false,
                        1,
                        false
                ),
                Arguments.of(
                        "/kanto/league/gyms/cerulean",
                        true,
                        Set.of("/kanto/league/gyms"),
                        SearchCondBuilder.membership("W.*1"),
                        PageRequest.of(1, 3, Sort.by(Sort.Order.desc("name"))),
                        AnyTypeKind.ANY_OBJECT,
                        List.of("mistys_lapras", "mistys_golduck", "eddies_azumarill"),
                        true,
                        7,
                        false
                ),
                Arguments.of(
                        "/",
                        true,
                        Set.of("waterTypes", "/kanto/league/gyms/cerulean"),
                        SearchCondBuilder.anyType("POKEMON"),
                        PageRequest.of(0, 10, Sort.by(Sort.Order.asc("name"))),
                        AnyTypeKind.ANY_OBJECT,
                        List.of("brianas_seaking_1", "brianas_seaking_2", "dianas_golduck", "eddies_azumarill",
                            "greens_gyarados", "joys_cloyster", "lances_gyarados", "loreleis_cloyster",
                            "loreleis_dewgong", "loreleis_lapras"),
                        true,
                        17,
                        false
                ),
                Arguments.of(
                        "/",
                        true,
                        Set.of("/kanto/league/gyms/cerulean@Sailors"),
                        SearchCondBuilder.not(SearchCondBuilder.membership("Sailors")),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        List.of(),
                        false,
                        0,
                        false
                ),
                Arguments.of(
                        "/kanto/league/gyms/vermilion",
                        true,
                        Set.of("/kanto/league/gyms/vermilion", "invalidDynamicRealm"),
                        SearchCondBuilder.not(SearchCondBuilder.anyType("POKEMON")),
                        Pageable.unpaged(),
                        AnyTypeKind.ANY_OBJECT,
                        List.of(),
                        false,
                        0,
                        false
                ),
                Arguments.of(
                        "/kanto/league/gyms/vermilion",
                        true,
                        Set.of("/kanto/league/gyms/vermilion", "invalidDynamicRealm"),
                        SearchCondBuilder.not(SearchCondBuilder.auxClass("TrainerMetadata")),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        List.of(),
                        false,
                        0,
                        false
                ),
                Arguments.of(
                        "/",
                        false,
                        Set.of("/"),
                        SearchCondBuilder.not(SearchCondBuilder.relationshipType("ownership")),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        List.of(),
                        false,
                        0,
                        false
                ),
                Arguments.of(
                        "/",
                        false,
                        Set.of("/"),
                        SearchCondBuilder.not(SearchCondBuilder.relationshipType("knowledge")),
                        Pageable.unpaged(),
                        AnyTypeKind.ANY_OBJECT,
                        List.of(),
                        false,
                        0,
                        false
                ),
                // [BUG]
                // È necessario saltare questo test perché sembra esserci un bug nella traduzione di una query che usa
                // NOT(RelationshipCond). Il DAO esegue getQuery() [L#264] per la traduzione, in particolare il codice
                // preposto si trova in [L#282]:
                //
                // + "WHERE anyObject.id " + (not ? "NOT " : "") + "IN $" + setParameter(parameters, rightAnyObjects)
                //
                // ma dovrebbe essere
                //
                // + "WHERE " + (not ? "NOT " : "") + "anyObject.id IN $" + setParameter(parameters, rightAnyObjects)
                //
                Arguments.of(
                        "/kanto/league/gyms/pewter",
                        false,
                        Set.of("/kanto/league/gyms/pewter"),
                        SearchCondBuilder.not(
                                SearchCondBuilder.relationship("00fa0eb4-5c5f-48d4-0df4-07b82751ab86")
                        ),
                        PageRequest.of(0, 2, Sort.by(Sort.Order.asc("username"))),
                        AnyTypeKind.USER,
                        List.of("brock", "edwin"),
                        true,
                        2,
                        SKIP_TEST
                ),
                Arguments.of(
                        "/kanto/league/gyms/cerulean",
                        true,
                        Set.of("/kanto/league/gyms"),
                        SearchCondBuilder.not(
                                SearchCondBuilder.member("7c462d53-2e40-43fb-935e-42989c676a5a")
                        ),
                        Pageable.unpaged(),
                        AnyTypeKind.GROUP,
                        List.of("Swimmers", "Women"),
                        false,
                        2,
                        false
                ),
                Arguments.of(
                        "/kanto/league/gyms",
                        true,
                        Set.of("/kanto/league/gyms"),
                        SearchCondBuilder.not(
                                SearchCondBuilder.role("GymTrainer")
                        ),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        List.of("lt_surge", "misty", "brock"),
                        false,
                        3,
                        false
                ),
                Arguments.of(
                        "/",
                        true,
                        Set.of("/kanto/league/gyms"),
                        SearchCondBuilder.and(
                                SearchCondBuilder.dynRealm("rockTypes"),
                                SearchCondBuilder.not(SearchCondBuilder.dynRealm("groundTypes"))
                        ),
                        PageRequest.of(2, 2, Sort.by("name")),
                        AnyTypeKind.ANY_OBJECT,
                        List.of("brocks_omastar", "brocks_kabutops"),
                        false,
                        2,
                        false
                ),
                Arguments.of(
                        "/kanto/league/gyms",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.and(
                                SearchCondBuilder.not(SearchCondBuilder.resource("local-cerulean")),
                                SearchCondBuilder.role("GymLeader")
                        ),
                        PageRequest.of(0, 10, Sort.by(Sort.Order.asc("username"))),
                        AnyTypeKind.USER,
                        List.of("brock", "lt_surge"),
                        true,
                        2,
                        false
                ),
                Arguments.of(
                        "/kanto/league",
                        false,
                        Set.of("/kanto/league"),
                        SearchCondBuilder.not(
                                SearchCondBuilder.attr(
                                        "email",
                                        AttrCond.Type.EQ,
                                        "green@indigoleague.org"
                                )
                        ),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        List.of("bruno", "lorelei", "lance", "agatha"),
                        false,
                        4,
                        false
                ),
                Arguments.of(
                        "/kanto/league",
                        false,
                        Set.of("/kanto/league"),
                        SearchCondBuilder.and(
                                SearchCondBuilder.attr(
                                        "email",
                                        AttrCond.Type.ILIKE,
                                        "%@iNDIGOlEAGUE%"
                                ),
                                SearchCondBuilder.not(
                                        SearchCondBuilder.attr(
                                                "trainerId",
                                                AttrCond.Type.IEQ,
                                                "000001"
                                        )
                                )
                        ),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        List.of("bruno", "lorelei", "lance", "agatha"),
                        false,
                        4,
                        false
                ),
                // [Coverage Info]
                // Il codice che gestisce la traduzione di NOT(ISNULL(any)) è
                // stato inserito due volte nella funzione preposta fillAttrQuery(...) [L#518], in particolare in:
                // [L#526:530] e [L#537:541]. Per come è stato scritto, la seconda regione di codice non può essere
                // eseguita cioè il codice è morto e non può essere coperto.
                Arguments.of(
                        "/kanto",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.and(
                                SearchCondBuilder.not(
                                        SearchCondBuilder.attr("loginDate", AttrCond.Type.ISNULL)
                                ),
                                SearchCondBuilder.attr("loginDate", AttrCond.Type.GT, "2026-06-01")
                        ),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        List.of("green", "lt_surge", "lorelei", "eddie", "horton"),
                        false,
                        5,
                        SKIP_TEST
                ),
                Arguments.of(
                        "/kanto",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.and(
                                SearchCondBuilder.attr("loginDate", AttrCond.Type.ISNOTNULL),
                                SearchCondBuilder.or(
                                        SearchCondBuilder.attr(
                                                "loginDate",
                                                AttrCond.Type.GT,
                                                "2026-06-01"
                                        ),
                                        SearchCondBuilder.attr(
                                                "loginDate",
                                                AttrCond.Type.LIKE,
                                                "2026-%-%"
                                        )
                                )
                        ),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        List.of("green", "lt_surge", "lorelei", "eddie", "horton"),
                        false,
                        5,
                        false
                ),
                // [BUG + Coverage INFO]
                // Questo test fallisce perché NOT non ha effetto né su AttrCond.Type.ISNOTNULL né su
                // AttrCond.Type.ISNULL. A dire il vero sembra esserci un po' di confusione nel codice perché
                // se da una parte quando la condizione incontrata è una delle due sopra si ignora la negazione
                // (vedi getQuery() [L#686]) sembra che fillAttrQuery() [L#396] che viene invocato per tutte le altre
                // condizioni eccetto ISNULL e ISNOTNULL si occupi di applicare la negazione alle condizioni
                // sopracitate (in pratica il codice nel metodo fillAttrQuery() in [L#400-404] e [L#454-458]
                // è codice morto. Infatti il seguente fallisce
                Arguments.of(
                        "/kanto",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.not(
                                SearchCondBuilder.attr("loginDate", AttrCond.Type.ISNOTNULL)
                        ),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        List.of("green", "lt_surge", "lorelei", "eddie", "horton", "misty", "brock"),
                        false,
                        7,
                        SKIP_TEST
                ),
                Arguments.of(
                        "/kanto/league/gyms/cerulean",
                        true,
                        Set.of("/kanto/league"),
                        SearchCondBuilder.and(
                                SearchCondBuilder.not(SearchCondBuilder.any("status", AttrCond.Type.ISNULL)),
                                SearchCondBuilder.not(
                                        SearchCondBuilder.any("username", AttrCond.Type.ILIKE, "%E%")
                                )
                        ),
                        PageRequest.of(0, 2),
                        AnyTypeKind.USER,
                        List.of("briana", "joy"),
                        false,
                        2,
                        false
                ),
                Arguments.of(
                        "/",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.any("creationDate", AttrCond.Type.LIKE, "2000%"),
                        Pageable.unpaged(),
                        AnyTypeKind.GROUP,
                        List.of(),
                        false,
                        0,
                        false
                ),
                Arguments.of(
                        "/",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.or(
                                SearchCondBuilder.any("name", AttrCond.Type.EQ, "Elite 4"),
                                SearchCondBuilder.any("name", AttrCond.Type.IEQ, "eLITE 4")
                        ),
                        Pageable.unpaged(),
                        AnyTypeKind.GROUP,
                        List.of("Elite 4"),
                        false,
                        1,
                        false
                ),
                Arguments.of(
                        "/",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.or(
                                SearchCondBuilder.any("username", AttrCond.Type.GE, "p"),
                                SearchCondBuilder.and(
                                        SearchCondBuilder.any("username", AttrCond.Type.GT, "m"),
                                        SearchCondBuilder.any("username", AttrCond.Type.LE, "o")
                                )
                        ),
                        PageRequest.of(0, 10, Sort.by(Sort.Order.asc("username"))),
                        AnyTypeKind.USER,
                        List.of("misty", "parker", "vincent"),
                        false,
                        3,
                        false
                ),
                // [Coverage INFO]
                // Il test passa, però questo caso di test evidenzia la presenza di codice morto in
                // fillAttrQuery() in L#518 che si occupa della traduzione di un oggetto AnyCond. L'autore ha gestito
                // la traduzione di una clausola NOT in L#531:536 però molto probabilmente questa sezione di codice è
                // stata aggiunta dopo perché se si guarda in L#585:623 si osserva che è ancora presente la logica
                // per applicare la negazione, questo codice non è raggiungibile.
                Arguments.of(
                        "/",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.or(
                                SearchCondBuilder.not(
                                        SearchCondBuilder.any("username", AttrCond.Type.LT, "p")
                                ),
                                SearchCondBuilder.and(
                                        SearchCondBuilder.not(
                                                SearchCondBuilder.any(
                                                        "username",
                                                        AttrCond.Type.LE,
                                                        "m"
                                                )
                                        ),
                                        SearchCondBuilder.not(
                                                SearchCondBuilder.any(
                                                        "username",
                                                        AttrCond.Type.GE,
                                                        "o"
                                                )
                                        )
                                )
                        ),
                        PageRequest.of(0, 10, Sort.by(Sort.Order.asc("username"))),
                        AnyTypeKind.USER,
                        List.of("misty", "parker", "vincent"),
                        false,
                        3,
                        false
                ),
                Arguments.of(
                        "/kanto/league",
                        false,
                        Set.of("/kanto/league"),
                        SearchCondBuilder.or(
                                SearchCondBuilder.any("realm", AttrCond.Type.EQ, "/kanto/league"),
                                SearchCondBuilder.any(
                                        "realm",
                                        AttrCond.Type.EQ,
                                        "b1758e7c-bae8-4ed3-8fa6-0412b092a569"
                                )
                        ),
                        PageRequest.of(0, 4, Sort.by(Sort.Order.asc("username"))),
                        AnyTypeKind.USER,
                        List.of("agatha", "bruno", "green", "lance"),
                        true,
                        5,
                        false
                ),
                Arguments.of(
                        "/kanto/league/gyms",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.or(
                                SearchCondBuilder.any(
                                        "realm",
                                        AttrCond.Type.EQ,
                                        "56e0366e-0630-42ff-bd58-d4f87d80c7a7"),
                                SearchCondBuilder.any(
                                        "realm",
                                        AttrCond.Type.IEQ,
                                        "/kanto/league/gyms/pewter"
                                )
                        ),
                        PageRequest.of(0, 3, Sort.by(Sort.Order.desc("username"))),
                        AnyTypeKind.USER,
                        List.of("jerry", "edwin", "brock"),
                        false,
                        3,
                        false
                ),
                Arguments.of(
                        "/",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.or(
                                SearchCondBuilder.any(
                                        "userOwner",
                                        AttrCond.Type.EQ,
                                        "177b6095-ba07-4423-a2fd-bd767501fe78"
                                ),
                                SearchCondBuilder.any(
                                        "groupOwner",
                                        AttrCond.Type.EQ,
                                        "3f173b2a-5cff-4be3-88e5-e3ca1b0c2f0a"
                                )
                        ),
                        Pageable.unpaged(),
                        AnyTypeKind.GROUP,
                        List.of(),
                        false,
                        0,
                        false
                ),
                Arguments.of(
                        "/kanto",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.and(
                                SearchCondBuilder.any("name", AttrCond.Type.LIKE, "brocks_%"),
                                SearchCondBuilder.attr("atk", AttrCond.Type.GT, "80")
                        ),
                        PageRequest.of(0, 10, Sort.by(Sort.Order.asc("id"))),
                        AnyTypeKind.ANY_OBJECT,
                        List.of("brocks_graveler", "brocks_kabutops", "brocks_rhyhorn"),
                        true,
                        3,
                        false
                ),
                Arguments.of(
                        "/kanto/league",
                        true,
                        Set.of("/kanto/league/gyms/pewter"),
                        SearchCondBuilder.not(
                                SearchCondBuilder.attr("profilePicture", AttrCond.Type.EQ, "12")
                        ),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        List.of("jerry", "edwin", "brock"),
                        false,
                        3,
                        false
                ));
    }

    @ParameterizedTest
    @MethodSource("coverageInputs")
    public void test(
            final String basePath,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final Pageable pageable,
            final AnyTypeKind kind,
            final List<String> expectedIds,
            final boolean sorted,
            final long expectedCount,
            final boolean skip
    ) {
        Assumptions.assumeFalse(
                skip,
                """
                Caso di test {
                    basePath: "%s",
                    recursive: %s,
                    adminRealms: %s,
                    cond: %s,
                    pageable: %s,
                    kind: %s,
                }
                """.formatted(
                        basePath,
                        recursive,
                        String.join(",", adminRealms),
                        cond.toString(),
                        pageable.toString(),
                        kind.toString()
                )
        );
        final Realm base = realmSearchDAO.findByFullPath(basePath).orElseThrow();
        AuthContextUtils.callAsAdmin(
                SyncopeConstants.MASTER_DOMAIN, () -> {
                    List<? extends Any> result = searchDAO.search(base, recursive, adminRealms, cond, pageable, kind);
                    long count = searchDAO.count(base, recursive, adminRealms, cond, kind);
                    Assertions.assertEquals(expectedCount, count);
                    List<String> ids = result.stream().map(e -> switch (e) {
                        case AnyObject o -> o.getName();
                        case Group g -> g.getName();
                        case User u -> u.getUsername();
                        default -> throw new IllegalArgumentException("unexpected type: " + e.getClass().getName());
                    }).toList();
                    if (sorted) {
                        Assertions.assertIterableEquals(expectedIds, ids);
                    } else {
                        subsetOf(expectedIds, ids);
                    }
                    return null;
                });
    }

    @Test
    public void testMoreThanOneOrdering() {
        final var realm = realmSearchDAO.findByFullPath("/").orElseThrow();
        AuthContextUtils.callAsAdmin(
                SyncopeConstants.MASTER_DOMAIN, () -> {
                    Assertions.assertThrows(
                            SyncopeClientException.class,
                            () -> searchDAO.search(realm, true, Set.of("/"),
                                    SearchCondBuilder.and(
                                            SearchCondBuilder.attr("hp", AttrCond.Type.GT, "79"),
                                            SearchCondBuilder.attr("hp", AttrCond.Type.LT, "92")
                                    ),
                                    Pageable.unpaged(Sort.by(
                                            Sort.Order.desc("hp"),
                                            Sort.Order.by("spe"))),
                                    AnyTypeKind.ANY_OBJECT)
                    );
                    return null;
                }
        );
    }

    @Test
    public void testMoreThanOneUniqueOrdering() {
        final var realm = realmSearchDAO.findByFullPath("/").orElseThrow();
        AuthContextUtils.callAsAdmin(
                SyncopeConstants.MASTER_DOMAIN, () -> {
                    Assertions.assertThrows(
                            SyncopeClientException.class,
                            () -> searchDAO.search(realm, true, Set.of("/"),
                                    SearchCondBuilder.attr("email", AttrCond.Type.ISNOTNULL),
                                    Pageable.unpaged(Sort.by(
                                            Sort.Order.desc("trainerId"),
                                            Sort.Order.by("email"))),
                                    AnyTypeKind.USER)
                    );
                    return null;
                }
        );
    }

    @Test
    public void testIllegalBooleanCondition() {
        AuthContextUtils.callAsAdmin(
                SyncopeConstants.MASTER_DOMAIN, () -> {
                    final var realm = realmSearchDAO.findByFullPath("/").orElseThrow();
                    Assertions.assertIterableEquals(
                            List.of(),
                            searchDAO.search(
                                    realm,
                                    true,
                                    Set.of("/"),
                                    SearchCondBuilder.attr("shiny", AttrCond.Type.EQ, "ALSE"),
                                    Pageable.unpaged(),
                                    AnyTypeKind.ANY_OBJECT));
                    Assertions.assertEquals(
                            0,
                            searchDAO.count(
                                    realm,
                                    true,
                                    Set.of("/"),
                                    SearchCondBuilder.attr("shiny", AttrCond.Type.EQ, "fals"),
                                    AnyTypeKind.ANY_OBJECT));
                    return null;
                }
        );
    }

    // [Coverage INFO]
    // La funzione getQuery()@L#630 non è completamente coperta perché non è possibile che la
    // variabile field sia diversa da "userOwner" o "groupOwner" quindi non è possibile eseguire il ramo
    // default dello switch.
    @Test
    public void testIllegalSchemaClause() {
        AuthContextUtils.callAsAdmin(
                SyncopeConstants.MASTER_DOMAIN, () -> {
                    final var realm = realmSearchDAO.findByFullPath("/").orElseThrow();
                    Assertions.assertThrows(
                            IllegalArgumentException.class,
                            () -> searchDAO.search(
                                    realm,
                                    true,
                                    Set.of("/"),
                                    SearchCondBuilder.any("invalidSchema", AttrCond.Type.ISNOTNULL),
                                    Pageable.unpaged(),
                                    AnyTypeKind.ANY_OBJECT));
                    Assertions.assertThrows(
                            IllegalArgumentException.class,
                            () -> searchDAO.count(
                                    realm,
                                    true,
                                    Set.of("/"),
                                    SearchCondBuilder.any("invalidSchema", AttrCond.Type.ISNOTNULL),
                                    AnyTypeKind.ANY_OBJECT));
                    return null;
                }
        );
    }

    @Test
    public void testIllegalRealmClause() {
        AuthContextUtils.callAsAdmin(
                SyncopeConstants.MASTER_DOMAIN, () -> {
                    final var realm = realmSearchDAO.findByFullPath("/").orElseThrow();
                    Assertions.assertThrows(
                            IllegalArgumentException.class,
                            () -> searchDAO.search(
                                    realm,
                                    true,
                                    Set.of("/"),
                                    SearchCondBuilder.any("realm", AttrCond.Type.EQ, "/invalid/realm"),
                                    Pageable.unpaged(),
                                    AnyTypeKind.ANY_OBJECT));
                    Assertions.assertThrows(
                            IllegalArgumentException.class,
                            () -> searchDAO.count(
                                    realm,
                                    true,
                                    Set.of("/"),
                                    SearchCondBuilder.any("realm", AttrCond.Type.EQ, "/invalid/realm"),
                                    AnyTypeKind.ANY_OBJECT));
                    return null;
                }
        );
    }

    private void createNonGymTrainerRealm() {
        var dynRealm = createDynRealm("goodTrainers", "USER", "$roles!=GymTrainer").orElseThrow();
        var nonGymTrainers = List.of(
                "green", "brock", "misty", "lt_surge",
                "lorelei", "bruno", "agatha", "lance"
        );
        for (var username : nonGymTrainers) {
            var user = userDAO.findByUsername(username).orElseThrow();
            addToDynRealm(dynRealm, user);
        }
    }

    private void createGroundRealm() {
        var dynRealm = createDynRealm("groundTypes", "POKEMON", "aType==Ground").orElseThrow();
        var groundTypes = List.of("mistys_quagsire", "brocks_graveler", "brocks_rhyhorn", "brocks_onix",
                "brunos_onix_1", "brunos_onix_2", "greens_rhydon", "jerrys_rhydon", "edwins_golem"
        );
        for (var name : groundTypes) {
            var anyObj = anyObjectDAO.findByName(name).getFirst();
            addToDynRealm(dynRealm, anyObj);
        }
    }

    private void createRockRealm() {
        var dynRealm = createDynRealm("rockTypes", "POKEMON", "aType==Rock").orElseThrow();
        var rockTypes = List.of("brocks_graveler", "brocks_omastar", "brocks_kabutops", "brocks_onix", "brunos_onix_1",
            "brunos_onix_2", "lances_aerodactyl", "edwins_golem");
        for (var name : rockTypes) {
            var anyObj = anyObjectDAO.findByName(name).getFirst();
            addToDynRealm(dynRealm, anyObj);
        }
    }

    private void createWaterRealm() {
        var dynRealm = createDynRealm("waterTypes", "POKEMON", "aType==Water").orElseThrow();
        var waterTypes = List.of("mistys_golduck", "mistys_quagsire", "mistys_lapras", "mistys_starmie",
                "loreleis_dewgong", "loreleis_cloyster", "loreleis_slowbro", "loreleis_lapras", "lances_gyarados",
                "greens_gyarados", "parkers_horsea", "parkers_seadra", "eddies_azumarill", "dianas_golduck",
                "joys_cloyster", "brianas_seaking_1", "brianas_seaking_2");
        for (var name : waterTypes) {
            var anyObj = anyObjectDAO.findByName(name).getFirst();
            addToDynRealm(dynRealm, anyObj);
        }
    }

    private Optional<DynRealm> createDynRealm(final String key, final String anyTypeKey, final String fiql) {
        if (dynRealmDAO.findById(key).isEmpty()) {
            DynRealm dyn = entityFactory.newEntity(DynRealm.class);
            dyn.setKey(key);
            DynRealmMembership membership = entityFactory.newEntity(DynRealmMembership.class);
            membership.setDynRealm(dyn);
            membership.setAnyType("USER".equals(anyTypeKey)
                    ? anyTypeDAO.getUser()
                    : anyTypeDAO.findById(anyTypeKey).orElseThrow());
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

    private void addToDynRealm(final DynRealm realm, final AnyObject anyObject) {
        neo4jClient.query(
                        "MATCH (a:AnyObject {id: $id}) "
                                + "MATCH (d:DynRealm {id: $dynRealmId}) "
                                + "MERGE (a)-[:DYN_REALM_MEMBERSHIP]->(d)"
                ).bind(anyObject.getKey()).to("id")
                .bind(realm.getKey()).to("dynRealmId")
                .run();
    }

    private void subsetOf(final List<String> superset, final Iterable<String> subset) {
        for (String element : subset) {
            Assertions.assertTrue(superset.contains(element));
        }
    }
}
