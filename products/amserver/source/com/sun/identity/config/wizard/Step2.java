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
 * $Id: Step2.java,v 1.15 2010-01-04 19:08:36 veiming Exp $
 *
 */
package com.sun.identity.config.wizard;

import com.sun.identity.config.SessionAttributeNames;
import com.sun.identity.config.util.AjaxPage;
import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.setup.SetupConstants;
import java.io.File;
import net.sf.click.control.ActionLink;

public class Step2 extends AjaxPage {
    public ActionLink validateConfigDirLink = 
        new ActionLink("validateConfigDir", this, "validateConfigDir");
    public ActionLink validateCookieDomainLink = 
        new ActionLink("validateCookieDomain", this, "validateCookieDomain");
    
    public Step2() {
    }
    
    public void onInit() {
        String val = (String)getContext().getSessionAttribute(
            SessionAttributeNames.SERVER_URL);
        if (val == null) {
            val = getServerURL();
        }
        add("serverURL", val);

        val = (String)getContext().getSessionAttribute(
            SessionAttributeNames.COOKIE_DOMAIN);
        if (val == null) {
            val = getCookieDomain();
        }
        add("cookieDomain", val);

        val = (String)getContext().getSessionAttribute(
            SessionAttributeNames.PLATFORM_LOCALE);
        if (val == null) {
            val = SetupConstants.DEFAULT_PLATFORM_LOCALE;
        }
        add("platformLocale", val);

        String baseDir = null;
        String presetDir = AMSetupServlet.getPresetConfigDir();
        if ((presetDir == null) || (presetDir.trim().length() == 0)) {
            add("fixDir",            
                "onkeyup=\"APP.callDelayed(this, validateConfigDir)\"");
            val = (String)getContext().getSessionAttribute(
                SessionAttributeNames.CONFIG_DIR);
            if (val == null) {
                val = getBaseDir(getContext().getRequest());
            }
            add("configDirectory", val);
            baseDir = val;
        } else {
            add("fixDir", "disabled");
            add("configDirectory", presetDir);
            baseDir = presetDir;
        }

        if (hasWritePermission(baseDir)) {
            add("canWriteDir", "");
        } else {
            add("canWriteDir", getLocalizedString(
                "configuration.wizard.step2.no.write.permission.to.basedir"));
        }
        
        super.onInit();
    }

    private static boolean hasWritePermission(String dirName) {
        File f = new File(dirName);
        while ((f != null) && !f.exists()) {
            f = f.getParentFile();
        }
        return (f == null) ? false : f.isDirectory() && f.canWrite();
    }
    
    public boolean validateConfigDir() {
        String configDir = toString("dir");
        
        if (configDir == null) {
            writeToResponse(getLocalizedString("missing.required.field"));
        } else if (!hasWritePermission(configDir)) {
            writeToResponse(getLocalizedString(
                "configuration.wizard.step2.no.write.permission.to.basedir"));
        } else {
            getContext().setSessionAttribute(
                SessionAttributeNames.CONFIG_DIR, configDir);
            writeToResponse("true");
        }
        setPath(null);        
        return false;    
    }

    public boolean validateCookieDomain() {
        String domain = toString("domain");
        
        if (domain == null) {
            writeToResponse(getLocalizedString("missing.required.field"));
        } else if (domain.indexOf(':') != -1) {
            writeToResponse(getLocalizedString(
                "configuration.wizard.step2.invalid.cookie.domain"));
        } else {
            getContext().setSessionAttribute(
                SessionAttributeNames.COOKIE_DOMAIN, domain);
            writeToResponse("true");
        }
        setPath(null);
        return false;
    }

    private String getServerURL() {        
        String hostname = (String)getContext().getRequest().getServerName();
        int portnum  = (int)getContext().getRequest().getServerPort();
        String protocol = (String)getContext().getRequest().getScheme();
        return protocol + "://" + hostname + ":" + portnum;
    }

    /**
     * used to add the key to the page and to the session so it can 
     * be retrieved when the final store is done
     */
    private void add(String key, String value) {
        addModel(key, value);
        getContext().setSessionAttribute(key, value);
    }
}
