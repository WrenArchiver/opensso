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
 * $Id: AjaxRequestWrapper.java,v 1.2 2008-06-25 05:44:02 qcheng Exp $
 *
 */

package com.sun.identity.setup;

import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

public class AjaxRequestWrapper
    implements IHttpServletRequest {
    private Locale locale;
    private Map parameterMap;
    private String contextPath;

    public AjaxRequestWrapper(
        String locale,
        Map parameters,
        String contextPath
    ) {
        this.locale = new Locale(locale);
        this.contextPath = contextPath;
        parameterMap = parameters;
    }

    /**
     * Returns the locale of the request.
     *
     * @return the locale of the request.
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns all parameters values
     *
     * @return all parameters values
     */
    public Map getParameterMap() {
        return parameterMap;
    }

    /**
     * Adds parmeter.
     *
     * @param parameterName Name of Parameter. 
     * @param parameterValue Value of Parameter. 
     */
    public void addParameter(String parameterName, Object parameterValue) {
        parameterMap.put(parameterName, parameterValue);
    }
    
    /**
     * Returns values of a parameter.
     *
     * @param parameterName Name of Parameter. 
     * @return values of a parameter.
     */
    public Object getParameter(String parameterName) {
        return parameterMap.get(parameterName);
    }

    /**
     * Returns the context path.
     *
     * @return the context path.
     */
    public String getContextPath() {
        return contextPath;
    }
}
