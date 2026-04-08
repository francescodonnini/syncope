package org.apache.syncope.core.spring.policy;

import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;

public class RandoopHelper {
    public static DefaultPasswordRule setupRule(DefaultPasswordRuleConf conf) {
        DefaultPasswordRule rule = new DefaultPasswordRule();
        rule.setConf(conf);
        return rule;
    }
}
