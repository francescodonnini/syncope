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

public final class SearchCondBuilder {
    private SearchCondBuilder() {
    }

    public static SearchCond anyType(final String anyTypeKey) {
        AnyTypeCond cond = new AnyTypeCond();
        cond.setAnyTypeKey(anyTypeKey);
        return SearchCond.of(cond);
    }

    public static SearchCond attr(final String schema, final AttrCond.Type type) {
        AttrCond cond = new AttrCond();
        cond.setSchema(schema);
        cond.setType(type);
        return SearchCond.of(cond);
    }

    public static SearchCond attr(final String schema, final AttrCond.Type type, final String expression) {
        AttrCond cond = new AttrCond();
        cond.setSchema(schema);
        cond.setType(type);
        cond.setExpression(expression);
        return SearchCond.of(cond);
    }

    public static SearchCond attrEq(final String schema, final String expression) {
        AttrCond cond = new AttrCond(AttrCond.Type.EQ);
        cond.setSchema(schema);
        cond.setExpression(expression);
        return SearchCond.of(cond);
    }

    public static SearchCond attrLike(final String schema, final String expression) {
        AttrCond cond = new AttrCond(AttrCond.Type.LIKE);
        cond.setSchema(schema);
        cond.setExpression(expression);
        return SearchCond.of(cond);
    }

    public static SearchCond attrIsNull(final String schema) {
        AttrCond cond = new AttrCond(AttrCond.Type.ISNULL);
        cond.setSchema(schema);
        return SearchCond.of(cond);
    }

    public static SearchCond attrNotNull(final String schema) {
        AttrCond cond = new AttrCond(AttrCond.Type.ISNOTNULL);
        cond.setSchema(schema);
        return SearchCond.of(cond);
    }

    public static SearchCond auxClass(final String auxClass) {
        AuxClassCond cond = new AuxClassCond();
        cond.setAuxClass(auxClass);
        return SearchCond.of(cond);
    }

    public static SearchCond dynRealm(final String dynRealm) {
        DynRealmCond cond = new DynRealmCond();
        cond.setDynRealm(dynRealm);
        return SearchCond.of(cond);
    }

    public static SearchCond membership(final String groupKey) {
        MembershipCond cond = new MembershipCond();
        cond.setGroup(groupKey);
        return SearchCond.of(cond);
    }

    public static SearchCond member(final String memberKey) {
        MemberCond cond = new MemberCond();
        cond.setMember(memberKey);
        return SearchCond.of(cond);
    }

    public static SearchCond relationship(final String anyObjectKey) {
        RelationshipCond cond = new RelationshipCond();
        cond.setAnyObject(anyObjectKey);
        return SearchCond.of(cond);
    }

    public static SearchCond relationshipType(final String typeKey) {
        RelationshipTypeCond cond = new RelationshipTypeCond();
        cond.setRelationshipType(typeKey);
        return SearchCond.of(cond);
    }

    public static SearchCond resource(final String resourceKey) {
        ResourceCond cond = new ResourceCond();
        cond.setResource(resourceKey);
        return SearchCond.of(cond);
    }

    public static SearchCond role(final String roleKey) {
        RoleCond cond = new RoleCond();
        cond.setRole(roleKey);
        return SearchCond.of(cond);
    }

    public static SearchCond any(final String schema, final AttrCond.Type type, final String expression) {
        AnyCond cond = new AnyCond();
        cond.setType(type);
        cond.setSchema(schema);
        cond.setExpression(expression);
        return SearchCond.of(cond);
    }

    public static SearchCond any(final String schema, final AttrCond.Type type) {
        AnyCond cond = new AnyCond();
        cond.setType(type);
        cond.setSchema(schema);
        return SearchCond.of(cond);
    }

    public static SearchCond anyEq(final String schema, final String expression) {
        AnyCond cond = new AnyCond(AttrCond.Type.EQ);
        cond.setSchema(schema);
        cond.setExpression(expression);
        return SearchCond.of(cond);
    }

    public static SearchCond and(final SearchCond left, final SearchCond right) {
        return SearchCond.and(left, right);
    }

    public static SearchCond or(final SearchCond left, final SearchCond right) {
        return SearchCond.or(left, right);
    }

    public static SearchCond not(final SearchCond cond) {
        return SearchCond.negate(cond);
    }
}
