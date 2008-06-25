/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: EntityOpViewBeanBase.java,v 1.3 2008-06-25 05:42:59 qcheng Exp $
 *
 */

package com.sun.identity.console.idm;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.HREF;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPostViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.idm.model.EntitiesModel;
import com.sun.identity.console.idm.model.EntitiesModelImpl;
import com.sun.identity.console.property.PropertyTemplate;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.tabs.CCTabs;
import javax.servlet.http.HttpServletRequest;

public abstract class EntityOpViewBeanBase
    extends AMPrimaryMastHeadViewBean {
    protected static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    public static final String ENTITY_NAME = "tfEntityName";
    public static final String ENTITY_TYPE = "entityTypeName";
    public static final String ENTITY_TYPE_NAME = "tfEntityTypeName";

    protected CCPageTitleModel ptModel;
    protected AMPropertySheetModel propertySheetModel;
    protected boolean hasNoAttributeToDisplay;
    protected boolean submitCycle;

    public EntityOpViewBeanBase(String name, String defaultDisplayURL) {
        super(name);
        setDefaultDisplayURL(defaultDisplayURL);
    }

    protected void initialize() {
        if (!initialized) {
            initialized = createPropertyModel();

            if (initialized) {
                super.initialize();
                createPageTitleModel();
                registerChildren();
                registerChild(AMAdminConstants.DYN_LINK_COMPONENT_NAME,
                    HREF.class);
            }
        }
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new EntitiesModelImpl(req, getPageSessionAttributes());
    }

    protected boolean createPropertyModel() {
        boolean created = false;
        String type = (String)getPageSessionAttribute(ENTITY_TYPE);

        if ((type != null) && (type.trim().length() > 0)) {
            AMPropertySheetModel psModel = createPropertySheetModel(type);
            if (psModel != null) {
                propertySheetModel = psModel;
                propertySheetModel.clear();
            }
            created = true;
        }

        return created;
    }

    protected AMPropertySheetModel createPropertySheetModel(String type) {
        AMPropertySheetModel psModel = null;
        EntitiesModel model = (EntitiesModel)getModel();
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);

        try {
            psModel = new AMPropertySheetModel(
                model.getPropertyXMLString(realmName, type, isCreateViewBean(),
                getClass().getName()));
        } catch (AMConsoleException e) {
            psModel = handleNoAttributeToDisplay(e);
        }
        return psModel;
    }

    protected AMPropertySheetModel handleNoAttributeToDisplay(
        AMConsoleException e) {
        hasNoAttributeToDisplay = true;
        setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
            e.getMessage());
        return new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/propertyBlank.xml"));
    }

    protected void registerChildren() {
        super.registerChildren();
        ptModel.registerChildren(this);
        registerChild(TAB_COMMON, CCTabs.class);

        if (propertySheetModel != null) {
            registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
            propertySheetModel.registerChildren(this);
        }
    }

    protected View createChild(String name) {
        View view = null;

        if ((ptModel != null) && ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if ((propertySheetModel != null) &&
            propertySheetModel.isChildSupported(name)
        ) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else if (name.equals(AMAdminConstants.DYN_LINK_COMPONENT_NAME)) {
            view = new HREF(this, name, "");
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);

        if (!submitCycle) {
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            EntitiesModel model = (EntitiesModel)getModel();

            try {
                String entityType = (String)getPageSessionAttribute(
                    ENTITY_TYPE);
                setDefaultValues(entityType);
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }
        }
    }

    protected void forwardToEntitiesViewBean() {
        EntitiesViewBean vb = (EntitiesViewBean)getViewBean(
            EntitiesViewBean.class);
        String entityType = (String)getPageSessionAttribute(ENTITY_TYPE);

        String tabId = Integer.toString(entityType.hashCode());
        setPageSessionAttribute(super.getTrackingTabIDName(), tabId);
        setPageSessionAttribute("CCTabs.SelectedTabId", tabId); 
        backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardToEntitiesViewBean();
    }

    protected abstract void createPageTitleModel();
    protected abstract void setDefaultValues(String type)
        throws AMConsoleException;
    protected abstract boolean isCreateViewBean();

    public void handleDynLinkRequest(RequestInvocationEvent event) {
        HttpServletRequest req = getRequestContext().getRequest();
        String url = req.getParameter(
            PropertyTemplate.PARAM_PROPERTIES_VIEW_BEAN_URL);
        AMPostViewBean vb = (AMPostViewBean)getViewBean(AMPostViewBean.class);
        passPgSessionMap(vb);
        vb.setTargetViewBeanURL(url);
        vb.forwardTo(getRequestContext());
    }
}
