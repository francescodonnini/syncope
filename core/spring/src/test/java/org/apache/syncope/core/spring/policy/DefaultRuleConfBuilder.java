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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;

public final class DefaultRuleConfBuilder {
    private int alpha = 0;
    private int digit = 0;
    private int lower = 0;
    private int upper = 0;
    private int minLen = 0;
    private int maxLen = 0;
    private int specials = 0;
    private int repeatSame = 0;
    private final Set<Character> specialChars = new HashSet<>();
    private boolean usernameAllowed = false;
    private final Set<String> illegalWords = new HashSet<>();

    private final Set<String> schemas = new HashSet<>();

    public static DefaultRuleConfBuilder builder() {
        return new DefaultRuleConfBuilder();
    }

    public static DefaultPasswordRuleConf empty() {
        return builder().build();
    }

    private DefaultRuleConfBuilder() {
    }

    public DefaultPasswordRuleConf build() {
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setAlphabetical(alpha);
        conf.setLowercase(lower);
        conf.setUppercase(upper);
        conf.setDigit(digit);
        conf.setSpecial(specials);
        conf.getSpecialChars().addAll(specialChars);
        conf.setMinLength(minLen);
        conf.setMaxLength(maxLen);
        conf.setUsernameAllowed(usernameAllowed);
        conf.getWordsNotPermitted().addAll(illegalWords);
        conf.setRepeatSame(repeatSame);
        conf.getSchemasNotPermitted().addAll(schemas);
        return conf;
    }

    public DefaultRuleConfBuilder schema(final String schema) {
        schemas.add(schema);
        return this;
    }

    public DefaultRuleConfBuilder repeatSame(final int repeatSame) {
        this.repeatSame = repeatSame;
        return this;
    }

    public DefaultRuleConfBuilder alpha(final int alpha) {
        this.alpha = alpha;
        return this;
    }

    public DefaultRuleConfBuilder digit(final int digit) {
        this.digit = digit;
        return this;
    }

    public DefaultRuleConfBuilder lower(final int lower) {
        this.lower = lower;
        return this;
    }

    public DefaultRuleConfBuilder upper(final int upper) {
        this.upper = upper;
        return this;
    }

    public DefaultRuleConfBuilder minLen(final int minLen) {
        this.minLen = minLen;
        return this;
    }

    public DefaultRuleConfBuilder maxLen(final int maxLen) {
        this.maxLen = maxLen;
        return this;
    }

    public DefaultRuleConfBuilder specials(final int specials) {
        this.specials = specials;
        return this;
    }

    public DefaultRuleConfBuilder special(final List<Character> specials) {
        this.specialChars.addAll(specials);
        return this;
    }

    public DefaultRuleConfBuilder special(final char special) {
        this.specialChars.add(special);
        return this;
    }

    public DefaultRuleConfBuilder usernameAllowed(final boolean usernameAllowed) {
        this.usernameAllowed = usernameAllowed;
        return this;
    }

    public DefaultRuleConfBuilder words(final List<String> words) {
        illegalWords.addAll(words);
        return this;
    }
}
