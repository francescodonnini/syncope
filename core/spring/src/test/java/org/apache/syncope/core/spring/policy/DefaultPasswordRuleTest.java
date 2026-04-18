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
package org.apache.syncope.core.spring.policy;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DefaultPasswordRuleTest {
    private static Stream<Arguments> alphaRuleInputs() {
        return Stream.of(
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .maxLen(Integer.MAX_VALUE)
                                .usernameAllowed(true)
                                .alpha(-1)
                                .build(),
                        null, "", Optional.empty()),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .maxLen(Integer.MAX_VALUE)
                                .usernameAllowed(true)
                                .alpha(0).build(),
                        "86*D", null, Optional.empty()),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .maxLen(Integer.MAX_VALUE)
                                .usernameAllowed(true)
                                .alpha(1)
                                .build(),
                        "u", null, Optional.empty()),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .maxLen(Integer.MAX_VALUE)
                                .usernameAllowed(true)
                                .alpha(1)
                                .build(),
                        "x", "1", Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .maxLen(Integer.MAX_VALUE)
                                .usernameAllowed(true)
                                .alpha(1)
                                .build(),
                        "admin", "a7", Optional.empty()),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .maxLen(Integer.MAX_VALUE)
                                .usernameAllowed(true)
                                .alpha(1)
                                .build(),
                        "#@[1", "7^?B", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .alpha(1)
                        .build(),
                        "admin", "+bCD34^", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .alpha(2)
                        .build(),
                        "mrossi", "213123a21", Optional.of(PasswordPolicyException.class)),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .alpha(Integer.MAX_VALUE)
                        .build(),
                        "User", "(CCi*]JSB)SI%M;", Optional.of(PasswordPolicyException.class)));
    }

    @ParameterizedTest
    @MethodSource("alphaRuleInputs")
    public void testAlphaRule(
            final DefaultPasswordRuleConf conf,
            final String username,
            final String password,
            final Optional<Class<Exception>> exception
    ) {
        testRule(conf, username, password, exception);
    }

    private static Stream<Arguments> digitRuleInputs() {
        return Stream.of(
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .digit(-1
                        ).build()
                        , null, "", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .digit(Integer.MIN_VALUE).build(), "", null, Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .digit(1)
                        .build(),
                        ":dU#AA", "'ZM6SQ", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .digit(1)
                        .build(),
                        "nhnujj", "9", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .digit(1)
                        .build(),
                        "adm1n", "password", Optional.of(PasswordPolicyException.class)),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .digit(1)
                        .build(),
                        "z+5c0", "OU3", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .digit(2)
                        .build(),
                        "admin", "t{JD6}{Y", Optional.of(PasswordPolicyException.class)),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .digit(2)
                        .build(),
                        "p", "4hbcf5vbh", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .digit(2)
                        .build(),
                        "user", "-6pg07{o", Optional.empty()),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                            .maxLen(Integer.MAX_VALUE)
                            .usernameAllowed(true)
                            .digit(Integer.MAX_VALUE).build(),
                        "admin", "1234567890", Optional.of(PasswordPolicyException.class)));
    }

    @ParameterizedTest
    @MethodSource("digitRuleInputs")
    public void testDigitRule(
            final DefaultPasswordRuleConf conf,
            final String username,
            final String password,
            final Optional<Class<Exception>> exception
    ) {
        testRule(conf, username, password, exception);
    }

    private static Stream<Arguments> uppercaseRuleInputs() {
        return Stream.of(
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .upper(-1
                        ).build(),
                        null, "", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .upper(0)
                        .build(),
                        "u", null, Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .upper(1)
                        .build(),
                        ")+M4[$", "\"d=$h_", Optional.of(PasswordPolicyException.class)),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .upper(0)
                        .build(),
                        "~0{H4G", "a,so-%", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .upper(1)
                        .build(),
                        "Ete}\\\\6i$", "?$crsfIqd", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .upper(2)
                        .build(),
                        "[4(O6g{", "?]s-H%", Optional.of(PasswordPolicyException.class)),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .upper(2)
                        .build(),
                        "user", "=]G5#E0,(U`!", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .upper(1)
                        .build(),
                        "admin", "~@#0{h4G", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .upper(Integer.MAX_VALUE).build(),
                        "admin", "AAAAAAAH", Optional.of(PasswordPolicyException.class)));
    }

    @ParameterizedTest
    @MethodSource("uppercaseRuleInputs")
    public void testUppercaseRule(
            final DefaultPasswordRuleConf conf,
            final String username,
            final String password,
            final Optional<Class<Exception>> exception
    ) {
        testRule(conf, username, password, exception);
    }

    private static Stream<Arguments> lowercaseRuleInputs() {
        return Stream.of(
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .lower(-1).build(),
                        null, "", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .lower(0).build(),
                        "user", null, Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .lower(1).build(),
                        "m#]+37[$", "\"%=Y$_", Optional.of(PasswordPolicyException.class)),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .lower(0).build(),
                        "~0{tw", "\"L.SI-%\"", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .lower(1).build(),
                        "£$&FGA%s", "$KLuA83", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .lower(2).build(),
                        "br532fd", "30s-Tù", Optional.of(PasswordPolicyException.class)),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .lower(2).build(),
                        "admin", "=]G5eE0a(U`l", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .lower(1).build(),
                        "34fd2d", "~_#0_{h4G", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .lower(Integer.MAX_VALUE).build(),
                        "verylongusername", "verylongpassword", Optional.of(PasswordPolicyException.class)));
    }

    @ParameterizedTest
    @MethodSource("lowercaseRuleInputs")
    public void testLowercaseRule(
            final DefaultPasswordRuleConf conf,
            final String username,
            final String password,
            final Optional<Class<Exception>> exception
    ) {
        testRule(conf, username, password, exception);
    }

    private static Stream<Arguments> minLenRuleInputs() {
        return Stream.of(
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .minLen(-1)
                        .build(),
                        null, "a", Optional.empty()),
                // I was expecting an Exception to be thrown, but it didn't
                // Arguments.of(
                //    DefaultRuleConfBuilder.builder()
                //        .maxLen(Integer.MAX_VALUE)
                //        .usernameAllowed(true)
                //        .minLen(0).build(), "", null, Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .minLen(1)
                        .build(),
                        "u", "", Optional.of(PasswordPolicyException.class)),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .minLen(1)
                        .build(),
                        "x", "1", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .minLen(2)
                        .build(),
                        "xyz", "*", Optional.of(PasswordPolicyException.class)),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .minLen(2)
                        .build(),
                        "u", "@a3", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .minLen(Integer.MAX_VALUE)
                        .build(),
                        "admin", "1234", Optional.of(PasswordPolicyException.class)),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .minLen(1)
                        .build(), "", "%&", Optional.empty())
        );
    }

    @ParameterizedTest
    @MethodSource("minLenRuleInputs")
    public void testMinLenRule(
            final DefaultPasswordRuleConf conf,
            final String username,
            final String password,
            final Optional<Class<Exception>> exception
    ) {
        testRule(conf, username, password, exception);
    }

    private static Stream<Arguments> maxLenRuleInputs() {
        return Stream.of(
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .usernameAllowed(true)
                        .maxLen(0)
                        .build(),
                        null, "a", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .usernameAllowed(true)
                        .maxLen(-1
                        ).build()
                        , "2", null, Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .usernameAllowed(true)
                        .maxLen(1)
                        .build(),
                        "", "*", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .usernameAllowed(true)
                        .maxLen(1)
                        .build(),
                        "u", ",^", Optional.of(PasswordPolicyException.class)),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .usernameAllowed(true)
                        .maxLen(2)
                        .build(),
                        "usr", "12", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .usernameAllowed(true)
                        .maxLen(Integer.MAX_VALUE).build(), "admin", "#".repeat(4096), Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .usernameAllowed(true)
                        .maxLen(2)
                        .build(),
                        "]'a3-R$", "x", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .usernameAllowed(true)
                        .maxLen(1)
                        .build(),
                        "X", "#A4", Optional.of(PasswordPolicyException.class)));
    }

    @ParameterizedTest
    @MethodSource("maxLenRuleInputs")
    public void testMaxLenRule(
            final DefaultPasswordRuleConf conf,
            final String username,
            final String password,
            final Optional<Class<Exception>> exception
    ) {
        testRule(conf, username, password, exception);
    }

    private static final List<Character> SPECIAL = "!\"#$%&\\'()*+,-./:;<=>?@[\\\\]^_`{|}~"
            .chars()
            .mapToObj(c -> (char) c)
            .toList();

    private static DefaultPasswordRuleConf buildSpecialConf(final int specials, final List<Character> specialChars) {
        DefaultRuleConfBuilder builder = DefaultRuleConfBuilder.builder()
                .maxLen(Integer.MAX_VALUE)
                .usernameAllowed(true)
                .specials(specials);
        specialChars.forEach(builder::special);
        return builder.build();
    }

    private static Stream<Arguments> specialRuleInput() {
        return Stream.of(
                Arguments.of(
                        buildSpecialConf(1, List.of()),
                        null, "a", Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        buildSpecialConf(0, List.of()),
                        "", null, Optional.empty()),
                Arguments.of(
                        buildSpecialConf(0, List.of('*', '^')),
                        "a", "p#", Optional.empty()),
                Arguments.of(
                        buildSpecialConf(1, SPECIAL),
                        "#us!", "password", Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        buildSpecialConf(1, List.of('!', '"', '£')),
                        "admin", "67buyh..!", Optional.empty()),
                Arguments.of(
                        buildSpecialConf(2, SPECIAL),
                        "mrossi", "#523432fsf", Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        buildSpecialConf(2, List.of('a', 'b', 'c', '1', '2')),
                        "fverdi", "HG£abS23ù", Optional.empty()),
                Arguments.of(
                        buildSpecialConf(1, List.of('a', 'b')),
                        "bh2ùa", "Aword", Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        buildSpecialConf(1, List.of('a', 'a', 'a')),
                        null, "aHJ~àF", Optional.empty()),
                Arguments.of(
                        buildSpecialConf(1, SPECIAL),
                        "8!3$", "", Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        buildSpecialConf(1, List.of('イ', 'ン', 'ス')),
                        "user", "ンスa", Optional.empty()),
                Arguments.of(
                        buildSpecialConf(1, List.of(' ', '\t', '\n')),
                        null, "a strange\tpassword", Optional.empty()));
    }

    @ParameterizedTest
    @MethodSource("specialRuleInput")
    public void testSpecialRule(
            final DefaultPasswordRuleConf conf,
            final String username,
            final String password,
            final Optional<Class<Exception>> exception
    ) {
        testRule(conf, username, password, exception);
    }

    private static Stream<Arguments> repeatSameRuleInputs() {
        return Stream.of(
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .repeatSame(0)
                        .build(),
                        null, "", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .repeatSame(-1)
                        .build(),
                        "", "aaa", Optional.empty()),
                // Arguments.of(
                //      DefaultRuleConfBuilder.builder()
                //          .maxLen(Integer.MAX_VALUE)
                //          .usernameAllowed(true)
                //          .repeatSame(1)
                //          .build(), "6%g", "klR2", Optional.of(IllegalStateException.class)),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .repeatSame(2)
                        .build(),
                        "11", null, Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .repeatSame(2)
                        .build(),
                        "admin", "wword", Optional.of(PasswordPolicyException.class)),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .repeatSame(2)
                        .build(),
                        "user", "distinct", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .repeatSame(2)
                        .build(),
                        "82zUfnHCf", "8M3gURAJJ", Optional.of(PasswordPolicyException.class)),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .repeatSame(2)
                        .build(),
                        "jr3EntuYl", "aRRN", Optional.of(PasswordPolicyException.class)),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .repeatSame(3)
                        .build(),
                        "u2gPPj1h0s", "qqf5Xq", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .repeatSame(3)
                        .build(),
                        "z25yyyurRS", "coodee", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .repeatSame(3)
                        .build(),
                        "mH6%", "AAadR62ns", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .repeatSame(2)
                        .build(),
                        "mrossi", "sec#et!", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .repeatSame(2)
                        .build(),
                        "6x", "gH°°ìl°°°", Optional.of(PasswordPolicyException.class))
        );
    }

    @ParameterizedTest
    @MethodSource("repeatSameRuleInputs")
    public void testRepeatSameRule(
            final DefaultPasswordRuleConf conf,
            final String username,
            final String password,
            final Optional<Class<Exception>> exception
    ) {
        testRule(conf, username, password, exception);
    }

    private static Stream<Arguments> usernameAllowedRuleInputs() {
        return Stream.of(
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(false)
                        .build(),
                        null, "p", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(false)
                        .build(),
                        "", "J&", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(false)
                        .build(),
                        "O", "O", Optional.of(PasswordPolicyException.class)),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(false)
                        .build(),
                        "_i", "_I", Optional.of(PasswordPolicyException.class)),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true).
                        build(),
                        "')bqDtl+o'", "')bqDtl+o'", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(false)
                        .build(),
                        "C", "Cu", Optional.of(PasswordPolicyException.class)),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(false)
                        .build(),
                        "a;]v", "tA;]v", Optional.of(PasswordPolicyException.class)),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true)
                        .build(),
                        "KT", "vKT>", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(false)
                        .build(),
                        "Gk", "{gK!", Optional.of(PasswordPolicyException.class)),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(false)
                        .build(),
                        "=", "ò", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(false)
                        .build(),
                        "admin", ":Fi*g%t*", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(false)
                        .build(),
                        "admin", ":Fi*admig%t*", Optional.empty()),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(false)
                        .build(),
                        "user", "7resu", Optional.of(PasswordPolicyException.class)),
                Arguments.of(DefaultRuleConfBuilder.builder()
                        .maxLen(Integer.MAX_VALUE)
                        .usernameAllowed(true).
                        build(),
                        "logistics", "logistics1", Optional.empty())
        );
    }

    @ParameterizedTest
    @MethodSource("usernameAllowedRuleInputs")
    public void testUsernameAllowedRule(
            final DefaultPasswordRuleConf conf,
            final String username,
            final String password,
            final Optional<Class<Exception>> exception
    ) {
        testRule(conf, username, password, exception);
    }

    private static Stream<Arguments> wordsNotPermittedRuleInputs() {
        return Stream.of(
                Arguments.of(
                        DefaultRuleConfBuilder.empty(),
                        null,
                        "p",
                        Optional.empty()),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .words(split("abcdefghijklmnopqrstuvxyz"))
                                .build(),
                        "",
                        "w",
                        Optional.empty()),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .words(split("abcdefghijklmnopqrstuvxyz"))
                                .build(),
                        "",
                        "A",
                        Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .words(split("abcdefghijklmnopqrstuvwxyz"))
                                .build(),
                        "admin",
                        "w",
                        Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .words(split("1234567890"))
                                .build(),
                        "user",
                        "1",
                        Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        DefaultRuleConfBuilder
                                .builder()
                                .words(split("1234567890"))
                                .build(),
                        "0LtG",
                        "dj",
                        Optional.empty()),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .words(split("!\"#$%&\\'()*+,-./:;<=>?@[\\\\]^_`{|}~ \\t\\n"))
                                .build(),
                        "KLLm5",
                        null,
                        Optional.empty()),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .words(split("!\"#$%&\\'()*+,-./:;<=>?@[\\\\]^_`{|}~ \\t\\n"))
                                .build(),
                        "zqqp",
                        "T4AH0",
                        Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .words(List.of("Hy}'_"))
                                .build(),
                        "+K4",
                        "hY}'_!",
                        Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .words(List.of("v~r2i "))
                                .build(),
                        "%4",
                        "hY}'_!v~r2i ",
                        Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .words(List.of("v~r2i ", "GM2+", "Hy}'_"))
                                .build(),
                        "%",
                        "hY}'_\"Hy}'_\"!v~r2i ",
                        Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .words(List.of(")", ";eS|", "6*3<", "det"))
                                .build(),
                        "",
                        ";eS mselet6*3",
                        Optional.empty()),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .words(List.of(")", ";eS|", "6*3<", "det"))
                                .build(),
                        "",
                        ")\\\\cS)6\n",
                        Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .words(List.of(")", ";eS|", "6*3<", "det"))
                                .build(),
                        "",
                        "zYsxt:dedet",
                        Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .words(List.of(")", ";eS", ";eS|", "det"))
                                .build(),
                        ")",
                        "jh7;e;eS83",
                        Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .words(List.of(")", ";eS", ";eS|", "det"))
                                .build(),
                        "det",
                        ",yE<B+XJ>AU",
                        Optional.empty()));
    }

    private static List<String> split(final String s) {
        return split(s, "");
    }

    private static List<String> split(final String s, final String delimiter) {
        return Arrays.stream(s.split(delimiter)).toList();
    }

    @ParameterizedTest
    @MethodSource("wordsNotPermittedRuleInputs")
    public void testWordsNotPermittedRule(
            final DefaultPasswordRuleConf conf,
            final String username,
            final String password,
            final Optional<Class<Exception>> exception
    ) {
        testRule(conf, username, password, Map.of(), exception);
    }

    private static Stream<Arguments> schemasNotPermittedRuleInput() {
        return Stream.of(
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .schema("surname")
                                .build(),
                        "mario",
                        "rossi123",
                        Map.of("surname", List.of("rossi")),
                        Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .schema("name")
                                .schema("surname")
                                .build(),
                        "Pablo",
                        "#dIego",
                        Map.of("surname", List.of("picasso", "diego", "josé", "fransisco de paula", "picasso")),
                        Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .schema("birth")
                                .schema("email")
                                .build(),
                        "admin",
                        "a19700101b",
                        Map.of(
                                "birth", List.of("19700101"),
                                "email",
                                List.of("ZiAk3H6bRx@2A8I7S.com",
                                        "281EhhtBhL@ciAZgW.com",
                                        "11WX06CT5M@dNSM5g.com")),
                        Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        DefaultRuleConfBuilder.empty(),
                        "m",
                        "value@value@value",
                        Map.of("key", List.of("value")),
                        Optional.empty()),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .schema("surname")
                                .build(),
                        "mario",
                        "12ross3",
                        Map.of("surname", List.of("rossi")),
                        Optional.empty()),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .schema("name")
                                .schema("surname")
                                .build(),
                        "Pablo",
                        "#Iego",
                        Map.of("surname", List.of("picasso", "diego", "josé", "fransisco de paula", "picasso")),
                        Optional.empty()),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .schema("birth")
                                .schema("email")
                                .build(),
                        "admin",
                        "a197001iAk3H6bRx@2A8I7S.com01",
                        Map.of(
                                "birth", List.of("19700101"),
                                "email",
                                List.of("ZiAk3H6bRx@2A8I7S.com", "281EhhtBhL@ciAZgW.com", "11WX06CT5M@dNSM5g.com")),
                        Optional.empty()));
    }

    @ParameterizedTest
    @MethodSource("schemasNotPermittedRuleInput")
    public void testSchemasNotPermittedRule(
            final DefaultPasswordRuleConf conf,
            final String username,
            final String password,
            final Map<String, List<Object>> attributes,
            final Optional<Class<Exception>> exception) {
        testRule(conf, username, password, attributes, exception);
    }

    private static Stream<Arguments> mixedRulesInput() {
        return Stream.of(
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .minLen(8).maxLen(9)
                                .alpha(3).lower(2).upper(2)
                                .digit(1)
                                .specials(1).special(SPECIAL)
                                .build(),
                        "Mario", "NeRi1#",
                        Map.of(),
                        Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .minLen(4).maxLen(5)
                                .alpha(2)
                                .digit(2)
                                .specials(1).special(SPECIAL)
                                .build(),
                        "ADMIN", "5o?a1",
                        Map.of(),
                        Optional.empty()),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .minLen(4).maxLen(5)
                                .alpha(2).lower(1).upper(1)
                                .digit(2)
                                .specials(1).special(SPECIAL)
                                .build(),
                        null, "o3k.1",
                        Map.of(),
                        Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .minLen(3).maxLen(4)
                                .alpha(4).upper(2)
                                .digit(1)
                                .specials(1).special(SPECIAL)
                                .build(),
                        null, "JUn3#",
                        Map.of(),
                        Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .minLen(12)
                                .maxLen(11)
                                .build(),
                        "user", "password1231",
                        Map.of(),
                        Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .alpha(3)
                                .maxLen(3)
                                .special('#')
                                .specials(1)
                                .build(),
                        null, "pas",
                        Map.of(),
                        Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .minLen(8)
                                .alpha(4)
                                .upper(1)
                                .special(SPECIAL)
                                .specials(1)
                                .repeatSame(3)
                                .build(),
                        "jon", "ddo#E11a",
                        Map.of(),
                        Optional.empty()),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .special(List.of('£'))
                                .specials(4)
                                .repeatSame(3)
                                .build(),
                        "", "££££",
                        Map.of(),
                        Optional.of(PasswordPolicyException.class)),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .alpha(3).upper(1)
                                .specials(2)
                                .special(List.of('a', 'b', 'c', 'd', 'e'))
                                .build(),
                        "Jon", "dOe",
                        Map.of(),
                        Optional.empty()),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .digit(2)
                                .specials(3)
                                .special(List.of('1', '2', '3', '4', '5'))
                                .build(),
                        "86&53a", "Jord4n23",
                        Map.of(),
                        Optional.empty()),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .minLen(20)
                                .digit(5)
                                .alpha(5)
                                .specials(5)
                                .build(),
                        "admin", null,
                        Map.of(),
                        Optional.empty()),
                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .minLen(8).maxLen(20)
                                .alpha(4).lower(2).upper(2)
                                .digit(2)
                                .specials(2).special(SPECIAL)
                                .schema("surname")
                                .build(),
                        "mrossi", "aB1!Rossi2@",
                        Map.of("surname", List.of("Rossi")),
                        Optional.of(PasswordPolicyException.class)),

                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .minLen(15)
                                .schema("surname")
                                .build(),
                        "mrossi", "Rossi123!",
                        Map.of("surname", List.of("Rossi")),
                        Optional.of(PasswordPolicyException.class)),

                Arguments.of(
                        DefaultRuleConfBuilder.builder()
                                .minLen(8)
                                .repeatSame(3)
                                .schema("department")
                                .build(),
                        "user", "123Boooook99",
                        Map.of("department", List.of("Boooook")),
                        Optional.of(PasswordPolicyException.class)));
    }

    @ParameterizedTest
    @MethodSource("mixedRulesInput")
    public void testMixedRules(
            final DefaultPasswordRuleConf conf,
            final String username,
            final String password,
            final Map<String, List<Object>> attributes,
            final Optional<Class<Exception>> exception) {
        testRule(conf, username, password, attributes, exception);
    }

    private void testRule(
            final DefaultPasswordRuleConf conf,
            final String username,
            final String password,
            final Optional<Class<Exception>> exception) {
        testRule(conf, username, password, Map.of(), exception);
    }

    private void testRule(
            final DefaultPasswordRuleConf conf,
            final String username,
            final String password,
            final Map<String, List<Object>> attributes,
            final Optional<Class<Exception>> exception) {
        User user = mock(User.class);
        when(user.getPlainAttr(anyString())).thenAnswer(i -> {
            String schema = i.getArgument(0, String.class);
            if (attributes.containsKey(schema)) {
                PlainAttr attr = mock(PlainAttr.class);
                List<String> values = attributes.get(schema).stream()
                        .map(o -> {
                            if (o instanceof Integer || o instanceof Long) {
                                return DecimalFormat.getInstance().format(o);
                            } else if (o instanceof Date) {
                                return DateFormat.getDateInstance().format(o);
                            } else if (o instanceof Boolean) {
                                return o.toString();
                            } else if (o instanceof String) {
                                return o.toString();
                            } else {
                                throw new IllegalArgumentException(
                                        "type not supported " + o.getClass().getCanonicalName());
                            }
                        }).toList();
                when(attr.getValuesAsStrings()).thenReturn(values);
                return Optional.of(attr);
            } else {
                return Optional.empty();
            }
        });
        when(user.getUsername()).thenReturn(username);
        when(user.getPassword()).thenReturn(password);

        DefaultPasswordRule rule = new DefaultPasswordRule();
        rule.setConf(conf);
        assertEquals(conf, rule.getConf());
        if (exception.isPresent()) {
            Assertions.assertThrows(exception.get(), () -> rule.enforce(user, password));
        } else {
            rule.enforce(user, password);
        }
    }

    private void assertEquals(final PasswordRuleConf c1, final PasswordRuleConf c2) {
        Assertions.assertEquals(c1, c2);
    }
}
