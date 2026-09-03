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
package org.apache.syncope.core.persistence.neo4j.dao;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.text.TextStringBuilder;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.JAXRSService;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.AuxClassCond;
import org.apache.syncope.core.persistence.api.dao.search.DynRealmCond;
import org.apache.syncope.core.persistence.api.dao.search.MemberCond;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.api.dao.search.RoleCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.persistence.common.dao.AbstractAnySearchDAO;
import org.apache.syncope.core.persistence.neo4j.dao.repo.AnyRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.DynRealmRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.GroupRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.RoleRepoExt;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyType;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyTypeClass;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jDynRealm;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRealm;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRelationshipType;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRole;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jAMembership;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jARelationship;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jAnyObject;
import org.apache.syncope.core.persistence.neo4j.entity.group.Neo4jGroup;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUMembership;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jURelationship;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.util.Streamable;

public class Neo4jAnySearchDAO extends AbstractAnySearchDAO {

    /**
     * {@code MATCH (n) } is the entry point of virtually every leaf condition query fragment built by this class.
     */
    protected static final String MATCH_N = "MATCH (n) ";

    /**
     * {@code WHERE } is appended before almost every predicate built by this class.
     */
    protected static final String WHERE = "WHERE ";

    /**
     * Fragment used to open a Cypher node property filter, e.g. {@code (:Node <ID_EQ>$param0}) }.
     */
    protected static final String ID_EQ = " {id: $";

    protected record AdminRealmsFilter(String filter, Set<String> dynRealmKeys, Set<String> groupOwners) {

    }

    protected record AnyCondQuery(String query, String field) {

    }

    protected record AttrCondQuery(String query, PlainSchema schema) {

    }

    protected record QueryInfo(
            TextStringBuilder query,
            Set<String> fields,
            Set<PlainSchema> plainSchemas,
            List<AttrCondQuery> membershipAttrConds) {

    }

    /**
     * Value plus the metadata ({@code fillAttrQuery} needs to decide how to render it in the generated query.
     *
     * @param value the literal value to render
     * @param isStr whether the value must be quoted as a string
     * @param lower whether the comparison must be case-insensitive
     */
    protected record ValueMeta(String value, boolean isStr, boolean lower) {

    }

    /**
     * The fields involved in a membership plain attribute lookup, plus the plain schemas they refer to.
     */
    protected record MembershipFieldSet(Set<String> fields, Set<PlainSchema> plainSchemas) {

    }

    protected static String setParameter(final Map<String, Object> parameters, final Object parameter) {
        String name = "param" + parameters.size();
        parameters.put(name, parameter);
        return name;
    }

    protected static void appendPlainAttrCond(
            final TextStringBuilder query, final PlainSchema schema, final String cond) {

        if (schema.isUniqueConstraint()) {
            query.append(schema.getKey()).append('.').append(key(schema.getType())).append(cond);
        } else {
            query.append("any(k IN ").append(schema.getKey()).
                    append(" WHERE k").append('.').append(key(schema.getType())).append(cond).
                    append(")");
        }
    }

    protected static String escapeIfString(final String value, final boolean isStr) {
        return isStr
                ? new StringBuilder().append('"').append(value).append('"').toString()
                : value;
    }

    protected final Neo4jTemplate neo4jTemplate;

    protected final Neo4jClient neo4jClient;

    // The parameter count is dictated by AbstractAnySearchDAO's constructor plus the two Neo4j-specific
    // dependencies below: it cannot be reduced further without changing the shared, backend-agnostic
    // AbstractAnySearchDAO contract used by every persistence implementation (JPA, Neo4j, ...).
    @SuppressWarnings("java:S107")
    public Neo4jAnySearchDAO(
            final RealmSearchDAO realmSearchDAO,
            final DynRealmDAO dynRealmDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final PlainAttrValidationManager validator,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient) {

        super(
                realmSearchDAO,
                dynRealmDAO,
                userDAO,
                groupDAO,
                anyObjectDAO,
                plainSchemaDAO,
                entityFactory,
                anyUtilsFactory,
                validator);
        this.neo4jTemplate = neo4jTemplate;
        this.neo4jClient = neo4jClient;
    }

    @Override
    protected boolean isPatternMatch(final String clause) {
        return clause.indexOf('*') != -1;
    }

    protected String buildAdminRealmsFilter(
            final Set<String> realmKeys,
            final boolean unrestricted,
            final Map<String, Object> parameters) {

        // An empty realmKeys set is ambiguous on its own: it can mean either "no restriction is needed here
        // because access is granted through another mechanism" (dynamic realms, group ownership - unrestricted)
        // or "no realm at all was found to be accessible" (must match nothing). The caller tells us which one
        // applies via the unrestricted flag.
        if (realmKeys.isEmpty() && unrestricted) {
            return "(n)-[]-(:" + Neo4jRealm.NODE + ")";
        }

        return "(n)-[]-(r:" + Neo4jRealm.NODE + ") WHERE r.id IN $" + setParameter(parameters, realmKeys);
    }

    /**
     * Resolves a single {@code adminRealms} entry (recursive case) into either a group-owner key, a set of
     * plain realm keys, or a dynamic realm key, mutating the relevant accumulator accordingly.
     */
    protected void collectRealmOrDynRealmKeys(
            final Realm base,
            final String realmPath,
            final Set<String> realmKeys,
            final Set<String> dynRealmKeys) {

        if (realmPath.startsWith("/")) {
            Realm realm = realmSearchDAO.findByFullPath(realmPath).orElseThrow(() -> {
                SyncopeClientException noRealm = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
                noRealm.getElements().add("Invalid realm specified: " + realmPath);
                return noRealm;
            });

            realmKeys.addAll(realmSearchDAO.findDescendants(realm.getFullPath(), base.getFullPath()));
        } else {
            dynRealmDAO.findById(realmPath).ifPresentOrElse(
                    dynRealm -> dynRealmKeys.add(dynRealm.getKey()),
                    () -> LOG.warn("Ignoring invalid dynamic realm {}", realmPath));
        }
    }

    protected AdminRealmsFilter getAdminRealmsFilter(
            final Realm base,
            final boolean recursive,
            final Set<String> adminRealms,
            final Map<String, Object> parameters) {

        Set<String> realmKeys = new HashSet<>();
        Set<String> dynRealmKeys = new HashSet<>();
        Set<String> groupOwners = new HashSet<>();

        if (recursive) {
            adminRealms.forEach(realmPath -> RealmUtils.GroupOwnerRealm.of(realmPath).ifPresentOrElse(
                    goRealm -> groupOwners.add(goRealm.groupKey()),
                    () -> collectRealmOrDynRealmKeys(base, realmPath, realmKeys, dynRealmKeys)));
            if (!dynRealmKeys.isEmpty()) {
                realmKeys.clear();
            }
        } else if (adminRealms.stream().anyMatch(base.getFullPath()::startsWith)) {
            // base is granted when one of the admin realms is base itself or one of its ancestors:
            // administering a realm implies administering everything below it.
            realmKeys.add(base.getKey());
        }

        // when access is (also) granted via dynamic realms or group ownership, those mechanisms are enforced
        // by extra OR-ed conditions elsewhere: the realm-based filter built here must not additionally restrict
        // the search in that case.
        boolean unrestricted = !dynRealmKeys.isEmpty() || !groupOwners.isEmpty();

        return new AdminRealmsFilter(
                buildAdminRealmsFilter(realmKeys, unrestricted, parameters), dynRealmKeys, groupOwners);
    }

    protected String getQuery(
            final AnyTypeCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        return matchNodeById(Neo4jAnyType.NODE, not, cond.getAnyTypeKey(), parameters);
    }

    protected String getQuery(
            final AuxClassCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        return matchNodeById(Neo4jAnyTypeClass.NODE, not, cond.getAuxClass(), parameters);
    }

    protected String getQuery(
            final DynRealmCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        return MATCH_N
                + WHERE + (not ? "NOT " : "") + "(n)-"
                + "[:" + DynRealmRepoExt.DYN_REALM_MEMBERSHIP_REL + "]-"
                + "(:" + Neo4jDynRealm.NODE + ID_EQ + setParameter(parameters, cond.getDynRealm()) + "}) ";
    }

    /**
     * Builds {@code MATCH (n) WHERE [NOT ](n)-[]-(:nodeLabel {id: $paramN}) }, the common shape shared by several
     * simple "n is linked to a single identified node" leaf conditions.
     */
    protected String matchNodeById(
            final String nodeLabel,
            final boolean not,
            final Object idValue,
            final Map<String, Object> parameters) {

        return MATCH_N
                + WHERE + (not ? "NOT " : "") + "(n)-[]-"
                + "(:" + nodeLabel + ID_EQ + setParameter(parameters, idValue) + "}) ";
    }

    protected String getQuery(
            final AnyTypeKind kind,
            final RelationshipTypeCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        String relTypeNode = kind == AnyTypeKind.ANY_OBJECT
                ? Neo4jARelationship.NODE
                : Neo4jURelationship.NODE;

        return MATCH_N
                + WHERE + (not ? "NOT " : "") + "EXISTS { MATCH (n)-[]-(r:" + relTypeNode + ")-[]-"
                + "(t:" + Neo4jRelationshipType.NODE
                + " {id: $ " + setParameter(parameters, cond.getRelationshipTypeKey()) + "}) } ";
    }

    protected String getQuery(
            final AnyTypeKind kind,
            final RelationshipCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        Set<String> rightAnyObjects = check(cond);

        String relTypeNode = kind == AnyTypeKind.ANY_OBJECT
                ? Neo4jARelationship.NODE
                : Neo4jURelationship.NODE;
        String destRelType = kind == AnyTypeKind.ANY_OBJECT
                ? Neo4jARelationship.DEST_REL
                : Neo4jURelationship.DEST_REL;

        return MATCH_N
                + "WHERE EXISTS { "
                + "MATCH(n)-[]-(:" + relTypeNode + ")-[:" + destRelType + "]-(anyObject:" + Neo4jAnyObject.NODE + ") "
                // NOT must negate the whole membership test, not just the IN operand: "id NOT IN $x" would
                // still match n as soon as ANY related anyObject falls outside $x, instead of requiring that
                // none of them falls inside it.
                + WHERE + (not ? "NOT " : "") + "anyObject.id IN $" + setParameter(parameters, rightAnyObjects)
                + " } ";
    }

    protected String getQuery(
            final MembershipCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        cond.setGroup(cond.getGroup().replace("%", ".*"));
        List<String> groupKeys = check(cond);

        String param = setParameter(parameters, groupKeys);
        return MATCH_N
                + WHERE + (not ? "NOT " : "") + "EXISTS { "
                + "MATCH (n)-[]-(:" + Neo4jUMembership.NODE + ")-[]-"
                + "(g:" + Neo4jGroup.NODE + ") WHERE g.id IN $" + param + " } "
                + (not ? "AND NOT" : "OR") + " EXISTS { "
                + "MATCH (n)-[:" + GroupRepoExt.DYN_GROUP_USER_MEMBERSHIP_REL + "]-"
                + "(g:" + Neo4jGroup.NODE + ") WHERE g.id IN $" + param + " } "
                + (not ? "AND NOT" : "OR") + " EXISTS { "
                + "MATCH (n)-[]-(:" + Neo4jAMembership.NODE + ")-[]-"
                + "(g:" + Neo4jGroup.NODE + ") WHERE g.id IN $" + param + " } "
                + (not ? "AND NOT" : "OR") + " EXISTS { "
                + "MATCH (n)-[:" + GroupRepoExt.DYN_GROUP_ANY_OBJECT_MEMBERSHIP_REL + "]-"
                + "(g:" + Neo4jGroup.NODE + ") WHERE g.id IN $" + param + " } ";
    }

    protected String getQuery(
            final MemberCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        Set<String> memberKeys = check(cond);

        String param = setParameter(parameters, memberKeys);
        return MATCH_N
                + WHERE + (not ? "NOT " : "") + "EXISTS { "
                + "MATCH (n)-[]-(:" + Neo4jUMembership.NODE + ")-[]-"
                + "(m:" + Neo4jUser.NODE + ") WHERE m.id IN $" + param + " } "
                + (not ? "AND NOT" : "OR") + " EXISTS { "
                + "MATCH (n)-[:" + GroupRepoExt.DYN_GROUP_USER_MEMBERSHIP_REL + "]-"
                + "(m:" + Neo4jUser.NODE + ") WHERE m.id IN $" + param + " }  "
                + (not ? "AND NOT" : "OR") + " EXISTS { "
                + "MATCH (n)-[]-(:" + Neo4jAMembership.NODE + ")-[]-"
                + "(m:" + Neo4jAnyObject.NODE + ") WHERE m.id IN $" + param + " } "
                + (not ? "AND NOT" : "OR") + " EXISTS { "
                + "MATCH (n)-[:" + GroupRepoExt.DYN_GROUP_ANY_OBJECT_MEMBERSHIP_REL + "]-"
                + "(m:" + Neo4jAnyObject.NODE + ") WHERE m.id IN $" + param + " } ";
    }

    protected String getQuery(
            final RoleCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        String param = setParameter(parameters, cond.getRole());
        return MATCH_N
                + WHERE + (not ? "NOT " : "")
                + "(n)-[:" + Neo4jUser.ROLE_MEMBERSHIP_REL + "]-"
                + "(:" + Neo4jRole.NODE + ID_EQ + param + "}) "
                + (not ? "AND NOT" : "OR") + " EXISTS { "
                + "MATCH (n)-[:" + RoleRepoExt.DYN_ROLE_MEMBERSHIP_REL + "]-"
                + "(:" + Neo4jRole.NODE + ID_EQ + param + "}) } ";
    }

    protected String getQuery(
            final AnyTypeKind kind,
            final ResourceCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        String param = setParameter(parameters, cond.getResource());
        TextStringBuilder query = new TextStringBuilder(MATCH_N).
                append(WHERE).
                append(not ? "NOT " : "").
                append("(n)-[]-(:").append(Neo4jExternalResource.NODE).append(ID_EQ).append(param).append("}) ");

        if (kind == AnyTypeKind.USER || kind == AnyTypeKind.ANY_OBJECT) {
            String membershipTypeNode = kind == AnyTypeKind.ANY_OBJECT
                    ? Neo4jAMembership.NODE
                    : Neo4jUMembership.NODE;

            query.append(not ? "AND NOT EXISTS { " : "OR EXISTS { ").
                    append("MATCH (n)-[]-(:").append(membershipTypeNode).append(")-[]-").
                    append("(g:").append(Neo4jGroup.NODE).append(") ").
                    append(WHERE).
                    append("(g)-[]-(:").append(Neo4jExternalResource.NODE).append(ID_EQ).append(param).append("})").
                    append(" } ");
        }

        return query.toString();
    }

    /**
     * Determines whether a raw expression can be treated as the plain schema's declared numeric/boolean type
     * (in which case it does not need to be quoted as a string in the generated query).
     */
    protected boolean parsesAsDeclaredType(final AttrSchemaType type, final String value) {
        try {
            switch (type) {
                case Long ->
                        Long.valueOf(value);

                case Double ->
                        Double.valueOf(value);

                case Boolean -> {
                    if (!("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value))) {
                        throw new IllegalArgumentException();
                    }
                }

                default -> {
                    // no numeric/boolean coercion applies to this schema type
                }
            }
            return true;
        } catch (RuntimeException ignored) {
            // value does not match the declared schema type: keep it quoted as a string
            return false;
        }
    }

    protected ValueMeta resolvePlainAttrValueMeta(
            final PlainSchema schema,
            final AttrCond cond,
            final PlainAttrValue attrValue) {

        String value = Optional.ofNullable(attrValue.getDateValue()).
                map(DateTimeFormatter.ISO_OFFSET_DATE_TIME::format).
                orElseGet(cond::getExpression);

        if (schema.getType().isStringClass()) {
            boolean lower = cond.getType() == AttrCond.Type.IEQ || cond.getType() == AttrCond.Type.ILIKE;
            return new ValueMeta(value, true, lower);
        }
        if (schema.getType() == AttrSchemaType.Date) {
            return new ValueMeta(value, true, false);
        }

        return new ValueMeta(value, !parsesAsDeclaredType(schema.getType(), value), false);
    }

    protected void negatePlainAttrClause(final TextStringBuilder query, final PlainSchema schema) {
        if (schema.isUniqueConstraint()) {
            query.replaceFirst("WHERE", "WHERE NOT(");
            query.append(')');
        } else {
            query.replaceAll("any(", schema.getKey() + " IS NULL OR none(");
        }
    }

    protected void appendPlainAttrClause(
            final TextStringBuilder query,
            final PlainSchema schema,
            final AttrCond cond,
            final ValueMeta meta) {

        switch (cond.getType()) {
            case ISNULL -> {
                // getQuery(AttrCond, boolean, Map) intercepts ISNULL before it ever reaches this switch;
                // kept here defensively for any other/future caller of this generic helper.
            }

            case ISNOTNULL ->
                    query.append(schema.getKey()).append(" IS NOT NULL");

            case ILIKE, LIKE -> {
                if (schema.getType().isStringClass()) {
                    appendPlainAttrCond(
                            query,
                            schema,
                            " =~ \"" + (meta.lower() ? "(?i)" : "")
                                    + AnyRepoExt.escapeForLikeRegex(meta.value()).replace("%", ".*") + '"');
                } else {
                    query.append(ALWAYS_FALSE_CLAUSE);
                    LOG.error("LIKE is only compatible with string or enum schemas");
                }
            }

            case IEQ, EQ -> {
                if (StringUtils.containsAny(meta.value(), AnyRepoExt.REGEX_CHARS) || meta.lower()) {
                    appendPlainAttrCond(
                            query,
                            schema,
                            " =~ \"^" + (meta.lower() ? "(?i)" : "")
                                    + AnyRepoExt.escapeForLikeRegex(meta.value()).replace("%", ".*") + "$\"");
                } else {
                    appendPlainAttrCond(query, schema, " = " + escapeIfString(meta.value(), meta.isStr()));
                }
            }

            case GE ->
                    appendPlainAttrCond(query, schema, " >= " + escapeIfString(meta.value(), meta.isStr()));

            case GT ->
                    appendPlainAttrCond(query, schema, " > " + escapeIfString(meta.value(), meta.isStr()));

            case LE ->
                    appendPlainAttrCond(query, schema, " <= " + escapeIfString(meta.value(), meta.isStr()));

            case LT ->
                    appendPlainAttrCond(query, schema, " < " + escapeIfString(meta.value(), meta.isStr()));

            default -> {
                // AttrCond.Type has no further values besides the ones handled above
            }
        }
    }

    protected void fillAttrQuery(
            final TextStringBuilder query,
            final PlainAttrValue attrValue,
            final PlainSchema schema,
            final AttrCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        if (not && cond.getType() == AttrCond.Type.ISNULL) {
            cond.setType(AttrCond.Type.ISNOTNULL);
            fillAttrQuery(query, attrValue, schema, cond, true, parameters);
            return;
        }
        if (not) {
            fillAttrQuery(query, attrValue, schema, cond, false, parameters);
            negatePlainAttrClause(query, schema);
            return;
        }

        ValueMeta meta = resolvePlainAttrValueMeta(schema, cond, attrValue);

        query.append(WHERE);
        appendPlainAttrClause(query, schema, cond, meta);
    }

    protected String loweredParam(final Map<String, Object> parameters, final boolean lower, final Object value) {
        String param = "$" + setParameter(parameters, value);
        return lower ? "toLower(" + param + ')' : param;
    }

    protected void appendAnyAttrClause(
            final TextStringBuilder query,
            final PlainSchema schema,
            final AnyCond cond,
            final PlainAttrValue attrValue,
            final String property,
            final boolean lower,
            final Map<String, Object> parameters) {

        switch (cond.getType()) {
            case ISNULL ->
                    query.append(property).append(" IS NULL");

            case ISNOTNULL ->
                    query.append(property).append(" IS NOT NULL");

            case ILIKE, LIKE -> {
                if (schema.getType().isStringClass()) {
                    query.append(property).append(" =~ ").
                            append(loweredParam(parameters, lower, cond.getExpression().replace("%", ".*")));
                } else {
                    query.append(' ').append(ALWAYS_FALSE_CLAUSE);
                    LOG.error("LIKE is only compatible with string or enum schemas");
                }
            }

            case IEQ, EQ ->
                    query.append(property).append('=').
                            append(loweredParam(parameters, lower, attrValue.getValue()));

            case GE ->
                    query.append(property).append(">=").
                            append('$').append(setParameter(parameters, attrValue.getValue()));

            case GT ->
                    query.append(property).append('>').
                            append('$').append(setParameter(parameters, attrValue.getValue()));

            case LE ->
                    query.append(property).append("<=").
                            append('$').append(setParameter(parameters, attrValue.getValue()));

            case LT ->
                    query.append(property).append('<').
                            append('$').append(setParameter(parameters, attrValue.getValue()));

            default -> {
                // AttrCond.Type has no further values besides the ones handled above
            }
        }
    }

    protected void fillAttrQuery(
            final TextStringBuilder query,
            final PlainAttrValue attrValue,
            final PlainSchema schema,
            final AnyCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        if (not && cond.getType() == AttrCond.Type.ISNULL) {
            cond.setType(AttrCond.Type.ISNOTNULL);
            fillAttrQuery(query, attrValue, schema, cond, true, parameters);
            return;
        }
        if (not) {
            query.append("NOT (");
            fillAttrQuery(query, attrValue, schema, cond, false, parameters);
            query.append(')');
            return;
        }

        boolean lower = schema.getType().isStringClass()
                && (cond.getType() == AttrCond.Type.IEQ || cond.getType() == AttrCond.Type.ILIKE);
        String property = lower ? "toLower (n." + cond.getSchema() + ')' : "n." + cond.getSchema();

        appendAnyAttrClause(query, schema, cond, attrValue, property, lower, parameters);
    }

    protected AnyCondQuery getQuery(
            final AnyTypeKind kind,
            final AnyCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        if (JAXRSService.PARAM_REALM.equals(cond.getSchema())) {
            if (!SyncopeConstants.UUID_PATTERN.matcher(cond.getExpression()).matches()) {
                Realm realm = realmSearchDAO.findByFullPath(cond.getExpression()).
                        orElseThrow(() -> new IllegalArgumentException(
                                "Invalid Realm full path: " + cond.getExpression()));
                cond.setExpression(realm.getKey());
            }

            return new AnyCondQuery(
                    "MATCH (n)-[]-"
                            + "(:" + Neo4jRealm.NODE + ID_EQ + setParameter(parameters, cond.getExpression()) + "}) ",
                    null);
        }

        CheckResult<AnyCond> checked = check(cond, kind);

        if (ArrayUtils.contains(
                RELATIONSHIP_FIELDS,
                StringUtils.substringBefore(checked.cond().getSchema(), "_id"))) {

            return getRelationshipFieldQuery(checked.cond(), parameters);
        }

        TextStringBuilder query = new TextStringBuilder(MATCH_N + WHERE);

        fillAttrQuery(query, checked.value(), checked.schema(), checked.cond(), not, parameters);

        return new AnyCondQuery(query.toString(), checked.cond().getSchema());
    }

    /**
     * Handles the {@code userOwner}/{@code groupOwner} pseudo-schemas, which resolve to a relationship rather
     * than to a plain attribute comparison.
     */
    protected AnyCondQuery getRelationshipFieldQuery(final AnyCond cond, final Map<String, Object> parameters) {
        String field = StringUtils.substringBefore(cond.getSchema(), "_id");
        return switch (field) {
            case "userOwner" ->
                    new AnyCondQuery(
                            "MATCH (n)-[:" + Neo4jGroup.USER_OWNER_REL + "]-"
                                    + "(:" + Neo4jUser.NODE + " "
                                    + ID_EQ + setParameter(parameters, cond.getExpression()) + "})",
                            null);

            case "groupOwner" ->
                    new AnyCondQuery(
                            "MATCH (n)-[:" + Neo4jGroup.GROUP_OWNER_REL + "]-"
                                    + "(:" + Neo4jGroup.NODE + " "
                                    + ID_EQ + setParameter(parameters, cond.getExpression()) + "})",
                            null);

            default ->
                    throw new IllegalArgumentException("Unsupported relationship: " + field);
        };
    }

    protected AttrCondQuery getQuery(
            final AttrCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        CheckResult<AttrCond> checked = check(cond);

        TextStringBuilder query = new TextStringBuilder(MATCH_N);
        switch (cond.getType()) {
            case ISNOTNULL ->
                    query.append(WHERE).append("n.`plainAttrs.").append(checked.schema().getKey()).
                            // NOT must be honoured here too: ISNOTNULL/ISNULL never reach fillAttrQuery from this
                            // call site, so their negation has to be applied directly.
                                    append(not ? "` IS NULL" : "` IS NOT NULL");

            case ISNULL ->
                    query.append(WHERE).append("n.`plainAttrs.").append(checked.schema().getKey()).
                            append(not ? "` IS NOT NULL" : "` IS NULL");

            default ->
                    fillAttrQuery(query, checked.value(), checked.schema(), cond, not, parameters);
        }

        return new AttrCondQuery(query.toString(), checked.schema());
    }

    protected void getQueryForCustomConds(
            final AnyTypeKind kind,
            final SearchCond cond,
            final Map<String, Object> parameters,
            final boolean not,
            final TextStringBuilder query) {

        // do nothing by default, leave it open for subclasses
    }

    protected void queryOp(
            final TextStringBuilder query,
            final String op,
            final QueryInfo leftInfo,
            final QueryInfo rightInfo) {

        query.append("WHERE EXISTS { ").
                append(Strings.CS.prependIfMissing(leftInfo.query().toString(), MATCH_N)).
                append(" } ").
                append(op).append(" EXISTS { ").
                append(Strings.CS.prependIfMissing(rightInfo.query().toString(), MATCH_N)).
                append(" }");
    }

    /**
     * Builds the query fragment (and collects the involved fields/plain schemas) for a single leaf
     * {@link SearchCond}.
     */
    protected QueryInfo getLeafQuery(
            final AnyTypeKind kind,
            final SearchCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        TextStringBuilder query = new TextStringBuilder();
        Set<String> involvedFields = new HashSet<>();
        Set<PlainSchema> involvedPlainSchemas = new HashSet<>();
        List<AttrCondQuery> membershipAttrConds = new ArrayList<>();

        cond.asLeaf(AnyTypeCond.class).
                filter(leaf -> AnyTypeKind.ANY_OBJECT == kind).
                ifPresent(leaf -> query.append(getQuery(leaf, not, parameters)));

        cond.asLeaf(AuxClassCond.class).
                ifPresent(leaf -> query.append(getQuery(leaf, not, parameters)));

        cond.asLeaf(RelationshipTypeCond.class).
                filter(leaf -> AnyTypeKind.GROUP != kind).
                ifPresent(leaf -> query.append(getQuery(kind, leaf, not, parameters)));

        cond.asLeaf(RelationshipCond.class).
                filter(leaf -> AnyTypeKind.GROUP != kind).
                ifPresent(leaf -> query.append(getQuery(kind, leaf, not, parameters)));

        cond.asLeaf(MembershipCond.class).
                filter(leaf -> AnyTypeKind.GROUP != kind).
                ifPresent(leaf -> query.append(getQuery(leaf, not, parameters)));

        cond.asLeaf(MemberCond.class).
                filter(leaf -> AnyTypeKind.GROUP == kind).
                ifPresent(leaf -> query.append(getQuery(leaf, not, parameters)));

        cond.asLeaf(RoleCond.class).
                filter(leaf -> AnyTypeKind.USER == kind).
                ifPresent(leaf -> query.append(getQuery(leaf, not, parameters)));

        cond.asLeaf(DynRealmCond.class).
                ifPresent(leaf -> query.append(getQuery(leaf, not, parameters)));

        cond.asLeaf(ResourceCond.class).
                ifPresent(leaf -> query.append(getQuery(kind, leaf, not, parameters)));

        cond.asLeaf(AnyCond.class).ifPresentOrElse(
                anyCond -> {
                    AnyCondQuery anyCondQuery = getQuery(kind, anyCond, not, parameters);
                    query.append(anyCondQuery.query());
                    Optional.ofNullable(anyCondQuery.field()).ifPresent(involvedFields::add);
                },
                () -> cond.asLeaf(AttrCond.class).ifPresent(leaf -> {
                    AttrCondQuery attrCondQuery = getQuery(leaf, not, parameters);
                    query.append(attrCondQuery.query());
                    involvedPlainSchemas.add(attrCondQuery.schema());
                    if (kind != AnyTypeKind.GROUP
                            && !not
                            && leaf.getType() != AttrCond.Type.ISNULL
                            && leaf.getType() != AttrCond.Type.ISNOTNULL) {

                        membershipAttrConds.add(attrCondQuery);
                    }
                }));

        // allow for additional search conditions
        getQueryForCustomConds(kind, cond, parameters, not, query);

        return new QueryInfo(query, involvedFields, involvedPlainSchemas, membershipAttrConds);
    }

    /**
     * Combines the {@link QueryInfo} of two branches ({@code AND}/{@code OR}) with the given Cypher operator.
     */
    protected QueryInfo combineQuery(
            final AnyTypeKind kind,
            final SearchCond cond,
            final Map<String, Object> parameters,
            final String op) {

        QueryInfo leftInfo = getQuery(kind, cond.getLeft(), parameters);
        QueryInfo rightInfo = getQuery(kind, cond.getRight(), parameters);

        Set<String> involvedFields = new HashSet<>(leftInfo.fields());
        involvedFields.addAll(rightInfo.fields());

        Set<PlainSchema> involvedPlainSchemas = new HashSet<>(leftInfo.plainSchemas());
        involvedPlainSchemas.addAll(rightInfo.plainSchemas());

        List<AttrCondQuery> membershipAttrConds = new ArrayList<>(leftInfo.membershipAttrConds());
        membershipAttrConds.addAll(rightInfo.membershipAttrConds());

        TextStringBuilder query = new TextStringBuilder();
        queryOp(query, op, leftInfo, rightInfo);

        return new QueryInfo(query, involvedFields, involvedPlainSchemas, membershipAttrConds);
    }

    protected QueryInfo getQuery(final AnyTypeKind kind, final SearchCond cond, final Map<String, Object> parameters) {
        switch (cond.getType()) {
            case LEAF, NOT_LEAF -> {
                return getLeafQuery(kind, cond, cond.getType() == SearchCond.Type.NOT_LEAF, parameters);
            }

            case AND -> {
                return combineQuery(kind, cond, parameters, "AND");
            }

            case OR -> {
                return combineQuery(kind, cond, parameters, "OR");
            }

            default -> {
                // SearchCond.Type currently only defines LEAF, NOT_LEAF, AND and OR: kept for forward
                // compatibility with any value that might be added in the future.
                return new QueryInfo(new TextStringBuilder(), new HashSet<>(), new HashSet<>(), new ArrayList<>());
            }
        }
    }

    protected void wrapQuery(
            final QueryInfo queryInfo,
            final Streamable<Order> orderBy,
            final AnyTypeKind kind,
            final String adminRealmsFilter) {

        TextStringBuilder match = new TextStringBuilder("MATCH (n:").append(AnyRepoExt.node(kind)).append(") ").
                append("WITH n.id AS id");

        // take fields into account
        AnyUtils anyUtils = anyUtilsFactory.getInstance(kind);
        queryInfo.fields().remove("id");
        Stream.concat(
                        queryInfo.fields().stream(),
                        orderBy.stream().filter(clause -> !"id".equals(clause.getProperty())
                                && anyUtils.getField(clause.getProperty()).isPresent()).map(Order::getProperty)).
                distinct().forEach(field -> match.append(", n.").append(field).append(" AS ").append(field));

        // take plain schemas into account
        Stream.concat(
                queryInfo.plainSchemas().stream(),
                orderBy.stream().map(clause -> plainSchemaDAO.findById(clause.getProperty())).
                        flatMap(Optional::stream)).distinct().forEach(schema -> {

            match.append(", apoc.convert.getJsonProperty(n, 'plainAttrs.").append(schema.getKey());
            if (schema.isUniqueConstraint()) {
                match.append("', '$.uniqueValue')");
            } else {
                match.append("', '$.values')");
            }
            match.append(" AS ").append(schema.getKey());
        });

        TextStringBuilder query = queryInfo.query();

        // take realms into account
        if (query.startsWith("MATCH (n)")) {
            query.replaceFirst("MATCH (n)", match + " WHERE (EXISTS { MATCH (n)");
            query.append("} ");
        } else {
            query.replaceFirst("WHERE EXISTS", "WHERE (EXISTS");
            query.insert(0, match.append(' '));
        }
        query.append(") AND EXISTS { ").append(adminRealmsFilter).append(" } ");
    }

    protected MembershipFieldSet resolveMembershipFields(
            final QueryInfo queryInfo,
            final Set<String> orderByItems,
            final AnyTypeKind kind) {

        AnyUtils anyUtils = anyUtilsFactory.getInstance(kind);
        Set<String> fields = Stream.concat(
                        queryInfo.fields().stream().filter(f -> !"id".equals(f)),
                        orderByItems.stream().filter(item -> !"id".equals(item) && anyUtils.getField(item).isPresent())).
                collect(Collectors.toSet());

        Set<PlainSchema> plainSchemas = Stream.concat(
                        queryInfo.membershipAttrConds().stream().map(AttrCondQuery::schema),
                        orderByItems.stream().map(plainSchemaDAO::findById).flatMap(Optional::stream)).
                collect(Collectors.toSet());

        return new MembershipFieldSet(fields, plainSchemas);
    }

    protected TextStringBuilder buildMembershipReturnClause(final MembershipFieldSet fieldSet) {
        TextStringBuilder returnStmt = new TextStringBuilder("RETURN id");
        fieldSet.fields().forEach(f -> returnStmt.append(", ").append(f));
        fieldSet.plainSchemas().forEach(schema -> returnStmt.append(", ").append(schema.getKey()));
        return returnStmt;
    }

    protected void appendMembershipPlainSchemaProjection(final TextStringBuilder query, final PlainSchema schema) {
        query.append(", apoc.convert.getJsonProperty(n, 'plainAttrs.").append(schema.getKey()).
                append(schema.isUniqueConstraint() ? "', '$.uniqueValue')" : "', '$.values')").
                append(" AS ").append(schema.getKey());
    }

    protected void appendMembershipUnionClause(
            final TextStringBuilder query,
            final MembershipFieldSet fieldSet,
            final AnyTypeKind kind) {

        query.append(" UNION ").
                append("MATCH (n:").append(AnyRepoExt.membNode(kind)).
                append(")-[]-(m:").append(AnyRepoExt.node(kind)).append(") ").
                append("WITH m.id AS id ");

        fieldSet.fields().forEach(f -> query.append(", m.").append(f).append(" AS ").append(f));

        fieldSet.plainSchemas().forEach(schema -> appendMembershipPlainSchemaProjection(query, schema));
    }

    protected void membershipAttrConds(
            final TextStringBuilder query,
            final QueryInfo queryInfo,
            final List<String> orderBy,
            final AnyTypeKind kind) {

        if (kind == AnyTypeKind.GROUP || queryInfo.membershipAttrConds().isEmpty()) {
            return;
        }

        Set<String> orderByItems = orderBy.stream().
                map(clause -> StringUtils.substringBefore(clause, " ")).
                collect(Collectors.toSet());

        MembershipFieldSet fieldSet = resolveMembershipFields(queryInfo, orderByItems, kind);

        query.insert(0, "CALL () { ");

        TextStringBuilder returnStmt = buildMembershipReturnClause(fieldSet);
        query.append(returnStmt);

        appendMembershipUnionClause(query, fieldSet, kind);

        query.append(WHERE).
                append(queryInfo.membershipAttrConds().stream().
                        map(mac -> "(EXISTS { " + mac.query() + "} )").
                        collect(Collectors.joining(" AND "))).
                append(" AND EXISTS { (m)-[]-(r:Realm) WHERE r.id IN $param0 } ").
                append(returnStmt).
                append(" } ");
    }

    @Override
    protected long doCount(
            final Realm base,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final AnyTypeKind kind) {

        Map<String, Object> parameters = new HashMap<>();

        AdminRealmsFilter filter = getAdminRealmsFilter(base, recursive, adminRealms, parameters);

        // 1. get the query string from the search condition
        QueryInfo queryInfo = getQuery(
                kind, buildEffectiveCond(cond, filter.dynRealmKeys(), filter.groupOwners(), kind), parameters);

        // 2. wrap query
        wrapQuery(queryInfo, Streamable.empty(), kind, filter.filter());
        TextStringBuilder query = queryInfo.query();

        // 3. include membership plain attr queries
        membershipAttrConds(query, queryInfo, List.of(), kind);

        // 4. prepare the count query
        query.append("RETURN COUNT(id)");

        return neo4jTemplate.count(query.toString(), parameters);
    }

    protected List<String> parseOrderBy(
            final AnyTypeKind kind,
            final Streamable<Sort.Order> orderBy) {

        AnyUtils anyUtils = anyUtilsFactory.getInstance(kind);

        List<String> clauses = new ArrayList<>();

        Set<String> orderByUniquePlainSchemas = new HashSet<>();
        Set<String> orderByNonUniquePlainSchemas = new HashSet<>();
        orderBy.forEach(clause -> {
            if (anyUtils.getField(clause.getProperty()).isPresent()) {
                clauses.add(clause.getProperty() + " " + clause.getDirection().name());
            } else {
                plainSchemaDAO.findById(clause.getProperty()).ifPresent(schema -> {
                    if (schema.isUniqueConstraint()) {
                        orderByUniquePlainSchemas.add(schema.getKey());
                    } else {
                        orderByNonUniquePlainSchemas.add(schema.getKey());
                    }
                    if (orderByUniquePlainSchemas.size() > 1 || orderByNonUniquePlainSchemas.size() > 1) {
                        SyncopeClientException invalidSearch =
                                SyncopeClientException.build(ClientExceptionType.InvalidSearchParameters);
                        invalidSearch.getElements().add("Order by more than one attribute is not allowed; "
                                + "remove one from " + (orderByUniquePlainSchemas.size() > 1
                                ? orderByUniquePlainSchemas : orderByNonUniquePlainSchemas));
                        throw invalidSearch;
                    }

                    clauses.add(schema.getKey() + " " + clause.getDirection().name());
                });
            }
        });

        return clauses;
    }

    @Override
    protected <T extends Any> List<T> doSearch(
            final Realm base,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final Pageable pageable,
            final AnyTypeKind kind) {

        Map<String, Object> parameters = new HashMap<>();

        AdminRealmsFilter filter = getAdminRealmsFilter(base, recursive, adminRealms, parameters);

        // 1. get the query string from the search condition
        QueryInfo queryInfo = getQuery(
                kind, buildEffectiveCond(cond, filter.dynRealmKeys(), filter.groupOwners(), kind), parameters);

        // 2. wrap query
        wrapQuery(queryInfo, pageable.getSort(), kind, filter.filter());
        TextStringBuilder query = queryInfo.query();

        List<String> orderBy = parseOrderBy(kind, pageable.getSort());
        String orderByStmt = String.join(", ", orderBy);

        // 3. include membership plain attr queries
        membershipAttrConds(query, queryInfo, orderBy, kind);

        // 4. prepare the search query
        query.append("RETURN id ").
                append("ORDER BY ").append(orderByStmt);

        if (pageable.isPaged()) {
            query.append(" SKIP ").append(pageable.getPageSize() * pageable.getPageNumber()).
                    append(" LIMIT ").append(pageable.getPageSize());
        }

        LOG.debug("Query with auth and order by statements: {}, parameters: {}", query, parameters);

        // 5. Prepare the result (avoiding duplicates)
        return buildResult(neo4jClient.query(query.toString()).bindAll(parameters).fetch().all().stream().
                map(found -> found.get("id")).toList(), kind);
    }
}
