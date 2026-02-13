package org.apache.syncope.core.spring.policy;

import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
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
                Arguments.of(0, "user", null, Optional.empty()),
                Arguments.of(1, "m#]+37[$", "\"%=Y$_", Optional.of(PasswordPolicyException.class)),
                Arguments.of(0, "~0{tw", "\"L.SI-%\"", Optional.empty()),
                Arguments.of(1, "£$&FGA%s", "$KLuA83", Optional.empty()),
                Arguments.of(2, "br532fd", "30s-Tù", Optional.of(PasswordPolicyException.class)),
                Arguments.of(2, "admin", "=]G5eE0a(U`l", Optional.empty()),
                Arguments.of(1, "34fd2d", "~_#0_{h4G", Optional.empty()),
                Arguments.of(Integer.MAX_VALUE, "verylongusername", "verylongpassword", Optional.of(PasswordPolicyException.class)));
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
        testRule(rule, username, password, exception);
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
                Arguments.of(0, null, "a", Optional.empty()),
                Arguments.of(-1, "2", null, Optional.empty()),
                Arguments.of(1, "", "*", Optional.empty()),
                Arguments.of(1, "u", ",^", Optional.of(PasswordPolicyException.class)),
                Arguments.of(2, "usr", "12", Optional.empty()),
                Arguments.of(Integer.MAX_VALUE, "admin", "#".repeat(4096), Optional.empty()),
                Arguments.of(2, "]'a3-R$", "x", Optional.empty()),
                Arguments.of(1, "X", "#A4", Optional.of(PasswordPolicyException.class)));
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
        testRule(rule, username, password, exception);
    }

    private static List<Character> SPECIAL = "!\"#$%&\\'()*+,-./:;<=>?@[\\\\]^_`{|}~"
            .chars()
            .mapToObj(c -> (char) c)
            .toList();

    private static Stream<Arguments> specialRuleInput() {
        return Stream.of(
                Arguments.of(1, List.of(), null, "a", Optional.of(PasswordPolicyException.class)),
                Arguments.of(0, List.of(), "", null, Optional.empty()),
                Arguments.of(0, List.of('*', '^'), "a", "p#", Optional.empty()),
                Arguments.of(1, SPECIAL, "#us!", "password", Optional.of(PasswordPolicyException.class)),
                Arguments.of(1, List.of('!', '"', '£'), "admin", "67buyh..!", Optional.empty()),
                Arguments.of(2, SPECIAL, "mrossi", "#523432fsf", Optional.of(PasswordPolicyException.class)),
                Arguments.of(2, List.of('a', 'b', 'c', '1', '2'), "fverdi", "HG£abS23ù", Optional.empty()),
                Arguments.of(1, List.of('a', 'b'), "bh2ùa", "Aword", Optional.of(PasswordPolicyException.class)),
                Arguments.of(1, List.of('a', 'a', 'a'), null, "aHJ~àF", Optional.empty()),
                Arguments.of(1, SPECIAL, "8!3$", "", Optional.of(PasswordPolicyException.class)),
                Arguments.of(1, List.of('イ', 'ン', 'ス'), "user", "ンスa", Optional.empty()),
                Arguments.of(1, List.of(' ', '\t', '\n'), null, "a strange\tpassword", Optional.empty()));
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
                Arguments.of(0, null, "", Optional.empty()),
                Arguments.of(-1, "", "aaa", Optional.empty()),
                // Arguments.of(1, "6%g", "klR2", Optional.of(IllegalStateException.class)),
                Arguments.of(2, "11", null, Optional.empty()),
                Arguments.of(2, "admin", "wword", Optional.of(PasswordPolicyException.class)),
                Arguments.of(2, "user", "distinct", Optional.empty()),
                Arguments.of(2, "82zUfnHCf", "8M3gURAJJ", Optional.of(PasswordPolicyException.class)),
                Arguments.of(2, "jr3EntuYl", "aRRN", Optional.of(PasswordPolicyException.class)),
                Arguments.of(3, "u2gPPj1h0s", "qqf5Xq", Optional.empty()),
                Arguments.of(3, "z25yyyurRS", "coodee", Optional.empty()),
                Arguments.of(3, "mH6%", "AAadR62ns", Optional.empty()),
                Arguments.of(2, "mrossi", "sec#et!", Optional.empty()),
                Arguments.of(2, "6x", "gH°°ìl°°°", Optional.of(PasswordPolicyException.class))
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

    private static Stream<Arguments> wordsNotPermittedRuleInputs() {
        return Stream.of(
                Arguments.of(List.of(), null, "p", Optional.empty()),
                Arguments.of(split("abcdefghijklmnopqrstuvxyz"), "", "w", Optional.empty()),
                // Parole non permesse sono tutte alfabetiche, la password è maiuscola
                Arguments.of(split("abcdefghijklmnopqrstuvxyz"), "", "A", Optional.of(PasswordPolicyException.class)),
                // Parole non permesse sono tutte alfabetiche
                Arguments.of(split("abcdefghijklmnopqrstuvwxyz"), "admin", "w", Optional.of(PasswordPolicyException.class)),
                // Parole non permesse sono tutti numeri
                Arguments.of(split("1234567890"), "user", "1", Optional.of(PasswordPolicyException.class)),
                // Parole non permesse sono tutti numeri, la password ne è sprovvista, lo username no
                Arguments.of(split("1234567890"), "0LtG", "dj", Optional.empty()),
                // Parole non permesse sono simboli stampabili (né lettere né numeri)
                Arguments.of(split("!\"#$%&\\'()*+,-./:;<=>?@[\\\\]^_`{|}~ \\t\\n"), "KLLm5", null, Optional.empty()),
                Arguments.of(split("!\"#$%&\\'()*+,-./:;<=>?@[\\\\]^_`{|}~ \\t\\n"), "zqqp", "T4AH0", Optional.of(PasswordPolicyException.class)),
                // La parola compare all'inizio
                Arguments.of(List.of("Hy}'_"), "+K4", "hY}'_!", Optional.of(PasswordPolicyException.class)),
                // La parola compare alla fine
                Arguments.of(List.of("v~r2i "), "%4", "hY}'_!v~r2i ", Optional.of(PasswordPolicyException.class)),
                // La parola è nel mezzo
                Arguments.of(List.of("v~r2i ", "GM2+", "Hy}'_"), "%", "hY}'_\"Hy}'_\"!v~r2i ", Optional.of(PasswordPolicyException.class)),
                // Le parole hanno una corrispondenza parziale con una sottostringa della password
                Arguments.of(List.of(")", ";eS|", "6*3<", "det"), "", ";eS mselet6*3", Optional.empty()),
                // La parola offensiva è la prima
                Arguments.of(List.of(")", ";eS|", "6*3<", "det"), "", ")\\\\cS)6\n", Optional.of(PasswordPolicyException.class)),
                // La parola offensiva è l'ultima
                Arguments.of(List.of(")", ";eS|", "6*3<", "det"), "", "zYsxt:dedet", Optional.of(PasswordPolicyException.class)),
                // Una parola proibita è sotto-stringa di un'altra parola proibita
                Arguments.of(List.of(")", ";eS", ";eS|", "det"), ")", "jh7;e;eS83", Optional.of(PasswordPolicyException.class)),
                // Password corretta, username non contiene parola proibita
                Arguments.of(List.of(")", ";eS", ";eS|", "det"), "det", ",yE<B+XJ>AU", Optional.empty()));
    }

    private static List<String> split(String s) {
        return split(s, "");
    }

    private static List<String> split(String s, String delimiter) {
        return Arrays.stream(s.split(delimiter)).toList();
    }

    @ParameterizedTest
    @MethodSource("wordsNotPermittedRuleInputs")
    public void testWordsNotPermittedRule(
            List<String> words,
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
        conf.setUsernameAllowed(true);
        conf.getWordsNotPermitted().addAll(words);
        DefaultPasswordRule rule = new DefaultPasswordRule();
        rule.setConf(conf);
        testRule(rule, username, password, exception);
    }

    private static Stream<Arguments> validMixedRulesInput() {
        return Stream.of(
                Arguments.of(0, 0, 0, 12, 11, 0, 0, List.of(), 0, true, List.of(), "user", "password1231", Optional.empty())
        );
    }

    @ParameterizedTest
    @MethodSource("validMixedRulesInput")
    public void testValidMixedRules(
            int alpha,
            int digit,
            int lower,
            int minLen,
            int maxLen,
            int repeat,
            int special,
            List<Character> specialChars,
            int upper,
            boolean usernameAllowed,
            List<String> wordsNotPermitted,
            String username,
            String password,
            Optional<Class<Exception>> exception) {
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setAlphabetical(alpha);
        conf.setDigit(digit);
        conf.setLowercase(lower);
        conf.setMinLength(minLen);
        conf.setMaxLength(maxLen);
        conf.setRepeatSame(repeat);
        conf.setSpecial(special);
        conf.getSpecialChars().addAll(specialChars);
        conf.setUppercase(upper);
        conf.setUsernameAllowed(usernameAllowed);
        conf.getWordsNotPermitted().addAll(wordsNotPermitted);
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

    private static Stream<Arguments> invalidMixedRulesInput() {
        return Stream.of(
        );
    }

    @ParameterizedTest
    @MethodSource("invalidMixedRulesInput")
    public void testValidMixedRules(
            int alpha,
            int digit,
            int lower,
            int minLen,
            int maxLen,
            int repeat,
            int special,
            List<Character> specialChars,
            int upper,
            boolean usernameAllowed,
            List<String> wordsNotPermitted,
            String username,
            String password) {
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setAlphabetical(alpha);
        conf.setDigit(digit);
        conf.setLowercase(lower);
        conf.setMinLength(minLen);
        conf.setMaxLength(maxLen);
        conf.setRepeatSame(repeat);
        conf.setSpecial(special);
        conf.getSpecialChars().addAll(specialChars);
        conf.setUppercase(upper);
        conf.setUsernameAllowed(usernameAllowed);
        conf.getWordsNotPermitted().addAll(wordsNotPermitted);
        DefaultPasswordRule rule = new DefaultPasswordRule();
        rule.setConf(conf);
        Assertions.assertDoesNotThrow(() -> rule.enforce(username, password));
    }
}
