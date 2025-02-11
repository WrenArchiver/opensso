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
 * $Id: ReferralManageHandler.java,v 1.4 2009-12-22 23:33:14 farble1670 Exp $
 */

package com.sun.identity.admin.handler;

import com.sun.identity.admin.Resources;
import com.sun.identity.admin.dao.ReferralDao;
import com.sun.identity.admin.model.MessageBean;
import com.sun.identity.admin.model.MessagesBean;
import com.sun.identity.admin.model.PhaseEventAction;
import com.sun.identity.admin.model.FilterHolder;
import com.sun.identity.admin.model.QueuedActionBean;
import com.sun.identity.admin.model.ReferralBean;
import com.sun.identity.admin.model.ReferralManageBean;
import com.sun.identity.admin.model.ViewApplicationsBean;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;

public class ReferralManageHandler implements Serializable {
    private ReferralManageBean referralManageBean;
    private QueuedActionBean queuedActionBean;
    private ReferralDao referralDao;
    private MessagesBean messagesBean;

    public FilterHolder getFilterHolder(ActionEvent event) {
        FilterHolder fh = (FilterHolder) event.getComponent().getAttributes().get("filterHolder");
        assert (fh != null);
        return fh;
    }

    public void selectAllListener(ActionEvent event) {
        selectListener(event, true);
    }

    public void selectNoneListener(ActionEvent event) {
        selectListener(event, false);
    }

    private void selectListener(ActionEvent event, boolean select) {
        int size = referralManageBean.getReferralBeans().size();
        int first = referralManageBean.getDataPaginator().getFirstRow();
        int rows = referralManageBean.getDataPaginator().getRows();
        int last = Math.min(first+rows, size);

        for (int i = first; i < last; i++) {
            ReferralBean rb = referralManageBean.getReferralBeans().get(i);
            rb.setSelected(select);
        }
    }

    public void sortTableListener(ActionEvent event) {
        PhaseEventAction pea = new PhaseEventAction();
        pea.setDoBeforePhase(true);
        pea.setPhaseId(PhaseId.RENDER_RESPONSE);
        pea.setAction("#{referralManageHandler.handleSort}");
        pea.setParameters(new Class[]{});
        pea.setArguments(new Object[]{});

        queuedActionBean.getPhaseEventActions().add(pea);
    }

    public void handleSort() {
        referralManageBean.getReferralManageTableBean().sort();
    }

    public void setReferralManageBean(ReferralManageBean referralManageBean) {
        this.referralManageBean = referralManageBean;
    }

    public void setQueuedActionBean(QueuedActionBean queuedActionBean) {
        this.queuedActionBean = queuedActionBean;
    }

    public void setReferralDao(ReferralDao referralDao) {
        this.referralDao = referralDao;
    }

    public void setMessagesBean(MessagesBean messagesBean) {
        this.messagesBean = messagesBean;
    }

    public ReferralBean getReferralBean(ActionEvent event) {
        ReferralBean rb = (ReferralBean) event.getComponent().getAttributes().get("referralBean");
        assert (rb != null);
        return rb;
    }

    public void removeListener(ActionEvent event) {
        if (!referralManageBean.isRemovePopupVisible()) {
            if (referralManageBean.getSizeSelected() == 0) {
                MessageBean mb = new MessageBean();
                Resources r = new Resources();
                mb.setSummary(r.getString(this, "removeNoneSelectedSummary"));
                mb.setDetail(r.getString(this, "removeNoneSelectedDetail"));
                mb.setSeverity(FacesMessage.SEVERITY_ERROR);
                messagesBean.addMessageBean(mb);
            } else {
                referralManageBean.setRemovePopupVisible(true);
            }
        } else {
            referralManageBean.setRemovePopupVisible(false);
        }
    }

    public void removePopupOkListener(ActionEvent event) {
        PhaseEventAction pea = new PhaseEventAction();
        pea.setDoBeforePhase(true);
        pea.setPhaseId(PhaseId.RENDER_RESPONSE);
        pea.setAction("#{referralManageHandler.handleRemoveAction}");
        pea.setParameters(new Class[]{});
        pea.setArguments(new Object[]{});

        queuedActionBean.getPhaseEventActions().add(pea);

        referralManageBean.setRemovePopupVisible(false);
    }

    public void removePopupCancelListener(ActionEvent event) {
        referralManageBean.setRemovePopupVisible(false);
    }

    public void handleRemoveAction() {
        Set<ReferralBean> removed = new HashSet<ReferralBean>();
        for (ReferralBean rb : referralManageBean.getReferralBeans()) {
            if (rb.isSelected()) {
                removed.add(rb);
                referralDao.remove(rb.getName());
            }
            rb.setSelected(false);
        }
        referralManageBean.getReferralBeans().removeAll(removed);
    }

    public void viewOptionsPopupCancelListener(ActionEvent event) {
        referralManageBean.setViewOptionsPopupVisible(false);
    }

    public void viewOptionsPopupOkListener(ActionEvent event) {
        referralManageBean.getReferralManageTableBean().getColumnsVisible().clear();
        referralManageBean.getReferralManageTableBean().getColumnsVisible().addAll(referralManageBean.getViewOptionsPopupColumnsVisible());
        referralManageBean.getReferralManageTableBean().setRows(referralManageBean.getViewOptionsPopupRows());

        referralManageBean.setViewOptionsPopupVisible(false);
    }

    public void viewOptionsListener(ActionEvent event) {
        referralManageBean.getViewOptionsPopupColumnsVisible().clear();
        referralManageBean.getViewOptionsPopupColumnsVisible().addAll(referralManageBean.getReferralManageTableBean().getColumnsVisible());
        referralManageBean.setViewOptionsPopupRows(referralManageBean.getReferralManageTableBean().getRows());

        referralManageBean.setViewOptionsPopupVisible(true);
    }

    public void addViewFilterListener(ActionEvent event) {
        referralManageBean.newFilterHolder();
        addResetEvent();
    }

    private void addResetEvent() {
        PhaseEventAction pea = new PhaseEventAction();
        pea.setDoBeforePhase(true);
        pea.setPhaseId(PhaseId.RENDER_RESPONSE);
        pea.setAction("#{referralManageHandler.handleReset}");
        pea.setParameters(new Class[]{});
        pea.setArguments(new Object[]{});

        queuedActionBean.getPhaseEventActions().add(pea);
    }

    public void handleReset() {
        referralManageBean.reset();
    }

    public void viewFilterChangedListener(ValueChangeEvent event) {
        addResetEvent();
    }

    public void viewFilterChangedListener(ActionEvent event) {
        addResetEvent();
    }

    public void removeViewFilterListener(ActionEvent event) {
        FilterHolder fh = getFilterHolder(event);
        referralManageBean.getFilterHolders().remove(fh);
        addResetEvent();
    }

    public String manageAction() {
        ViewApplicationsBean vasb = ViewApplicationsBean.getInstance();
        if (vasb.getViewApplications() == null || vasb.getViewApplications().size() == 0) {
            MessageBean mb = new MessageBean();
            Resources r = new Resources();
            mb.setSummary(r.getString(this, "noApplicationsSummary"));
            mb.setDetail(r.getString(this, "noApplicationsDetail"));
            mb.setSeverity(FacesMessage.SEVERITY_ERROR);
            messagesBean.addMessageBean(mb);

            return null;
        } else {
            return "referral-manage";
        }
    }
}
