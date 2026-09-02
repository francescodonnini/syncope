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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class Neo4jAnySearchDAOTestBB extends AbstractNeo4jAnySearchDAOTest {
    private static final boolean SKIP_TEST = true;

    static Stream<Arguments> inputs() {
        return Stream.of(
                // [0] Il DAO lancia un'eccezione dovuto un errore nella generazione della query e non credo sia
                // il comportamento appropriato, il DAO dovrebbe ritornare una lista vuota.
                Arguments.of(
                        "/kanto/league/gyms/vermilion",
                        true,
                        Set.of("/kanto/league/gyms/pewter"),
                        SearchCondBuilder.attr("aType", AttrCond.Type.EQ, "Electric"),
                        Pageable.unpaged(),
                        AnyTypeKind.ANY_OBJECT,
                        List.of(),
                        false,
                        0,
                        SKIP_TEST
                ),
                // [1] base è un nodo interno presente nel database. Il predicato usa AuxClassCond e ci si aspetta
                // almeno una corrispondenza. Non ci si aspetta un'impaginazione dei risultati.
                Arguments.of(
                        "/kanto/league",
                        false,
                        Set.of("/kanto/league"),
                        SearchCondBuilder.and(
                                SearchCondBuilder.auxClass("TrainerMetadata"),
                                SearchCondBuilder.any("username", AttrCond.Type.ILIKE, "%E%")
                        ),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        List.of("green", "lorelei", "lance"),
                        false,
                        3,
                        false
                ),
                // [2] base è un nodo foglia presente nel database. Il predicato usa MembershipCond
                // e ci si aspetta almeno una corrispondenza. Si salta la prima pagine dei risultati e la successiva non
                // è riempita completamente. I risultati non vengono ordinati.
                Arguments.of(
                        "/kanto/league/gyms/cerulean",
                        true,
                        Set.of("/kanto/league/gyms/pewter", "/kanto/league/gyms/cerulean"),
                        SearchCondBuilder.membership("8ab844e4-dca0-46ac-93de-3c12ddfd6aff"),
                        PageRequest.of(1, 4),
                        AnyTypeKind.ANY_OBJECT,
                        List.of("mistys_golduck", "mistys_quagsire", "mistys_lapras", "parkers_horsea",
                                "parkers_seadra", "eddies_azumarill", "dianas_golduck"),
                        false,
                        7,
                        false
                ),
                // [3] base è un nodo foglia presente nel database. Il predicato usa RelationshipCond e ci si aspetta
                // almenouna corrispondenza.
                Arguments.of(
                        "/kanto/league/gyms/vermilion",
                        false,
                        Set.of("/kanto/league/gyms/cerulean", "/kanto/league/gyms"),
                        SearchCondBuilder.relationship("5780be17-12fe-cc2b-1acf-42c5f3a02120"),
                        PageRequest.of(0, 2, Sort.by(Sort.Order.by("trainerId"))),
                        AnyTypeKind.USER,
                        List.of("briana"),
                        true,
                        1,
                        false
                ),
            // [4] base è un nodo interno presente nel database. Il predicato usa AnyTypeCond e ci si aspetta almeno
            // una corrispondenza. Si vuole la prima pagina dei risultati, il numero di corrispondenze eccede la pagina.
            // The number of matches will exceed the size of a page
            Arguments.of(
                    "/",
                    true,
                    Set.of("/"),
                    SearchCondBuilder.anyType("MOVE"),
                    PageRequest.of(0, 119),
                    AnyTypeKind.ANY_OBJECT,
                    List.of("Light Screen", "Shadow Ball", "Double-Edge", "Sand Tomb", "Curse",
                        "Flamethrower", "Attract", "Aerial Ace", "Solar Beam", "Brick Break", "Sing", "Dream Eater",
                        "Counter", "Roar", "Take Down", "Yawn", "Rain Dance", "Growth", "Aqua Tail", "Shock Wave",
                        "Spark", "Recover", "Double Slap", "Sand Attack", "Scary Face", "Iron Tail", "Endure",
                       "Feather Dance", "Cotton Spore", "Body Slam", "Safeguard", "Synthesis", "Sandstorm",
                        "Defense Curl", "Cross Chop", "Shadow Punch", "Icicle Spear", "Mega Kick", "Screech",
                        "Twister", "Aqua Ring", "Wing Attack", "Calm Mind", "Sludge Bomb", "Smokescreen",
                        "Future Sight", "Whirlwind", "Mean Look", "Hail", "Horn Drill", "Poison Jab", "Tackle",
                        "Rock Blast", "Disable", "Magnet Bomb", "Rock Slide", "Pin Missile", "Water Pulse", "Surf",
                        "Thunderbolt", "Megahorn", "Brine", "Spikes", "Stone Edge", "Supersonic", "Charge Beam",
                        "Earthquake", "Thunder Fang", "Facade", "Psychic", "Thunder Wave", "Hydro Pump",
                        "Nightmare", "Bite", "Explosion", "Sunny Day", "Waterfall", "Amnesia", "Sucker Punch",
                        "Ancient Power", "Rock Tomb", "Air Cutter", "Charge", "Double Team", "Low Kick", "Sky Uppercut",
                        "Spike Cannon", "Ice Beam", "Giga Drain", "Poison Fang", "Swords Dance", "Extreme Speed",
                        "Mirror Shot", "Rollout", "Psych Up", "Quick Attack", "Reflect", "Foresight", "Discharge",
                        "Lovely Kiss", "Swift", "Double Kick", "Agility", "Bulk Up", "Self-Destruct", "Dragon Pulse",
                        "Confuse Ray", "Hypnosis", "Dragon Rage", "Toxic", "Thrash", "Protect", "Outrage", "Hyper Beam",
                        "Mach Punch", "Superpower", "Aqua Jet", "Sonic Boom", "Dive", "Ice Punch"),
                    false,
                    120,
                    false
            ),
            // [5] base è un nodo interno presente nel database. Il predicato usa `MemberCond` e ci si aspetta almeno
            // una corrispondenza. Si prende la seconda pagina dei risultati
            Arguments.of(
                "/",
                    false,
                    Set.of("/"),
                    SearchCondBuilder.member("bcf2fc3b-d958-74c2-6053-8c1ba84510a1"),
                    PageRequest.of(1, 1),
                    AnyTypeKind.GROUP,
                    List.of("Field", "Dragon"),
                    false,
                    2,
                    false
            ),
            // [7] base è un nodo interno e presente nel database. Si usa AnyTypeCond e ci si aspetta almeno una
            // corrispondenza. Si deve saltare almeno una pagina e i risultati devono essere ordinati.
            Arguments.of(
                    "/",
                    true,
                    Set.of("/"),
                    SearchCondBuilder.anyType("MOVE"),
                    PageRequest.of(1, 10),
                    AnyTypeKind.ANY_OBJECT,
                    List.of("Body Slam", "Brick Break", "Brine", "Bulk Up", "Calm Mind", "Charge", "Charge Beam",
                        "Confuse Ray", "Cotton Spore", "Counter"),
                    false,
                    120,
                    false
            ),
            // [8] base è un nodo foglia presente nel database. Si usa RoleCond e ci si aspetta almeno
            // una corrispondenza, viene saltata la prima pagina.
            Arguments.of(
                    "/kanto/league/gyms/vermilion",
                    true,
                    Set.of("/kanto/league/gyms/pewter", "/kanto/league/gyms/vermilion"),
                    SearchCondBuilder.role("GymTrainer"),
                    PageRequest.of(1, 2),
                    AnyTypeKind.USER,
                    List.of("horton", "vincent", "gregory"),
                    false,
                    3,
                    false
            ),
            // [9] base è un nodo foglia presente nel database. Si usa RelationshipCond e ci si aspetta almeno
            // una corrispondenza.
            Arguments.of(
                    "/kanto/league/gyms/vermilion",
                    true,
                    Set.of("/"),
                    SearchCondBuilder.relationship("93829b43-922f-e15a-e1e3-db63ef7ddc76"),
                    Pageable.unpaged(),
                    AnyTypeKind.ANY_OBJECT,
                    List.of("surges_raichu", "surges_electrode_1", "hortons_electrode_3", "vincents_magnemite",
                            "gregorys_pikachu", "gregorys_flaaffy"),
                    false,
                    6,
                    false
            ),
            // [10] base è un nodo foglia e presente nel database. Si usa MemberCond e ci si aspetta almeno
            // una corrispondenza.
            Arguments.of(
                    "/kanto/league/gyms/cerulean",
                    true,
                    Set.of("/"),
                    SearchCondBuilder.member("7c462d53-2e40-43fb-935e-42989c676a5a"),
                    Pageable.unpaged(),
                    AnyTypeKind.GROUP,
                    List.of("Sailors"),
                    false,
                    1,
                    false
            ),
            // [11] base è un nodo foglia e presente nel database. Si usa RelTypeCond ma non si hanno i permessi
            // sufficienti per ispezionare gli oggetti del nodo.
            Arguments.of(
                    "/kanto/league/gyms/pewter",
                    false,
                    Set.of("/kanto/league/gyms/pewter"),
                    SearchCondBuilder.relationshipType("ownership"),
                    Pageable.unpaged(),
                    AnyTypeKind.ANY_OBJECT,
                    List.of(),
                    false,
                    0,
                    false
            ),
            // [13] base è un nodo foglia e non è presente nel database. Si usa RoleCond e ci si
            // aspetta almeno una corrispondenza. Viene saltata la prima pagina e i risultati sono
            // ordinati per nome.
            // __ATTENZIONE:__ recursive == false fa fallire il seguente test se si specifica come adminRealms
            // un insieme di reami che ha un genitore di base perché sembra che in questo caso la ricerca parte da
            // adminRealms e non da base. In particolare la configurazione sotto (commentata) produce come risultato
            // tutti gli allenatori che si trovano nei sotto-reami di "/kanto/league/gyms" invece di considerare
            // esclusivamente gli utenti del reame indicato da base.
            Arguments.of(
                    "/kanto/league/gyms/cerulean",
                    false,
                    Set.of("/kanto/league/gyms"),
                    SearchCondBuilder.role("GymTrainer"),
                    PageRequest.of(1, 2, Sort.by(Sort.Order.by("firstName"))),
                    AnyTypeKind.USER,
                    List.of("eddie", "joy"),
                    true,
                    5,
                    SKIP_TEST
            ),
            Arguments.of(
                    "/kanto/league/gyms/cerulean",
                    false,
                    Set.of("/kanto/league/gyms/cerulean"),
                    SearchCondBuilder.role("GymTrainer"),
                    PageRequest.of(1, 2, Sort.by(Sort.Order.by("firstName"))),
                    AnyTypeKind.USER,
                    List.of("eddie", "joy"),
                    true,
                    5,
                    SKIP_TEST
            ),
            // fallisce perché count() ritorna il numero di tutti gli allenatori nel reame /kanto/league/gyms
            // mentre se recursive == true count() ritorna il numero corretto cioè 5 che è il numero di allenatori
            // nella palestra /kanto/league/gyms/cerulean.
            // [14] base è un nodo foglia presente nel database. cond usa RoleCond e ci si aspetta almeno
            // una corrispondenza. Viene saltata una pagina e il numero degli oggetti restanti supera la dimensione
            // della seconda pagina
            Arguments.of(
                "/kanto/league/gyms/cerulean",
                    false,
                    Set.of("/kanto/league/gyms/cerulean"),
                    SearchCondBuilder.role("GymTrainer"),
                    PageRequest.of(1, 2, Sort.by(Sort.Order.by("firstName"))),
                    AnyTypeKind.USER,
                    List.of("eddie", "joy"),
                    true,
                    5,
                    false
            ),
            // [15] base è un nodo interno presente nel database. cond usa MemberCond e il numero di
            // risultati (che vengono ordinati per nome) superano la dimensione della pagina.
            Arguments.of(
                "/",
                    true,
                    Set.of("/"),
                    SearchCondBuilder.member("f2a32efd-ff13-8c80-c398-c3320e500785"),
                    PageRequest.of(0, 1, Sort.by(Sort.Order.by("name"))),
                    AnyTypeKind.GROUP,
                    List.of("Dragon"),
                    true,
                    2,
                    false
            ),
            // [16] base è un nodo foglia presente nel database. cond usa AttrCond e ci si aspetta almeno
            // una corrispondenza, si prende almeno la seconda pagina dei risultati che devono essere ordinati.
            Arguments.of(
                "/kanto/league/gyms/vermilion",
                    true,
                    Set.of("/kanto/league/gyms"),
                    SearchCondBuilder.attr("level", AttrCond.Type.GE, "44"),
                    PageRequest.of(3, 2, Sort.by(Sort.Order.by("name"))),
                    AnyTypeKind.ANY_OBJECT,
                    List.of("vincents_jolteon"),
                    true,
                    7,
                    false
            ),
            // [17] base è un nodo foglia presente nel database. cond è un'espressione composta e
            // non ci si aspetta alcuna corrispondenza.
            Arguments.of(
                "/kanto/league/gyms/cerulean",
                    false,
                    Set.of("/kanto/league/gyms/cerulean"),
                    SearchCondBuilder.and(
                            SearchCondBuilder.attrEq("title", "Sailor"),
                            SearchCondBuilder.attrEq("title", "Swimmer")
                    ),
                    PageRequest.of(1, 1, Sort.by(Sort.Order.by("username"))),
                    AnyTypeKind.USER,
                    List.of(),
                    true,
                    0,
                    false
            ),
            // [18] base è un nodo foglia presente nel database. cond usa AttrCond e ci si aspetta almeno una
            // corrispondenza.
            Arguments.of(
                "/kanto/league/gyms/cerulean",
                    true,
                    Set.of("/"),
                    SearchCondBuilder.attrNotNull("email"),
                    PageRequest.of(0, 2, Sort.by(Sort.Order.by("email"))),
                    AnyTypeKind.GROUP,
                    List.of("Sailors", "Swimmers"),
                    true,
                    3,
                    false
            ),
            Arguments.of(
                "/kanto/league/gyms/cerulean",
                    true,
                    Set.of("/"),
                    SearchCondBuilder.or(
                        SearchCondBuilder.member("412728bf-8a1b-4742-9ce6-a19774c30893"),
                        SearchCondBuilder.member("32d0be33-964c-407c-9792-23278662591f")
                    ),
                    PageRequest.of(0, 2, Sort.by(Sort.Order.by("creationDate"))),
                    AnyTypeKind.GROUP,
                    List.of("Sailors", "Swimmers"),
                    true,
                    3,
                    false
            ),
            // [19] base è un nodo interno presente nel database, cond usa RelType e i risultati superano
            // la dimensione della pagina
            Arguments.of(
                "/kanto",
                    true,
                    Set.of("/kanto/league/gyms"),
                    SearchCondBuilder.relationshipType("ownership"),
                    PageRequest.of(0, 5),
                    AnyTypeKind.USER,
                    List.of("briana", "brock", "diana", "eddie", "edwin", "gregory", "horton",
                        "jerry", "joy", "lt_surge", "misty", "parker", "vincent"),
                    false,
                    13,
                    false
            ),
            // [20] base è un nodo foglia presente nel database, cond usa AuxCond e ci si aspetta almeno una
            // corrispondenza. I risultati devono essere ordinati e il numero dei risultati supera la dimensione
            // della pagina.
            Arguments.of(
                "/kanto/league/gyms/cerulean",
                    true,
                    Set.of("/kanto/league"),
                    SearchCondBuilder.auxClass("GroupUselessInformation"),
                    PageRequest.of(0, 1, Sort.by(Sort.Order.desc("creationDate"))),
                    AnyTypeKind.GROUP,
                    List.of("Swimmers"),
                    true,
                    2,
                    false
            ),
            // Arguments.of()
            // [21] base è un nodo interno presente nel db, cond è un'espressione composita e ci si aspetta almeno una
            // corrispondenza, la pagina deve essere riempita parzialmente.
            Arguments.of(
                "/kanto",
                    true,
                    Set.of("/kanto/league"),
                    SearchCondBuilder.and(
                            SearchCondBuilder.attrEq("aType", "Ground"),
                            SearchCondBuilder.relationship("5ab33edf-6e59-5ed3-a8b3-17fa18d0752b")
                    ),
                    PageRequest.of(0, 9, Sort.by(Sort.Order.by("name"))),
                    AnyTypeKind.ANY_OBJECT,
                    List.of("brocks_graveler", "brocks_rhyhorn", "brunos_onix_1", "brunos_onix_2", "edwins_golem",
                        "greens_rhydon", "jerrys_rhydon", "mistys_quagsire"),
                    true,
                    8,
                    false
            ),
            // [22] base è un nodo foglia presente nel db, cond è ResourceCond e ci si aspetta almeno una corrispondenza
            // La prima pagina si riempie parzialmente e gli elementi vengono ordinati.
            Arguments.of(
                "/kanto/league/gyms/cerulean",
                    true,
                    Set.of("/kanto/league/gyms"),
                    SearchCondBuilder.resource("local-cerulean"),
                    PageRequest.of(0, 4, Sort.by(Sort.Order.asc("name"))),
                    AnyTypeKind.GROUP,
                    List.of("Sailors", "Swimmers", "Women"),
                    true,
                    3,
                    false
            ),
            // [23] base è un nodo interno presente nel db. cond è RelationshipCond ma non ci si aspetta alcuna
            // corrispondenza. DA FARE: questo deve far parte di incompatibleInputs
            Arguments.of(
                "/kanto/league/gyms",
                    true,
                    Set.of("/"),
                    SearchCondBuilder.relationship("8b0901ab-c66d-ceb3-4494-5e091d3ca5a4"),
                    PageRequest.of(1, 20),
                    AnyTypeKind.GROUP,
                    List.of(),
                    false,
                    0,
                    SKIP_TEST
            ),
            // [23]
            Arguments.of(
                "/kanto/league",
                    true,
                    Set.of("/kanto/league/gyms"),
                    SearchCondBuilder.resource("local-cerulean"),
                    PageRequest.of(0, 4, Sort.by(Sort.Order.asc("name"))),
                    AnyTypeKind.GROUP,
                    List.of("Sailors", "Swimmers", "Women"),
                    true,
                    3,
                    false
            ),
            // [24] base è un nodo interno presente nel db, si usa ResourceCond su un insieme di ANY_OBJECTs.
            // Ci si aspetta almeno una corrispondenza su un insieme ordinato.
            Arguments.of(
                "/kanto/league/gyms",
                    false,
                    Set.of("/"),
                    SearchCondBuilder.resource("local-pewter"),
                    Pageable.unpaged(Sort.by(Sort.Order.by("name"))),
                    AnyTypeKind.ANY_OBJECT,
                    List.of("brocks_graveler", "brocks_kabutops", "brocks_omastar", "brocks_onix", "brocks_rhyhorn",
                        "edwins_golem", "jerrys_rhydon"),
                    true,
                    7,
                    false
            ),
            // [25] base è un nodo foglia presente nel db, si usa ResourceCond ma la risorsa non esiste.
            Arguments.of(
                "/kanto/league/gyms/cerulean",
                    true,
                    Set.of("/"),
                    SearchCondBuilder.resource("local-pewte"),
                    PageRequest.of(1, 4),
                    AnyTypeKind.ANY_OBJECT,
                    List.of(),
                    false,
                    0,
                    false
            ),
            // [26] base è un nodo interno presente nel db, cond è RoleCond e ci si aspetta almeno una corrispondenza,
            Arguments.of(
                "/",
                    true,
                    Set.of("/"),
                    SearchCondBuilder.role("Elite4"),
                    Pageable.unpaged(),
                    AnyTypeKind.USER,
                    List.of("lorelei", "bruno", "agatha", "lance"),
                    false,
                    4,
                    false
            ),
            // [27] base è un nodo interno presente nel db, cond è AttrCond
            // e ci si aspetta almeno una corrispondenza.
            Arguments.of(
                "/",
                    true,
                    Set.of("/"),
                    SearchCondBuilder.attr("email", AttrCond.Type.ILIKE, "%ceruleangym.org"),
                    Pageable.unpaged(),
                    AnyTypeKind.GROUP,
                    List.of("Swimmers", "Sailors", "Women"),
                    false,
                    3,
                    false
            ),
            // [28] base è un nodo interno presente nel db, cond usa un AuxCond e non ci si aspettano corrispondenze
            // (non ci sono oggetti che soddisfano questa condizione).
            Arguments.of(
            "/kanto/league/gyms",
                    true,
                    Set.of("/kanto"),
                    SearchCondBuilder.and(
                            SearchCondBuilder.auxClass("SpeciesMetadata"),
                            SearchCondBuilder.attr("dexNo", AttrCond.Type.GE, "310")
                    ),
                    PageRequest.of(2, 10),
                    AnyTypeKind.ANY_OBJECT,
                    List.of(),
                    false,
                    0,
                    false
            ),
            // [29] base è un nodo interno presente nel db, cond usa AttrCond e non ci si aspettano corrispondenze
            Arguments.of(
                    "/kanto/league",
                    true,
                    Set.of("/kanto/league"),
                    SearchCondBuilder.not(
                            SearchCondBuilder.attr("email", AttrCond.Type.ILIKE, "%ceruleangym.org")),
                    PageRequest.of(0, 10, Sort.by(Sort.Order.by("name"))),
                    AnyTypeKind.GROUP,
                    List.of(),
                    false,
                    0,
                    false
            ),
            // [30] base è un nodo foglia presente nel db, cond è RelationshipTypeCond e ci si aspetta almeno
            // una corrispondenza. I risultati non sono impaginati.
            Arguments.of(
            "/kanto/league/gyms/pewter",
                    true,
                    Set.of("/"),
                    SearchCondBuilder.relationshipType("knowledge"),
                    Pageable.unpaged(Sort.by(Sort.Order.by("atk"), Sort.Order.by("name"))),
                    AnyTypeKind.ANY_OBJECT,
                    List.of("jerrys_rhydon", "edwins_golem", "brocks_kabutops", "brocks_graveler", "brocks_rhyhorn",
                        "brocks_omastar", "brocks_onix"),
                    false,
                    7,
                    false
            ),
            // [32] base è un nodo foglia presente nel db. Si usa una condizione composta per recuperare gruppi.
            Arguments.of(
                "/kanto/league/gyms/cerulean",
                    true,
                    Set.of("/kanto/league/gyms/cerulean"),
                    SearchCondBuilder.and(
                        SearchCondBuilder.attr("email", AttrCond.Type.ILIKE, "%.org"),
                        SearchCondBuilder.or(
                            SearchCondBuilder.any("name", AttrCond.Type.LIKE, "S%"),
                            SearchCondBuilder.any("creationDate", AttrCond.Type.LT, "2000-01-01")
                        )
                    ),
                    Pageable.unpaged(),
                    AnyTypeKind.GROUP,
                    List.of("Sailors", "Swimmers"),
                    false,
                    2,
                    false
            ),
            // [36] base è un nodo interno presente nel db. Si usa una condizione composta e ci si aspetta almeno
            // una corrispondenza.
            Arguments.of(
                "/",
                    true,
                    Set.of("/"),
                    SearchCondBuilder.membership("887bf7d9-9854-46cd-9e70-430c22b2a306"),
                    Pageable.unpaged(Sort.by(Sort.Direction.DESC, "name", "spe")),
                    AnyTypeKind.ANY_OBJECT,
                    List.of("mistys_starmie", "loreleis_cloyster", "joys_cloyster", "brocks_omastar",
                        "brocks_kabutops"),
                    true,
                    5,
                    false
            ),
            // [39] base è un nodo foglia presente nel db. recursive==false fallisce perché per qualche motivo il dao
            // considera tutti gli oggetti pokemon nel conteggio!
            Arguments.of(
                    "/kanto/league/gyms/vermilion",
                    false,
                    Set.of("/"),
                    SearchCondBuilder.anyType("POKEMON"),
                    Pageable.unpaged(Sort.by(Sort.Direction.ASC, "dexNo", "name")),
                    AnyTypeKind.ANY_OBJECT,
                    List.of("gregorys_pikachu", "surges_raichu", "vincents_magnemite", "surges_magneton",
                            "vincents_voltorb", "hortons_electrode_1", "hortons_electrode_2", "hortons_electrode_3",
                            "surges_electrode_1", "surges_electrode_2", "surges_electabuzz", "vincents_jolteon",
                            "gregorys_flaaffy", "gregorys_electrike"),
                    true,
                    14,
                    SKIP_TEST
            ),
            Arguments.of(
                "/kanto/league/gyms/vermilion",
                    true,
                    Set.of("/"),
                    SearchCondBuilder.anyType("POKEMON"),
                    Pageable.unpaged(Sort.by(Sort.Direction.ASC, "dexNo", "name")),
                    AnyTypeKind.ANY_OBJECT,
                    List.of("gregorys_pikachu", "surges_raichu", "vincents_magnemite", "surges_magneton",
                        "vincents_voltorb", "hortons_electrode_1", "hortons_electrode_2", "hortons_electrode_3",
                        "surges_electrode_1", "surges_electrode_2", "surges_electabuzz", "vincents_jolteon",
                        "gregorys_flaaffy", "gregorys_electrike"),
                    true,
                    14,
                    false
            ),
            // [43] base è un nodo foglia presente nel db. cond usa AuxCond cond e ci si aspetta almeno una
            // corrispondenza.
            Arguments.of(
                "/kanto/league/gyms/pewter",
                    true,
                    Set.of("/kanto/league/gyms/pewter"),
                    SearchCondBuilder.auxClass("TrainerMetadata"),
                    PageRequest.of(0, 4, Sort.by(Sort.Order.asc("username"))),
                    AnyTypeKind.USER,
                    List.of("brock", "edwin", "jerry"),
                    true,
                    3,
                    false
            ),
            // [44] base è un nodo foglia presente nel db. cond usa AnyType cond e non ci si aspettano
            // corrispondenze.
            Arguments.of(
                "/kanto/league/gyms/cerulean",
                    true,
                    Set.of("/kanto/league/gyms"),
                    SearchCondBuilder.membership("Water 1"),
                    PageRequest.of(1, 3, Sort.by(Sort.Order.desc("name"))),
                    AnyTypeKind.ANY_OBJECT,
                    List.of("mistys_lapras", "mistys_golduck", "eddies_azumarill"),
                    true,
                    7,
                    false
            ),
            // [46] base è un nodo foglia presente nel db. cond usa MemberCond ma non ci si aspettano corrispondenze.
            Arguments.of(
            "/kanto/league/gyms/cerulean",
                    true,
                    Set.of("/"),
                    SearchCondBuilder.member("dcb880fc-a5d7-446a-a444-095b85db0352"),
                    PageRequest.of(1, 1, Sort.by("name")),
                    AnyTypeKind.GROUP,
                    List.of("Women"),
                    true,
                    2,
                    false
            ),
            // [47] base è una foglia presente nel db, cond è una condizione composta e ci si aspetta almeno una
            // corrispondenza. Si salta almeno la prima pagina dei risultati.
            Arguments.of(
                    "/kanto/league/gyms/cerulean",
                    false,
                    Set.of("/kanto/league/gyms/cerulean"),
                    SearchCondBuilder.or(
                            SearchCondBuilder.and(
                                    SearchCondBuilder.not(
                                            SearchCondBuilder.attr("gender", AttrCond.Type.IEQ, "f")
                                    ),
                                    SearchCondBuilder.attr("title", AttrCond.Type.IEQ, "sailoR")
                            ),
                            SearchCondBuilder.and(
                                    SearchCondBuilder.not(
                                            SearchCondBuilder.role("GymLeader")
                                    ),
                                    SearchCondBuilder.attr("gender", AttrCond.Type.EQ, "F")
                            )
                    ),
                    PageRequest.of(0, 5, Sort.by("firstName")),
                    AnyTypeKind.USER,
                    List.of("briana", "diana", "eddie", "joy", "parker"),
                    true,
                    5,
                    false
            ),
            Arguments.of(
                    "/kanto/league/gyms/cerulean",
                    false,
                    Set.of("/kanto/league/gyms/cerulean"),
                    SearchCondBuilder.or(
                        SearchCondBuilder.and(
                            SearchCondBuilder.not(
                                SearchCondBuilder.attr("gender", AttrCond.Type.IEQ, "f")
                            ),
                            SearchCondBuilder.attr("title", AttrCond.Type.IEQ, "Sailo.")
                        ),
                        SearchCondBuilder.and(
                            SearchCondBuilder.not(
                                SearchCondBuilder.attr("gender", AttrCond.Type.IEQ, "m")
                            ),
                            SearchCondBuilder.not(SearchCondBuilder.role("GymLeader"))
                        )
                    ),
                    PageRequest.of(1, 2, Sort.by("username")),
                    AnyTypeKind.USER,
                    List.of("eddie", "joy"),
                    false,
                    5,
                    SKIP_TEST
            ),
           // [50] base è un nodo interno presente nel db. cond usa MembershipCond ma non ci si aspettano corrispondenze
           // perché non si hanno i permessi sufficienti.
            Arguments.of(
                    "/kanto/league",
                    true,
                    Set.of(),
                    SearchCondBuilder.membership("8df0e76b-ec23-43dc-a332-fbb19dd3e236"),
                    PageRequest.of(0, 5, Sort.by("name")),
                    AnyTypeKind.ANY_OBJECT,
                    List.of(),
                    false,
                    0,
                    false
            ),
            // [51] base è un nodo interno presente nel db. Si usa RelationshipTypeCond e ci si aspetta almeno
            // una corrispondenza. I risultati riempiono parzialmente la pagina e devono essere ordinati
            Arguments.of(
                    "/kanto/league",
                    false,
                    Set.of("/kanto/league"),
                    SearchCondBuilder.and(
                            SearchCondBuilder.relationshipType("knowledge"),
                            SearchCondBuilder.and(
                                    SearchCondBuilder.anyType("MOVE"),
                                    SearchCondBuilder.attr("power", AttrCond.Type.GE, "60")
                            )
                    ),
                    Pageable.unpaged(Sort.by(Sort.Order.asc("name"))),
                    AnyTypeKind.ANY_OBJECT,
                    List.of(),
                    true,
                    0,
                    false
            ),
            // [52] base è un nodo FOGLIA presente nel db. Si usa RoleCond e non ci si aspettano
            // corrispondenze.
            // ATT: Agg test tra quelli che devono fallire
            Arguments.of(
                    "/kanto/league/gyms/pewter",
                    false,
                    Set.of("/"),
                    SearchCondBuilder.role("Champion"),
                    Pageable.unpaged(Sort.by(Sort.Order.asc("name"))),
                    AnyTypeKind.ANY_OBJECT,
                    List.of(),
                    false,
                    0,
                    SKIP_TEST
            ),
            // [53] base è un nodo interno presente nel db. Tutti i reami in adminRealms sono validi e si usa
            // ResourceCond come predicato, ci si aspetta almeno un risultato e il numero di risultati eccede la
            // grandezza di pagina. I risultati devono essere ordinati.
            Arguments.of(
                    "/",
                    true,
                    Set.of("/kanto", "/"),
                    SearchCondBuilder.resource("move-db"),
                    PageRequest.of(0, 10, Sort.by(Sort.Order.asc("name"))),
                    AnyTypeKind.ANY_OBJECT,
                    List.of("Aerial Ace", "Agility", "Air Cutter", "Amnesia", "Ancient Power", "Aqua Jet", "Aqua Ring",
                        "Aqua Tail", "Attract", "Bite"),
                    true,
                    120,
                    false
            ),
            // [55] base è un nodo foglia presente nel db. Si usa AnyTypeCond e sebbene ci siano corrispondenze ci si
            // aspetta una pagina vuota.
            // recursive == false e adminRealms contiene un reame parente di base fa fallire il test perché il DAO
            // include gli oggetti nel suddetto parente invece di considerare esclusivamente gli oggetti della foglia
            Arguments.of(
                    "/kanto/league/gyms/cerulean",
                    false,
                    Set.of("/kanto/league/gyms"),
                    SearchCondBuilder.anyType("POKEMON"),
                    PageRequest.of(1, 11),
                    AnyTypeKind.ANY_OBJECT,
                    List.of(),
                    false,
                    11,
                    SKIP_TEST
            ),
            Arguments.of(
                    "/",
                    true,
                    Set.of("/"),
                    SearchCondBuilder.attr("trainerId", AttrCond.Type.LIKE, "00000%"),
                    PageRequest.of(0, 2, Sort.by(Sort.Order.asc("trainerId"))),
                    AnyTypeKind.USER,
                    List.of("green", "brock"),
                    false,
                    4,
                    false
            )
        );
    }

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

    static Stream<Arguments> incompatibleInputs() {
        return Stream.of(
                Arguments.of(
                        "/kanto/league/gyms/cerulean",
                        true,
                        Set.of("/kanto/league/gyms"),
                        SearchCondBuilder.membership("Water 1"),
                        PageRequest.of(1, 3, Sort.by(Sort.Order.desc("name"))),
                        AnyTypeKind.GROUP
                ),
                Arguments.of(
                        "/kanto/league/gyms/vermilion",
                        false,
                        Set.of("/kanto/league/gyms/vermilion"),
                        SearchCondBuilder.relationship("93829b43-922f-e15a-e1e3-db63ef7ddc76"),
                        Pageable.unpaged(),
                        AnyTypeKind.GROUP
                ),
                Arguments.of(
                        "/",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.member("f2a32efd-ff13-8c80-c398-c3320e500785"),
                        PageRequest.of(0, 1, Sort.by(Sort.Order.by("name"))),
                        AnyTypeKind.ANY_OBJECT,
                        List.of("Dragon")
                ),
                Arguments.of(
                        "/kanto/league/gyms/cerulean",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.anyType("POKEMON"),
                        Pageable.unpaged(),
                        AnyTypeKind.GROUP
                ),
                Arguments.of(
                        "/",
                        false,
                        Set.of("/"),
                        SearchCondBuilder.anyType("MOVE"),
                        Pageable.unpaged(),
                        AnyTypeKind.USER
                ),
                Arguments.of(
                        "/kanto/league/gyms/cerulean",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.member("7c462d53-2e40-43fb-935e-42989c676a5a"),
                        Pageable.unpaged(),
                        AnyTypeKind.USER
                ),
                Arguments.of(
                        "/kanto",
                        true,
                        Set.of("/kanto/league/gyms"),
                        SearchCondBuilder.relationshipType("ownership"),
                        PageRequest.of(0, 5),
                        AnyTypeKind.GROUP
                ),
                Arguments.of(
                        "/kanto/league/gyms/vermilion",
                        false,
                        Set.of("/kanto/league/gyms/vermilion"),
                        SearchCondBuilder.role("GymTrainer"),
                        Pageable.unpaged(),
                        AnyTypeKind.ANY_OBJECT
                )
        );
    }

    @ParameterizedTest
    @MethodSource("incompatibleInputs")
    public void testIncompatibleQueries(
            final String basePath,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final Pageable pageable,
            final AnyTypeKind kind
    ) {
        final Realm base = realmSearchDAO.findByFullPath(basePath).orElseThrow();
        AuthContextUtils.callAsAdmin(
                SyncopeConstants.MASTER_DOMAIN, () -> {
                    Assertions.assertThrows(Exception.class, () ->
                            searchDAO.search(base, recursive, adminRealms, cond, pageable, kind));
                    Assertions.assertThrows(Exception.class, () ->
                            searchDAO.count(base, recursive, adminRealms, cond, kind));
                    return null;
                });
    }

    // Ho preso casi di test selezionati in precedenza che producevano risultati,
    // l'idea è quella di usare come base il mock di un reame che ha un percorso differente
    // da quello usato nel caso di test originale.
    static Stream<Arguments> baseNotPresent() {
        return Stream.of(
                // [19] (13 oggetti soddisfano il criterio di ricerca)
                Arguments.of(
                        "/kant", "kant",
                        false,
                        Set.of("/kanto/league/gyms"),
                        SearchCondBuilder.relationshipType("ownership"),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        Optional.empty(),
                        false
                ),
                // [19] (13 oggetti soddisfano il criterio di ricerca)
                // Questo test non passa perché nonostante base non esiste il DAO produce
                // 18 risultati (in questo caso ignora anche adminRealms perché include utenti
                // del reame genitore /kanto/league).
                Arguments.of(
                        "/kant", "kant",
                        true,
                        Set.of("/kanto/league/gyms"),
                        SearchCondBuilder.relationshipType("ownership"),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        Optional.empty(),
                        SKIP_TEST
                ),
                // [16] (7 oggetti soddisfano il criterio di ricerca)
                Arguments.of(
                        "kanto/league/gyms/vermilion", "vermilion",
                        true,
                        Set.of("/kanto/league/gyms"),
                        SearchCondBuilder.attr("level", AttrCond.Type.GE, "44"),
                        PageRequest.of(3, 2, Sort.by(Sort.Order.by("name"))),
                        AnyTypeKind.ANY_OBJECT,
                        Optional.of(Exception.class),
                        false
                ),
                Arguments.of(
                        "kanto/league/gyms/vermilion", "vermilion",
                        false,
                        Set.of("/kanto/league/gyms"),
                        SearchCondBuilder.attr("level", AttrCond.Type.GE, "44"),
                        PageRequest.of(3, 2, Sort.by(Sort.Order.by("name"))),
                        AnyTypeKind.ANY_OBJECT,
                        Optional.of(Exception.class),
                        false
                ),
                // [26] (la query originale che parte da "/" produce 4 risultati)
                // La richiesta continua a produrre quattro risultati, ci si aspetta che il numero di
                // risultati prodotti sia zero.
                Arguments.of(
                        "", "",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.role("Elite4"),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        Optional.empty(),
                        SKIP_TEST
                ),
                Arguments.of(
                        "", "",
                        false,
                        Set.of("/"),
                        SearchCondBuilder.role("Elite4"),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        Optional.empty(),
                        false
                ),
                // [44] (7 oggetti soddisfano la query)
                // Il DAO produce in entrambi i casi quattro risultati quando dovrebbero essere zero.
                Arguments.of(
                        "/kanto/league/cerulean", "cerulean",
                        true,
                        Set.of("/kanto/league/gyms/pewter", "/kanto/league/gyms/cerulean"),
                        SearchCondBuilder.membership("8ab844e4-dca0-46ac-93de-3c12ddfd6aff"),
                        PageRequest.of(1, 4),
                        AnyTypeKind.ANY_OBJECT,
                        Optional.empty(),
                        SKIP_TEST
                ),
                Arguments.of(
                        "/kanto/league/cerulean", "cerulean",
                        false,
                        Set.of("/kanto/league/gyms/pewter", "/kanto/league/gyms/cerulean"),
                        SearchCondBuilder.membership("8ab844e4-dca0-46ac-93de-3c12ddfd6aff"),
                        PageRequest.of(1, 4),
                        AnyTypeKind.ANY_OBJECT,
                        Optional.empty(),
                        SKIP_TEST
                ),
                // [23] (3 oggetti soddisfano la query di ricerca)
                // Il DAO produce tre risultati in entrambi i casi, dovrebbero essere zero.
                Arguments.of(
                        "/kanto/league//cerulean", "/cerulean",
                        false,
                        Set.of("/kanto/league/gyms/cerulean"),
                        SearchCondBuilder.resource("local-cerulean"),
                        PageRequest.of(0, 4, Sort.by(Sort.Order.asc("name"))),
                        AnyTypeKind.GROUP,
                        Optional.empty(),
                        SKIP_TEST
                ),
                Arguments.of(
                        "/kanto/league//cerulean", "/cerulean",
                        true,
                        Set.of("/kanto/league/gyms/cerulean"),
                        SearchCondBuilder.resource("local-cerulean"),
                        PageRequest.of(0, 4, Sort.by(Sort.Order.asc("name"))),
                        AnyTypeKind.GROUP,
                        Optional.empty(),
                        SKIP_TEST
                ),
                // [1] (5 oggetti soddisfano la query di ricerca)
                Arguments.of(
                        "/kanto/", "/",
                        false,
                        Set.of("/kanto/league"),
                        SearchCondBuilder.auxClass("TrainerMetadata"),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        Optional.empty(),
                        false
                ),
                // [39] (14 oggetti soddisfano la query)
                // Qui il DAO produce 58 risultati, la query originale col percorso corretto
                // ne produce 14 perché restringe i permessi al reame di vermilion. In ogni caso
                // in questi due casi di test ci si aspettano zero risultati.
                Arguments.of(
                        "/kanto/league/gyms/vemilion", "vemilion",
                        false,
                        Set.of("/kanto/league/gyms/vermilion"),
                        SearchCondBuilder.anyType("POKEMON"),
                        Pageable.unpaged(Sort.by(Sort.Direction.ASC, "dexNo", "name")),
                        AnyTypeKind.ANY_OBJECT,
                        Optional.empty(),
                        SKIP_TEST
                ),
                Arguments.of(
                        "/kanto/league/gyms/vemilion", "vemilion",
                        true,
                        Set.of("/kanto/league/gyms/vermilion"),
                        SearchCondBuilder.anyType("POKEMON"),
                        Pageable.unpaged(Sort.by(Sort.Direction.ASC, "dexNo", "name")),
                        AnyTypeKind.ANY_OBJECT,
                        Optional.empty(),
                        SKIP_TEST
                ),
                // [46] (2 oggetti soddisfano la query)
                // Entrambi i casi di test falliscono perché le richieste producono un risultato.
                Arguments.of(
                        "/kanto/league/gyms/cerulea", "cerulea",
                        false,
                        Set.of("/"),
                        SearchCondBuilder.member("dcb880fc-a5d7-446a-a444-095b85db0352"),
                        PageRequest.of(1, 1, Sort.by("name")),
                        AnyTypeKind.GROUP,
                        Optional.empty(),
                        SKIP_TEST
                ),
                Arguments.of(
                        "/kanto/league/gyms/cerulea", "cerulea",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.member("dcb880fc-a5d7-446a-a444-095b85db0352"),
                        PageRequest.of(1, 1, Sort.by("name")),
                        AnyTypeKind.GROUP,
                        Optional.empty(),
                        SKIP_TEST
                ),
                // [3] (1 oggetto)
                // Entrambi i test falliscono perché la richiesta produce un risultato.
                Arguments.of(
                        "/kanto/league/gyms/vermilio", "vermilio",
                        false,
                        Set.of("/kanto/league/gyms/cerulean", "/kanto/league/gyms"),
                        SearchCondBuilder.relationship("5780be17-12fe-cc2b-1acf-42c5f3a02120"),
                        PageRequest.of(0, 2, Sort.by(Sort.Order.by("trainerId"))),
                        AnyTypeKind.USER,
                        Optional.empty(),
                        SKIP_TEST
                ),
                Arguments.of(
                        "/kanto/league/gyms/vermilio", "vermilio",
                        true,
                        Set.of("/kanto/league/gyms/cerulean", "/kanto/league/gyms"),
                        SearchCondBuilder.relationship("5780be17-12fe-cc2b-1acf-42c5f3a02120"),
                        PageRequest.of(0, 2, Sort.by(Sort.Order.by("trainerId"))),
                        AnyTypeKind.USER,
                        Optional.empty(),
                        SKIP_TEST
                ),
                // [3] (1 oggetto)
                Arguments.of(
                        null, null,
                        false,
                        Set.of("/kanto/league/gyms/cerulean", "/kanto/league/gyms"),
                        SearchCondBuilder.relationship("5780be17-12fe-cc2b-1acf-42c5f3a02120"),
                        PageRequest.of(0, 2, Sort.by(Sort.Order.by("trainerId"))),
                        AnyTypeKind.USER,
                        Optional.of(NullPointerException.class),
                        false
                )
        );
    }

    @ParameterizedTest
    @MethodSource("baseNotPresent")
    public void testBaseNotPresent(
            final String basePath,
            final String name,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final Pageable pageable,
            final AnyTypeKind kind,
            final Optional<Class<Exception>> exception,
            final boolean skip
    ) {
        Assumptions.assumeFalse(
                skip,
                "La richiesta produce risultati nonostante il reame %s non esiste".formatted(basePath)
        );
        final Realm base = mock(Realm.class);
        when(base.getFullPath()).thenReturn(basePath);
        when(base.getName()).thenReturn(name);
        when(base.getKey()).thenReturn("a key that doesn't exist");
        AuthContextUtils.callAsAdmin(
            SyncopeConstants.MASTER_DOMAIN, () -> {
                if (exception.isPresent()) {
                    Assertions.assertThrows(exception.get(), () ->
                            searchDAO.search(base, recursive, adminRealms, cond, pageable, kind));
                    Assertions.assertThrows(exception.get(), () ->
                            searchDAO.count(base, recursive, adminRealms, cond, kind));
                } else {
                    Assertions.assertIterableEquals(
                            List.of(),
                            searchDAO.search(base, recursive, adminRealms, cond, pageable, kind));
                    Assertions.assertEquals(
                            0,
                            searchDAO.count(base, recursive, adminRealms, cond, kind));
                }
                return null;
            });
    }

    static Stream<Arguments> nonExistentAdminRealms() {
        return Stream.of(
                // [1] base è un nodo interno presente nel database. Il predicato usa AuxClassCond e ci si aspetta
                // almeno una corrispondenza. Non ci si aspetta un'impaginazione dei risultati. Ci si aspetta
                // un'eccezione perché il reame "/kanto/leagu" non esiste e recursive == true
                Arguments.of(
                        "/kanto/league",
                        true,
                        Set.of("/kanto/leagu"),
                        SearchCondBuilder.auxClass("TrainerMetadata"),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        Optional.of(Exception.class),
                        false
                ),
                // [2] base è un nodo foglia presente nel database. Il predicato usa MembershipCond
                // e ci si aspetta almeno una corrispondenza. Si salta la prima pagine dei risultati e la successiva non
                // è riempita completamente. I risultati non vengono ordinati. Le due stringhe vengono scartate e il
                // chiamante non ha permessi validi per cercare gli oggetti. Il DAO ignora il fatto che adminRealms
                // non contiene reami validi su cui effettuare la ricerca.
                Arguments.of(
                        "/kanto/league/gyms/cerulean",
                        false,
                        Set.of("1", "2"),
                        SearchCondBuilder.membership("8ab844e4-dca0-46ac-93de-3c12ddfd6aff"),
                        PageRequest.of(1, 4),
                        AnyTypeKind.ANY_OBJECT,
                        Optional.empty(),
                        SKIP_TEST
                ),
                Arguments.of(
                        "/kanto/league/gyms/cerulean",
                        true,
                        Set.of("1", "2"),
                        SearchCondBuilder.membership("8ab844e4-dca0-46ac-93de-3c12ddfd6aff"),
                        PageRequest.of(1, 4),
                        AnyTypeKind.ANY_OBJECT,
                        Optional.empty(),
                        SKIP_TEST
                )
        );
    }

    @ParameterizedTest
    @MethodSource("nonExistentAdminRealms")
    public void testNonExistentAdminRealms(
            final String basePath,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final Pageable pageable,
            final AnyTypeKind kind,
            final Optional<Class<Exception>> exception,
            final boolean skip
    ) {
        Assumptions.assumeFalse(
                skip,
                "Il test fallisce perché il DAO ignora la mancanza di permessi"
        );
        final Realm base = realmSearchDAO.findByFullPath(basePath).orElseThrow();
        AuthContextUtils.callAs(
                SyncopeConstants.MASTER_DOMAIN, "green", List.of(),
                () -> {
                    if (exception.isPresent()) {
                        Assertions.assertThrows(Exception.class, () ->
                                searchDAO.search(base, recursive, adminRealms, cond, pageable, kind));
                        Assertions.assertThrows(Exception.class, () ->
                                searchDAO.count(base, recursive, adminRealms, cond, kind));
                    } else {
                        Assertions.assertIterableEquals(
                                List.of(),
                                searchDAO.search(base, recursive, adminRealms, cond, pageable, kind));
                        Assertions.assertEquals(
                                0,
                                searchDAO.count(base, recursive, adminRealms, cond, kind));
                    }
                    return null;
                });
    }

    // In generale tutti i test falliscono ma non dovrebbero
    static Stream<Arguments> disjointRealms() {
        return Stream.of(
                // [2] base è un nodo foglia presente nel database. Il predicato usa MembershipCond
                // e ci si aspetta almeno una corrispondenza. Si salta la prima pagine dei risultati e la successiva non
                // è riempita completamente. I risultati non vengono ordinati. Il test fallisce perché il DAO
                // ritorna tutti gli oggetti di tipo ANY_OBJECT che sono membri del gruppo "Water 1" invece di
                // non ritornare nulla.
                Arguments.of(
                        "/kanto/league/gyms/cerulean",
                        true,
                        Set.of("/kanto/league/gyms/vermilion"),
                        SearchCondBuilder.membership("8ab844e4-dca0-46ac-93de-3c12ddfd6aff"),
                        Pageable.unpaged(),
                        AnyTypeKind.ANY_OBJECT,
                        SKIP_TEST
                ),
                // Il test fallisce perché il DAO ritorna l'utente Horton nel reame vermilion ma
                // non dovrebbe perché in adminRealms è presente un reame "fratello"
                Arguments.of(
                        "/kanto/league/gyms/vermilion",
                        false,
                        Set.of("/kanto/league/gyms/cerulean"),
                        SearchCondBuilder.relationship("e1035e1c-e3bb-22ae-6552-ac5309eda6f5"),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        SKIP_TEST
                ),
                // [10] Il test fallisce perché il DAO ritorna il gruppo "Sailors" definito
                // nel reame cerulean ma non dovrebbe perché in adminRealms è presente un reame "fratello"
                Arguments.of(
                        "/kanto/league/gyms/cerulean",
                        false,
                        Set.of("/kanto/league/gyms/pewter"),
                        SearchCondBuilder.member("7c462d53-2e40-43fb-935e-42989c676a5a"),
                        Pageable.unpaged(),
                        AnyTypeKind.GROUP,
                        SKIP_TEST
                ),
                Arguments.of(
                        "/kanto/league/gyms/vermilion",
                        false,
                        Set.of("/kanto/league/gyms/cerulean"),
                        SearchCondBuilder.attr("title", AttrCond.Type.EQ, "Juggler"),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        SKIP_TEST
                ),
                Arguments.of(
                        "/kanto/league/gyms/vermilion",
                        true,
                        Set.of("/kanto/league/gyms/cerulean"),
                        SearchCondBuilder.attr("title", AttrCond.Type.EQ, "Juggler"),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        SKIP_TEST
                ),
                Arguments.of(
                        "/kanto/league/gyms/vermilion",
                        true,
                        Set.of("/kanto/league/gyms/pewter"),
                        SearchCondBuilder.attr("aType", AttrCond.Type.EQ, "Electric"),
                        Pageable.unpaged(),
                        AnyTypeKind.ANY_OBJECT,
                        SKIP_TEST
                )
        );
    }

    @ParameterizedTest
    @MethodSource("disjointRealms")
    public void testDisjointRealms(
            final String basePath,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final Pageable pageable,
            final AnyTypeKind kind,
            final boolean skip
    ) {
        Assumptions.assumeFalse(skip);
        final Realm base = realmSearchDAO.findByFullPath(basePath).orElseThrow();
        AuthContextUtils.callAs(
                SyncopeConstants.MASTER_DOMAIN, "green", List.of(),
                () -> {
                    List<String> result = searchDAO.search(base, recursive, adminRealms, cond, pageable, kind)
                            .stream()
                            .map(x -> {
                                switch (kind) {
                                    case USER:
                                        return ((User) x).getUsername();
                                    case GROUP:
                                        return ((Group) x).getName();
                                    case ANY_OBJECT:
                                        return ((AnyObject) x).getName();
                                    default:
                                        throw new IllegalStateException("Unexpected value: " + kind);
                                }
                            })
                            .toList();

                    Assertions.assertIterableEquals(
                            List.of(),
                            result);
                    Assertions.assertEquals(
                            0,
                            searchDAO.count(base, recursive, adminRealms, cond, kind));
                    return null;
                });
    }

    private static Stream<Arguments> illegalInputs() {
        return Stream.of(
                // [36]
                Arguments.of(
                        "/",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.membership("887bf7d9-9854-46cd-9e70-430c22b2a306"),
                        null,
                        AnyTypeKind.ANY_OBJECT,
                        Optional.of(NullPointerException.class),
                        false
                ),
                // [10]
                Arguments.of(
                        null,
                        true,
                        Set.of("/"),
                        SearchCondBuilder.member("7c462d53-2e40-43fb-935e-42989c676a5a"),
                        Pageable.unpaged(),
                        AnyTypeKind.GROUP,
                        Optional.of(NullPointerException.class),
                        false
                ),
                // [4]
                Arguments.of(
                        "/",
                        true,
                        null,
                        SearchCondBuilder.anyType("MOVE"),
                        PageRequest.of(0, 10),
                        AnyTypeKind.ANY_OBJECT,
                        Optional.empty(),
                        false
                ),
                // [4]
                Arguments.of(
                        "/",
                        true,
                        setOfNull(),
                        SearchCondBuilder.anyType("MOVE"),
                        PageRequest.of(0, 10),
                        AnyTypeKind.ANY_OBJECT,
                        Optional.of(NullPointerException.class),
                        false
                ),
                // [26]
                Arguments.of(
                        "/",
                        true,
                        Set.of("/"),
                        null,
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        Optional.empty(),
                        false
                ),
                // [1]
                Arguments.of(
                        "/kanto/league",
                        false,
                        Set.of("/kanto/league"),
                        SearchCondBuilder.and(
                                SearchCondBuilder.auxClass("TrainerMetadata"),
                                SearchCondBuilder.any("username", AttrCond.Type.ILIKE, "%E%")
                        ),
                        Pageable.unpaged(),
                        null,
                        Optional.of(NullPointerException.class),
                        false
                )
        );
    }

    private static Set<String> setOfNull() {
        final var set = new HashSet<String>();
        set.add(null);
        return set;
    }

    @ParameterizedTest
    @MethodSource("illegalInputs")
    public void testIllegalInputs(
            final String basePath,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final Pageable pageable,
            final AnyTypeKind kind,
            final Optional<Class<Exception>> exception,
            final boolean skip
    ) {
        Assumptions.assumeFalse(skip);
        Realm base = null;
        if (basePath != null) {
            base = realmSearchDAO.findByFullPath(basePath).orElseThrow();
        }
        final var realm = base;
        AuthContextUtils.callAs(
                SyncopeConstants.MASTER_DOMAIN, "green", List.of(),
                () -> {
                    if (exception.isPresent()) {
                        Assertions.assertThrows(
                                exception.get(),
                                () -> searchDAO.search(realm, recursive, adminRealms, cond, pageable, kind)
                        );
                        if (pageable != null) {
                            Assertions.assertThrows(
                                    exception.get(),
                                    () -> searchDAO.count(realm, recursive, adminRealms, cond, kind)
                            );
                        }
                    } else {
                        Assertions.assertIterableEquals(
                                List.of(),
                                searchDAO.search(realm, recursive, adminRealms, cond, pageable, kind)
                        );
                        Assertions.assertEquals(
                                0,
                                searchDAO.count(realm, recursive, adminRealms, cond, kind)
                        );
                    }

                    return null;
                });
    }

    private static Stream<Arguments> illegalConditions() {
        return Stream.of(
                Arguments.of(
                        "/kanto/league",
                        false,
                        Set.of("/kanto/league"),
                        SearchCondBuilder.attr("username", AttrCond.Type.EQ, null),
                        Pageable.unpaged(),
                        AnyTypeKind.USER
                ),
                Arguments.of(
                        "/",
                        true,
                        Set.of("/"),
                        SearchCondBuilder.or(
                                SearchCondBuilder.anyType("MOVE"), // Ramo valido
                                SearchCondBuilder.membership(null) // Ramo invalido che corrompe l'intera espressione
                        ),
                        PageRequest.of(0, 119),
                        AnyTypeKind.ANY_OBJECT
                ),
                Arguments.of(
                        "/kanto/league/gyms",
                        true,
                        Set.of("/kanto"),
                        SearchCondBuilder.and(
                                null,
                                SearchCondBuilder.attr("dexNo", AttrCond.Type.GE, "310")
                        ),
                        Pageable.unpaged(),
                        AnyTypeKind.ANY_OBJECT
                ),
                Arguments.of(
                        "/",
                        true,
                        Set.of("/"),
                        SearchCond.of(null),
                        Pageable.unpaged(),
                        AnyTypeKind.USER,
                        Optional.empty(),
                        false
                ),
                Arguments.of(
                        "/",
                        true,
                        Set.of("/"),
                        SearchCond.of(new AnyCond(null)),
                        Pageable.unpaged(),
                        AnyTypeKind.ANY_OBJECT,
                        Optional.empty(),
                        false
                )
        );
    }

    @ParameterizedTest
    @MethodSource("illegalConditions")
    public void testIllegalConditions(
            final String basePath,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final Pageable pageable,
            final AnyTypeKind kind
    ) {
        Realm base = null;
        if (basePath != null) {
            base = realmSearchDAO.findByFullPath(basePath).orElseThrow();
        }
        final var realm = base;
        AuthContextUtils.callAs(
                SyncopeConstants.MASTER_DOMAIN, "green", List.of(),
                () -> {
                    Assertions.assertIterableEquals(
                            List.of(),
                            searchDAO.search(realm, recursive, adminRealms, cond, pageable, kind)
                    );
                    Assertions.assertEquals(
                            0,
                            searchDAO.count(realm, recursive, adminRealms, cond, kind)
                    );

                    return null;
                });
    }

    private void subsetOf(final List<String> superset, final Iterable<String> subset) {
        for (String element : subset) {
            Assertions.assertTrue(superset.contains(element));
        }
    }
}
