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
 * $Id: PermissionsBean.java,v 1.1 2009-06-08 18:06:12 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.ManagedBeanResolver;
import com.sun.identity.admin.dao.PermissionDao;
import java.io.Serializable;
import java.util.List;
import static com.sun.identity.admin.model.Permission.*;

public class PermissionsBean implements Serializable {
    private List<Permission> permissions;
    private PermissionDao permissionDao;

    public boolean isAllowed(Permission p) {
        boolean allowed = permissions.contains(p);
        return allowed;
    }

    public boolean isViewAllowed(ViewId vid) {
        if (vid == null) {
            return false;
        }
        Permission p = vid.getPermission();
        return isAllowed(p);
    }

    public boolean isViewAllowed(String viewId) {
        ViewId vid = ViewId.valueOfId(viewId);
        return isViewAllowed(vid);
    }
    
    public boolean isActionAllowed(String action) {
        FromAction fa = FromAction.valueOfAction(action);
        if (fa == null) {
            return false;
        }
        Permission p = fa.toPermission();
        return isAllowed(p);
    }

    public static PermissionsBean getInstance() {
        ManagedBeanResolver mbr = new ManagedBeanResolver();
        PermissionsBean psb = (PermissionsBean)mbr.resolve("permissionsBean");
        return psb;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissionDao(PermissionDao permissionDao) {
        this.permissionDao = permissionDao;
        permissions = permissionDao.getPermissions();
    }

    public boolean isPolicyAllowed() {
        return isAllowed(POLICY);
    }

    public boolean isPolicyCreateAllowed() {
        return isAllowed(POLICY_CREATE);
    }

    public boolean isPolicyManageAllowed() {
        return isAllowed(POLICY_MANAGE);
    }

    public boolean isPolicyEditAllowed() {
        return isAllowed(POLICY_EDIT);
    }

    public boolean isReferralCreateAllowed() {
        return isAllowed(REFERRAL_CREATE);
    }
}
