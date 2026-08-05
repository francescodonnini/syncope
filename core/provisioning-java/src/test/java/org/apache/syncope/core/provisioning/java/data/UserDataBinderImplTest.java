package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.jexl.JexlTools;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserDataBinderImplTest {
    @Mock private AnyTypeDAO anyTypeDAO;
    @Mock
    private RealmSearchDAO realmSearchDAO;
    @Mock private AnyTypeClassDAO anyTypeClassDAO;
    @Mock private AnyObjectDAO anyObjectDAO;
    @Mock private UserDAO userDAO;
    @Mock private GroupDAO groupDAO;
    @Mock private PlainSchemaDAO plainSchemaDAO;
    @Mock private ExternalResourceDAO resourceDAO;
    @Mock private RelationshipTypeDAO relationshipTypeDAO;
    @Mock private EntityFactory entityFactory;
    @Mock private AnyUtilsFactory anyUtilsFactory;
    @Mock private DerAttrHandler derAttrHandler;
    @Mock private MappingManager mappingManager;
    @Mock private IntAttrNameParser intAttrNameParser;
    @Mock private OutboundMatcher outboundMatcher;
    @Mock private PlainAttrValidationManager validator;
    @Mock private JexlTools jexlTools;

    @Mock private RoleDAO roleDAO;
    @Mock private SecurityQuestionDAO securityQuestionDAO;
    @Mock private AccessTokenDAO accessTokenDAO;
    @Mock private DelegationDAO delegationDAO;
    @Mock private ConfParamOps confParamOps;
    @Mock private SecurityProperties securityProperties;

    @InjectMocks
    private UserDataBinderImpl sut;

    @Test
    public void test() {
        when(securityProperties.getAnonymousUser()).thenReturn("unauthenticated");
        UserTO userTO = sut.getAuthenticatedUserTO();
        Assertions.assertEquals(sut.getAuthenticatedUserTO().getUsername(), "unauthenticated");
    }
}
