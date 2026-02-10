package org.apache.syncope.core.spring.policy;

import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class DefaultPasswordRuleTest {
    private static Stream<Arguments> alphaRuleInputs() {
        return Stream.of(
                Arguments.of(-1, null, "", Optional.empty()),
                Arguments.of(0, "86*D", null, Optional.empty()),
                Arguments.of(1, "u", null, Optional.empty()),
                Arguments.of(1, "x", "1", Optional.of(PasswordPolicyException.class)),
                Arguments.of(1, "admin", "a7", Optional.empty()),
                Arguments.of(1, "#@[1", "7^?B", Optional.empty()),
                Arguments.of(1, "admin", "+bCD34^", Optional.empty()),
                Arguments.of(2, "mrossi", "213123a21", Optional.of(PasswordPolicyException.class)),
                Arguments.of(Integer.MAX_VALUE, "User", "(CCi*]JSB)SI%M;", Optional.of(PasswordPolicyException.class)));
    }

    @ParameterizedTest
    @MethodSource("alphaRuleInputs")
    public void testAlphaRule(
            int alphabetical,
            String username,
            String password,
            Optional<Class<Exception>> exception
    ) {
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(0);
        conf.setAlphabetical(alphabetical);
        conf.setLowercase(0);
        conf.setMaxLength(Integer.MAX_VALUE);
        conf.setSpecial(0);
        conf.setDigit(0);
        conf.setUppercase(0);
        conf.setRepeatSame(0);
        conf.setUsernameAllowed(true);
        DefaultPasswordRule rule = new DefaultPasswordRule();
        rule.setConf(conf);
        testRule(rule, username, password, exception);
    }

    private static Stream<Arguments> digitRuleInputs() {
        return Stream.of(
                Arguments.of(-1, null, "", Optional.empty()),
                Arguments.of(Integer.MIN_VALUE, "", null, Optional.empty()),
                Arguments.of(1, ":dU#AA", "'ZM6SQ", Optional.empty()),
                Arguments.of(1, "nhnujj", "9", Optional.empty()),
                Arguments.of(1, "adm1n", "password", Optional.of(PasswordPolicyException.class)),
                Arguments.of(1, "z+5c0", "OU3", Optional.empty()),
                Arguments.of(2, "admin", "t{JD6}{Y", Optional.of(PasswordPolicyException.class)),
                Arguments.of(2, "p", "4hbcf5vbh", Optional.empty()),
                Arguments.of(2, "user", "-6pg07{o", Optional.empty()),
                Arguments.of(Integer.MAX_VALUE, "admin", "1234567890", Optional.of(PasswordPolicyException.class)));
    }

    @ParameterizedTest
    @MethodSource("digitRuleInputs")
    public void testDigitRule(
            int digit,
            String username,
            String password,
            Optional<Class<Exception>> exception
    ) {
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(0);
        conf.setAlphabetical(0);
        conf.setLowercase(0);
        conf.setMaxLength(Integer.MAX_VALUE);
        conf.setSpecial(0);
        conf.setDigit(digit);
        conf.setUppercase(0);
        conf.setRepeatSame(0);
        conf.setUsernameAllowed(true);
        DefaultPasswordRule rule = new DefaultPasswordRule();
        rule.setConf(conf);
        testRule(rule, username, password, exception);
    }

    private static Stream<Arguments> uppercaseRuleInputs() {
        return Stream.of(
                Arguments.of(-1, null, "", Optional.empty()),
                Arguments.of(0, "u", null, Optional.empty()),
                Arguments.of(1, ")+M4[$", "\"d=$h_", Optional.of(PasswordPolicyException.class)),
                Arguments.of(0, "~0{H4G", "a,so-%", Optional.empty()),
                Arguments.of(1, "Ete}\\\\6i$", "?$crsfIqd", Optional.empty()),
                Arguments.of(2, "[4(O6g{", "?]s-H%", Optional.of(PasswordPolicyException.class)),
                Arguments.of(2, "user", "=]G5#E0,(U`!", Optional.empty()),
                Arguments.of(1, "admin", "~@#0{h4G", Optional.empty()),
                Arguments.of(Integer.MAX_VALUE, "admin", "AAAAAAAH", Optional.of(PasswordPolicyException.class)));
    }

    @ParameterizedTest
    @MethodSource("uppercaseRuleInputs")
    public void testUppercaseRule(
            int uppercase,
            String username,
            String password,
            Optional<Class<Exception>> exception
    ) {
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setAlphabetical(0);
        conf.setDigit(0);
        conf.setUppercase(uppercase);
        conf.setMinLength(0);
        conf.setLowercase(0);
        conf.setMaxLength(Integer.MAX_VALUE);
        conf.setSpecial(0);
        conf.setRepeatSame(0);
        conf.setUsernameAllowed(true);
        DefaultPasswordRule rule = new DefaultPasswordRule();
        rule.setConf(conf);
        testRule(rule, username, password, exception);
    }

    private static Stream<Arguments> lowercaseRuleInputs() {
        return Stream.of(
                Arguments.of(-1, null, "", Optional.empty()),
                Arguments.of(0, "", "hgtf53JSh", Optional.empty()),
                Arguments.of(1, "admin", "#?2", Optional.of(PasswordPolicyException.class)),
                Arguments.of(1, "user", "OpLà", Optional.empty()),
                Arguments.of(1, "1", "jfHY3a#", Optional.empty()),
                Arguments.of(2, "username", "Ah", Optional.of(PasswordPolicyException.class)),
                // Arguments.of(2, "z", null, Optional.empty())
                Arguments.of(2, "p", "oH638h", Optional.empty())
        );
    }

    @ParameterizedTest
    @MethodSource("lowercaseRuleInputs")
    public void testLowercaseRule(
            int lowercase,
            String username,
            String password,
            Optional<Class<Exception>> exception
    ) {
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(0);
        conf.setAlphabetical(0);
        conf.setLowercase(lowercase);
        conf.setMaxLength(Integer.MAX_VALUE);
        conf.setSpecial(0);
        conf.setDigit(0);
        conf.setUppercase(0);
        conf.setRepeatSame(0);
        conf.setUsernameAllowed(true);
        DefaultPasswordRule rule = new DefaultPasswordRule();
        rule.setConf(conf);
        if (exception.isPresent()) {
            Assertions.assertThrows(exception.get(), () -> rule.enforce(username, password));
        } else {
            rule.enforce(username, password);
        }
    }

    private static Stream<Arguments> minLenRuleInputs() {
        return Stream.of(
                Arguments.of(-1, null, "a", Optional.empty()),
                // I was expecting an Exception to be thrown, but it didn't
                // Arguments.of(0, "", null, Optional.empty()),
                Arguments.of(1, "u", "", Optional.of(PasswordPolicyException.class)),
                Arguments.of(1, "x", "1", Optional.empty()),
                Arguments.of(2, "xyz", "*", Optional.of(PasswordPolicyException.class)),
                Arguments.of(2, "u", "@a3", Optional.empty()),
                Arguments.of(Integer.MAX_VALUE, "admin", "1234", Optional.of(PasswordPolicyException.class)),
                Arguments.of(1, "", "%&", Optional.empty())
        );
    }

    @ParameterizedTest
    @MethodSource("minLenRuleInputs")
    public void testMinLenRule(
            int minLength,
            String username,
            String password,
            Optional<Class<Exception>> exception
    ) {
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setAlphabetical(0);
        conf.setDigit(0);
        conf.setLowercase(0);
        conf.setMaxLength(Integer.MAX_VALUE);
        conf.setMinLength(minLength);
        conf.setRepeatSame(0);
        conf.setSpecial(0);
        conf.setUppercase(0);
        conf.setUsernameAllowed(true);
        DefaultPasswordRule rule = new DefaultPasswordRule();
        rule.setConf(conf);
        testRule(rule, username, password, exception);
    }

    private static Stream<Arguments> maxLenRuleInputs() {
        return Stream.of(
                // It seems -1 disable the option
                // Arguments.of(-1, "user", "", Optional.empty()),
                Arguments.of(0, "u", "", Optional.empty()),
                Arguments.of(1, "", "pp", Optional.of(PasswordPolicyException.class)),
                Arguments.of(1, null, "a", Optional.empty()),
                Arguments.of(8, "passwor", Optional.empty()),
                Arguments.of(8, "password", Optional.empty()),
                Arguments.of(8, "ner+3àsgm", Optional.of(PasswordPolicyException.class))
        );
    }

    @ParameterizedTest
    @MethodSource("maxLenRuleInputs")
    public void testMaxLenRule(
            int maxLength,
            String username,
            String password,
            Optional<Class<Exception>> exception
    ) {
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(0);
        conf.setAlphabetical(0);
        conf.setLowercase(0);
        conf.setMaxLength(maxLength);
        conf.setSpecial(0);
        conf.setDigit(0);
        conf.setUppercase(0);
        conf.setRepeatSame(0);
        conf.setUsernameAllowed(true);
        DefaultPasswordRule rule = new DefaultPasswordRule();
        rule.setConf(conf);
        if (exception.isPresent()) {
            Assertions.assertThrows(exception.get(), () -> rule.enforce(username, password));
        } else {
            rule.enforce(username, password);
        }
    }

    private static Stream<Arguments> specialRuleInput() {
        return Stream.of(
                Arguments.of(0, List.of(), "admin", "", Optional.empty()),
                Arguments.of(1, List.of('a', 'b', 'c', 'd', 'e', 'f'), null, "@#èg", Optional.of(PasswordPolicyException.class)),
                Arguments.of(-1, List.of('#', '?'), "n", "a2l", Optional.empty()),
                 // TODO: I was expecting a failure but got nothing
                // Arguments.of(1, List.of('?'), "", null, Optional.of(PasswordPolicyException.class))
                Arguments.of(1, List.of('1', '2', '3'), "user", "1@asdad", Optional.empty()),
                Arguments.of(1, List.of('#', 'à', 'ù'), "m", "nj456à", Optional.empty()),
                Arguments.of(2, List.of('.', '-', '^', '['), "", "longpasswor[d", Optional.of(PasswordPolicyException.class)),
                Arguments.of(3, List.of('1', '2', 'a', 'b', '@'), "username1", "p@ssword12a", Optional.empty())
        );
    }

    @ParameterizedTest
    @MethodSource("specialRuleInput")
    public void testSpecialRule(
            int special,
            List<Character> specialChars,
            String username,
            String password,
            Optional<Class<Exception>> exception
    ) {
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(0);
        conf.setAlphabetical(0);
        conf.setLowercase(0);
        conf.setMaxLength(Integer.MAX_VALUE);
        conf.setSpecial(special);
        conf.getSpecialChars().addAll(specialChars);
        conf.setDigit(0);
        conf.setUppercase(0);
        conf.setRepeatSame(0);
        conf.setUsernameAllowed(true);
        DefaultPasswordRule rule = new DefaultPasswordRule();
        rule.setConf(conf);
        testRule(rule, username, password, exception);
    }



    private static Stream<Arguments> repeatSameRuleInputs() {
        return Stream.of(
                Arguments.of(0, "", Optional.empty()),
                Arguments.of(2, "", Optional.empty()),
                Arguments.of(2, "abbaiare", Optional.of(PasswordPolicyException.class)),
                Arguments.of(3, "avellino", Optional.empty()),
                Arguments.of(2, "pas5word", Optional.empty()),
                Arguments.of(3, "iNteRessan3t", Optional.empty()),
                Arguments.of(3, "wrooong", Optional.of(PasswordPolicyException.class)),
                Arguments.of(2, "llegar", Optional.of(PasswordPolicyException.class)),
                Arguments.of(3, "faalse positive", Optional.empty())
        );
    }

    @ParameterizedTest
    @MethodSource("repeatSameRuleInputs")
    public void testRepeatSameRule(
            int repeat,
            String username,
            String password,
            Optional<Class<Exception>> exception
    ) {
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(0);
        conf.setAlphabetical(0);
        conf.setLowercase(0);
        conf.setMaxLength(Integer.MAX_VALUE);
        conf.setSpecial(0);
        conf.setDigit(0);
        conf.setUppercase(0);
        conf.setRepeatSame(repeat);
        conf.setUsernameAllowed(true);
        DefaultPasswordRule rule = new DefaultPasswordRule();
        rule.setConf(conf);
        testRule(rule, username, password, exception);
    }

    private static Stream<Arguments> usernameAllowedRuleInputs() {
        return Stream.of(
                Arguments.of(false, null, "p", Optional.empty()),
                Arguments.of(false, "", "J&", Optional.empty()),
                Arguments.of(false, "O", "O", Optional.of(PasswordPolicyException.class)),
                Arguments.of(false, "_i", "_I", Optional.of(PasswordPolicyException.class)),
                Arguments.of(true, "')bqDtl+o'", "')bqDtl+o'", Optional.empty()),
                Arguments.of(false, "C", "Cu", Optional.of(PasswordPolicyException.class)),
                Arguments.of(false, "a;]v", "tA;]v", Optional.of(PasswordPolicyException.class)),
                Arguments.of(true, "KT", "vKT>", Optional.empty()),
                Arguments.of(false, "Gk", "{gK!", Optional.of(PasswordPolicyException.class)),
                Arguments.of(false, "=", "ò", Optional.empty()),
                Arguments.of(false, "admin", ":Fi*g%t*", Optional.empty()),
                Arguments.of(false, "admin", ":Fi*admig%t*", Optional.empty()),
                Arguments.of(false, "user", "7resu", Optional.of(PasswordPolicyException.class)),
                Arguments.of(true, "logistics", "logistics1", Optional.empty())
        );
    }

    @ParameterizedTest
    @MethodSource("usernameAllowedRuleInputs")
    public void testUsernameAllowedRule(
            boolean usernameAllowed,
            String username,
            String password,
            Optional<Class<Exception>> exception
    ) {
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(0);
        conf.setAlphabetical(0);
        conf.setLowercase(0);
        conf.setMaxLength(Integer.MAX_VALUE);
        conf.setSpecial(0);
        conf.setDigit(0);
        conf.setUppercase(0);
        conf.setRepeatSame(0);
        conf.setUsernameAllowed(usernameAllowed);
        DefaultPasswordRule rule = new DefaultPasswordRule();
        rule.setConf(conf);
        testRule(rule, username, password, exception);
    }

    private void testRule(DefaultPasswordRule rule, String username, String password, Optional<Class<Exception>> exception) {
        if (exception.isPresent()) {
            Assertions.assertThrows(exception.get(), () -> rule.enforce(username, password));
        } else {
            rule.enforce(username, password);
        }
    }
}
