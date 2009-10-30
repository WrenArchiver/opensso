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
 * $Id: DelegationSummary.java,v 1.2 2009-10-30 17:33:54 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import javax.faces.event.ActionEvent;

public abstract class DelegationSummary extends Summary {

    private DelegationWizardBean delegationWizardBean;

    public DelegationSummary(DelegationWizardBean delegationWizardBean) {
        this.delegationWizardBean = delegationWizardBean;
    }

    public abstract String getLabel();

    public abstract String getValue();

    public String getTemplate() {
        return null;
    }

    public abstract String getIcon();

    public abstract boolean isExpandable();

    public DelegationWizardBean getDelegationWizardBean() {
        return delegationWizardBean;
    }

    protected DelegationWizardStep getGotoStep(ActionEvent event) {
        Object o = event.getComponent().getAttributes().get("gotoStep");
        Integer i = (Integer)o;
        DelegationWizardStep dws = DelegationWizardStep.valueOf(i.intValue());

        return dws;
    }

    public void editListener(ActionEvent event) {
        DelegationWizardStep gotoStep = getGotoStep(event);
        delegationWizardBean.gotoStep(gotoStep.toInt());
    }

    public int getTabIndex() {
        return -1;
    }
}
