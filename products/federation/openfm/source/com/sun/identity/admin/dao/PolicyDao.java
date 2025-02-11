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
 * $Id: PolicyDao.java,v 1.7 2009-11-19 01:02:04 veiming Exp $
 */
package com.sun.identity.admin.dao;

import com.iplanet.sso.SSOToken;
import com.sun.identity.admin.ManagedBeanResolver;
import com.sun.identity.admin.Token;
import com.sun.identity.admin.model.FilterHolder;
import com.sun.identity.admin.model.PrivilegeBean;
import com.sun.identity.admin.model.RealmBean;
import com.sun.identity.admin.model.RealmsBean;
import com.sun.identity.admin.model.SubjectFactory;
import com.sun.identity.admin.model.ViewApplicationsBean;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationPrivilege.Action;
import com.sun.identity.entitlement.ApplicationPrivilegeManager;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.util.SearchFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

public class PolicyDao implements Serializable {

    private ViewApplicationsBean viewApplicationsBean;
    private int timeout;
    private int limit;
    private boolean limited = false;

    private String getPattern(String filter) {
        String pattern;
        if (filter == null || filter.length() == 0) {
            pattern = "*";
        } else {
            pattern = "*" + filter + "*";
        }

        return pattern;
    }

    public List<PrivilegeBean> getPrivilegeBeans() {
        return getPrivilegeBeans(null, Collections.EMPTY_LIST);
    }

    private Set<SearchFilter> getSearchFilters(List<FilterHolder> filterHolders) {
        Set<SearchFilter> sfs = new HashSet<SearchFilter>();

        for (FilterHolder fh : filterHolders) {
            List<SearchFilter> l = fh.getViewFilter().getSearchFilters();
            if (l != null) {
                // TODO: list should never be null
                sfs.addAll(l);
            }
        }

        return sfs;
    }

    public PrivilegeBean getPrivilegeBean(String privilegeName) {
        PrivilegeManager pm = getPrivilegeManager();
        ApplicationPrivilegeManager apm = getApplicationPrivilegeManager();
        SubjectFactory subjectFactory = SubjectFactory.getInstance();

        try {
            Privilege p = pm.getPrivilege(privilegeName);
            boolean writable = apm.hasPrivilege(p, Action.MODIFY);
            PrivilegeBean pb = new PrivilegeBean(
                    p,
                    writable,
                    viewApplicationsBean.getViewApplications(),
                    subjectFactory);
            return pb;
        } catch (EntitlementException ee) {
            throw new RuntimeException(ee);
        }
    }

    public List<PrivilegeBean> getPrivilegeBeans(String filter, List<FilterHolder> filterHolders) {
        Set<SearchFilter> psfs = getSearchFilters(filterHolders);
        String pattern = getPattern(filter);
        psfs.add(new SearchFilter(Privilege.NAME_ATTRIBUTE, pattern));

        PrivilegeManager pm = getPrivilegeManager();
        ApplicationPrivilegeManager apm = getApplicationPrivilegeManager();
        List<PrivilegeBean> privilegeBeans = null;
        SubjectFactory subjectFactory = SubjectFactory.getInstance();

        try {
            Set<String> privilegeNames;
            privilegeNames = pm.searchPrivilegeNames(psfs, limit, timeout);
            if (privilegeNames.size() >= limit) {
                limited = true;
            } else {
                limited = false;
            }

            privilegeBeans = new ArrayList<PrivilegeBean>();
            for (String privilegeName : privilegeNames) {
                Privilege p = pm.getPrivilege(privilegeName);
                boolean writable = apm.hasPrivilege(p, Action.MODIFY);
                PrivilegeBean pb = new PrivilegeBean(
                        p,
                        writable,
                        viewApplicationsBean.getViewApplications(),
                        subjectFactory);
                privilegeBeans.add(pb);
            }
        } catch (EntitlementException ee) {
            throw new RuntimeException(ee);
        }

        return privilegeBeans;

    }

    public List<String> getPrivilegeNames() {
        return getPrivilegeNames(null, Collections.EMPTY_LIST);
    }

    public List<String> getPrivilegeNames(String filter, List<FilterHolder> policyFilterHolders) {
        Set<SearchFilter> psfs = getSearchFilters(policyFilterHolders);
        String pattern = getPattern(filter);
        psfs.add(new SearchFilter(Privilege.NAME_ATTRIBUTE, pattern));
        PrivilegeManager pm = getPrivilegeManager();
        Set<String> privilegeNames;
        try {
            privilegeNames = pm.searchPrivilegeNames(psfs, limit, timeout);
        } catch (EntitlementException ee) {
            throw new RuntimeException(ee);
        }

        return new ArrayList<String>(privilegeNames);
    }

    private PrivilegeManager getPrivilegeManager() {
        SSOToken t = new Token().getSSOToken();
        Subject s = SubjectUtils.createSubject(t);
        RealmBean realmBean = RealmsBean.getInstance().getRealmBean();
        PrivilegeManager pm = PrivilegeManager.getInstance(realmBean.getName(), s);

        return pm;
    }

    private ApplicationPrivilegeManager getApplicationPrivilegeManager() {
        SSOToken t = new Token().getSSOToken();
        Subject s = SubjectUtils.createSubject(t);
        RealmBean realmBean = RealmsBean.getInstance().getRealmBean();
        ApplicationPrivilegeManager apm = ApplicationPrivilegeManager.getInstance(realmBean.getName(), s);

        return apm;
    }

    public void removePrivilege(String name) {
        PrivilegeManager pm = getPrivilegeManager();

        try {
            pm.removePrivilege(name);
        } catch (EntitlementException ee) {
            throw new RuntimeException(ee);
        }
    }

    public boolean privilegeExists(Privilege p) {
        return privilegeExists(p.getName());
    }

    public boolean privilegeExists(PrivilegeBean pb) {
        return privilegeExists(pb.getName());
    }

    public boolean privilegeExists(String name) {
        PrivilegeManager pm = getPrivilegeManager();

        try {
            return (pm.getPrivilege(name) != null);
        } catch (EntitlementException ee) {
            return false;
        }
    }

    public void setPrivilege(Privilege p) {
        validateAction(p.getEntitlement());
        if (privilegeExists(p)) {
            modifyPrivilege(p);
        } else {
            addPrivilege(p);
        }
    }

    public void addPrivilege(Privilege p) {
        PrivilegeManager pm = getPrivilegeManager();

        try {
            pm.addPrivilege(p);
        } catch (EntitlementException ee) {
            throw new RuntimeException(ee);
        }
    }

    private void validateAction(Entitlement e) {
        Subject adminSubject = new Token().getAdminSubject();

        RealmBean realmBean = RealmsBean.getInstance().getRealmBean();
        try {
            Application app = e.getApplication(adminSubject, realmBean.getName());
            Set<String> validActionName = app.getActions().keySet();

            Map<String, Boolean> actionValues = e.getActionValues();

            for (String actionName : actionValues.keySet()) {
                if (!validActionName.contains(actionName)) {
                    try {
                        app.addAction(actionName, actionValues.get(actionName));
                    } catch (EntitlementException ee) {
                        throw new RuntimeException(ee);
                    }
                }
            }
        } catch (EntitlementException ee) {
            throw new RuntimeException(ee);
        } 
    }

    public void modifyPrivilege(Privilege p) {
        PrivilegeManager pm = getPrivilegeManager();

        try {
            pm.modifyPrivilege(p);
        } catch (EntitlementException ee) {
            throw new RuntimeException(ee);
        }
    }

    public void setViewApplicationsBean(ViewApplicationsBean viewApplicationsBean) {
        this.viewApplicationsBean = viewApplicationsBean;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public boolean isLimited() {
        return limited;
    }

    public static PolicyDao getInstance() {
        ManagedBeanResolver mbr = new ManagedBeanResolver();
        PolicyDao pdao = (PolicyDao) mbr.resolve("policyDao");
        return pdao;
    }
}
