package org.apache.syncope.core.spring.policy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefaultPasswordRuleGeminiTest {
    private DefaultPasswordRule rule;
    private DefaultPasswordRuleConf conf;

    @Mock
    private User user;

    @BeforeEach
    public void setUp() {
        rule = new DefaultPasswordRule();
        conf = new DefaultPasswordRuleConf();

        // Base configuration for tests
        conf.setMinLength(8);
        conf.setMaxLength(20);
        conf.setUppercase(1);
        conf.setDigit(1);
        conf.setUsernameAllowed(false);

        rule.setConf(conf);
    }

    @Test
    public void testEnforce_NullPassword() {
        // Enforce should ignore null passwords without throwing exceptions
        assertDoesNotThrow(() -> rule.enforce(user, null));
    }

    @Test
    public void testEnforce_ValidPassword() {
        when(user.getUsername()).thenReturn("johndoe");

        assertDoesNotThrow(() -> rule.enforce(user, "Valid1Password"));
    }

    @Test
    public void testEnforce_InvalidPassword_Length() {
        when(user.getUsername()).thenReturn("johndoe");

        // Expect an exception due to policy violation (Length is 5, minimum is 8)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            rule.enforce(user, "Sh1rt");
        });

        // Verify an exception message is generated without depending on exact passay.properties phrasing
        assertNotNull(exception.getMessage());
    }

    @Test
    public void testEnforce_InvalidPassword_UsernameNotAllowed() {
        when(user.getUsername()).thenReturn("johndoe");

        // Expect an exception due to policy violation (username contained in password)
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            rule.enforce(user, "johndoe1A");
        });

        assertNotNull(exception.getMessage());
    }

    @Test
    public void testEnforce_InvalidPassword_WordsNotPermitted() {
        conf.getWordsNotPermitted().add("syncope");
        rule.setConf(conf); // Re-initialize passay validator

        when(user.getUsername()).thenReturn("johndoe");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            rule.enforce(user, "syncope1A");
        });

        // This message is hardcoded in the DefaultPasswordRule class, so it is safe to assert
        assertEquals("Used word(s) not permitted", exception.getMessage());
    }

    @Test
    public void testEnforce_InvalidPassword_SchemasNotPermitted() {
        conf.getSchemasNotPermitted().add("surname");
        rule.setConf(conf); // Re-initialize passay validator

        PlainAttr surnameAttr = mock(PlainAttr.class);
        when(surnameAttr.getValuesAsStrings()).thenReturn(List.of("smith"));

        when(user.getPlainAttr("surname")).thenReturn(Optional.of(surnameAttr));
        when(user.getUsername()).thenReturn("johndoe");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            rule.enforce(user, "smith1A!");
        });

        // This message is hardcoded in the DefaultPasswordRule class, so it is safe to assert
        assertEquals("Used word(s) not permitted", exception.getMessage());
    }

    @Test
    public void testEnforce_ValidPassword_WithEmptyPlainAttr() {
        conf.getSchemasNotPermitted().add("surname");
        rule.setConf(conf);

        PlainAttr surnameAttr = mock(PlainAttr.class);
        // User has the attribute, but it's empty
        when(surnameAttr.getValuesAsStrings()).thenReturn(List.of());

        when(user.getPlainAttr("surname")).thenReturn(Optional.of(surnameAttr));
        when(user.getUsername()).thenReturn("johndoe");

        assertDoesNotThrow(() -> rule.enforce(user, "Valid1Password"));
    }
}