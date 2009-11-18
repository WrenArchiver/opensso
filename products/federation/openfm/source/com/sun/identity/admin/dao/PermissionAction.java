/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: PermissionAction.java,v 1.3 2009-11-18 17:14:30 farble1670 Exp $
 */
package com.sun.identity.admin.dao;

import com.sun.identity.admin.model.AccessLevel;
import com.sun.identity.admin.model.Permission;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PermissionAction {

    private static final Pattern LINE_PATTERN = Pattern.compile("^(.*)=(.*)/(.*)/(.*)/(.*)/(.*)$");
    private String service;
    private String version;
    private String type;
    private String subconfig;
    private Permission permission;
    private AccessLevel accessLevel;

    public PermissionAction(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line.trim());
        if (!matcher.matches()) {
            throw new AssertionError("invalid permission line: " + line);
        }
        String ps = matcher.group(1);
        permission = Permission.valueOf(ps);
        if (permission == null) {
            throw new AssertionError("no permission value for: " + ps);
        }

        service = matcher.group(2);
        version = matcher.group(3);
        type = matcher.group(4);
        subconfig = matcher.group(5);

        String as = matcher.group(6);
        accessLevel = AccessLevel.valueOf(as);
        if (accessLevel == null) {
            throw new AssertionError("no access level value for: " + as);
        }
    }

    public String getVersion() {
        return version;
    }

    public String getType() {
        return type;
    }

    public String getSubconfig() {
        return subconfig;
    }

    public Permission getPermission() {
        return permission;
    }

    public String getService() {
        return service;
    }

    public DelegationPermission toDelegationPermission() throws DelegationException {
        DelegationPermission dp = new DelegationPermission();

        dp.setServiceName(service);
        dp.setConfigType(type);
        dp.setSubConfigName(subconfig);
        Set<String> actions = Collections.singleton(accessLevel.toString());
        dp.setActions(actions);

        return dp;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }
}
