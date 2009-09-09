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
 * $Id: ApplicationManageTableBean.java,v 1.1 2009-09-09 17:13:43 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationManageTableBean implements Serializable {

    public int getCellWidth() {
        return cellWidth;
    }

    public List<String> getColumnsVisible() {
        return columnsVisible;
    }

    public void setColumnsVisible(List<String> columnsVisible) {
        this.columnsVisible = columnsVisible;
    }

    public void setPrivilegeBeans(List<ViewApplication> viewApplications) {
        this.setViewApplications(viewApplications);
        sort();
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    private TableSortKey sortKey = new TableSortKey("name");
    private List<ViewApplication> viewApplications;
    private static Map<TableSortKey,Comparator> comparators = new HashMap<TableSortKey,Comparator>();
    private int cellWidth = 20;
    private List<String> columnsVisible = new ArrayList<String>();
    private int rows = 10;

    static {
        comparators.put(new TableSortKey("name", true), new ViewApplication.NameComparator(true));
        comparators.put(new TableSortKey("name", false), new ViewApplication.NameComparator(false));
    }

    public ApplicationManageTableBean() {
        // TODO: add columns visible
    }

    public TableSortKey getSortKey() {
        return sortKey;
    }

    public void setSortKey(TableSortKey sortKey) {
        this.sortKey = sortKey;
    }

    public void sort() {
        Comparator c = comparators.get(sortKey);
        Collections.sort(viewApplications, c);
    }

    public void setViewApplications(List<ViewApplication> viewApplications) {
        this.viewApplications = viewApplications;
    }
}
