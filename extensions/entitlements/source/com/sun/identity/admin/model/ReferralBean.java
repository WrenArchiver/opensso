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
 * $Id: ReferralBean.java,v 1.5 2009-06-05 21:29:00 farble1670 Exp $
 */
package com.sun.identity.admin.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.faces.model.SelectItem;

public class ReferralBean {

    private String name;
    private String description;
    private List<Resource> resources;
    private List<RealmBean> realmBeans = new ArrayList<RealmBean>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public List<SelectItem> getRealmBeanItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        if (realmBeans != null) {
            for (RealmBean rb : realmBeans) {
                items.add(new SelectItem(rb, rb.getTitle()));
            }
        }

        return items;
    }

    public List<RealmBean> getRealmBeans() {
        return realmBeans;
    }

    public void setRealmBeans(List<RealmBean> realmBeans) {
        this.realmBeans = realmBeans;
    }

    public String getListToString(List list) {
        StringBuffer b = new StringBuffer();

        for (Iterator<Resource> i = list.iterator(); i.hasNext();) {
            b.append(i.next());
            if (i.hasNext()) {
                b.append(",");
            }

        }

        return b.toString();
    }

    public String getResourcesToString() {
        return getListToString(resources);
    }

    public String getSubjectsToString() {
        return getListToString(realmBeans);
    }

    public String getListToFormattedString(List list) {
        StringBuffer b = new StringBuffer();

        for (Iterator i = list.iterator(); i.hasNext();) {
            b.append(i.next());
            if (i.hasNext()) {
                b.append("\n");
            }

        }

        return b.toString();
    }

    public String getResourcesToFormattedString() {
        StringBuffer b = new StringBuffer();
        for (Iterator<Resource> i = resources.iterator(); i.hasNext();) {
            ReferralResource rr = (ReferralResource)i.next();
            List<Resource> rs = rr.getViewEntitlement().getResources();

            b.append(rr.getTitle());
            b.append("\n");
            if (rs != null) {
                for (Resource r: rs) {
                    b.append("    ");
                    b.append(r.getTitle());
                    b.append("\n");
                }
            }
        }

        return b.toString();
    }

    public String getSubjectsToFormattedString() {
        return getListToFormattedString(realmBeans);
    }
}
