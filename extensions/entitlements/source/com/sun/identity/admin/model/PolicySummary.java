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
 * $Id: PolicySummary.java,v 1.3 2009-06-04 11:49:16 veiming Exp $
 */

package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import com.icesoft.faces.context.effects.SlideDown;
import com.icesoft.faces.context.effects.SlideUp;
import com.sun.identity.admin.handler.MultiPanelHandler;
import java.io.Serializable;
import javax.faces.event.ActionEvent;

public abstract class PolicySummary
        implements Serializable, MultiPanelBean, MultiPanelHandler {

    private PolicyWizardBean policyWizardBean;
    private Effect panelExpandEffect;
    private Effect panelEffect;
    private boolean panelExpanded = false;
    private boolean panelVisible = true;

    public PolicySummary(PolicyWizardBean policyWizardBean) {
        this.policyWizardBean = policyWizardBean;
    }

    public abstract String getLabel();

    public abstract String getValue();

    public abstract String getTemplate();

    public abstract String getIcon();

    public abstract boolean isExpandable();

    public abstract PolicyWizardStep getGotoStep();

    public PolicyWizardAdvancedTabIndex getAdvancedTabIndex() {
        return PolicyWizardAdvancedTabIndex.ACTIONS;
    }

    public void panelExpandListener(ActionEvent event) {
        Effect e;
        if (isPanelExpanded()) {
            e = new SlideUp();
        } else {
            e = new SlideDown();
        }
        e.setSubmit(true);
        e.setTransitory(false);
        setPanelExpandEffect(e);
    }

    public void panelRemoveListener(ActionEvent event) {
        // nothing
    }

    public Effect getPanelExpandEffect() {
        return panelExpandEffect;
    }

    public void setPanelExpandEffect(Effect panelExpandEffect) {
        this.panelExpandEffect = panelExpandEffect;
    }

    public Effect getPanelEffect() {
        return panelEffect;
    }

    public void setPanelEffect(Effect panelEffect) {
        this.panelEffect = panelEffect;
    }

    public boolean isPanelExpanded() {
        return panelExpanded;
    }

    public void setPanelExpanded(boolean panelExpanded) {
        this.panelExpanded = panelExpanded;
    }

    public boolean isPanelVisible() {
        return panelVisible;
    }

    public void setPanelVisible(boolean panelVisible) {
        this.panelVisible = panelVisible;
    }

    public PolicyWizardBean getPolicyWizardBean() {
        return policyWizardBean;
    }

    protected PolicyWizardStep getGotoStep(ActionEvent event) {
        Object o = event.getComponent().getAttributes().get("gotoStep");
        PolicyWizardStep pws = (PolicyWizardStep) o;

        return pws;
    }

    protected PolicyWizardAdvancedTabIndex getGotoAdvancedTabIndex(ActionEvent event) {
        Object o = event.getComponent().getAttributes().get("gotoAdvancedTabIndex");
        PolicyWizardAdvancedTabIndex index = (PolicyWizardAdvancedTabIndex) o;

        return index;
    }

    public void editListener(ActionEvent event) {
        PolicyWizardStep gotoStep = getGotoStep(event);
        getPolicyWizardBean().gotoStep(gotoStep.toInt());

        if (gotoStep.equals(PolicyWizardStep.ADVANCED)) {
            PolicyWizardAdvancedTabIndex gotoIndex = getGotoAdvancedTabIndex(event);
            getPolicyWizardBean().setAdvancedTabsetIndex(gotoIndex.toInt());
        }
    }
}
