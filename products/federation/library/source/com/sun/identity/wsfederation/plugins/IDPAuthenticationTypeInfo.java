/* The contents of this file are subject to the terms
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
 * $Id: IDPAuthenticationTypeInfo.java,v 1.3 2007-11-05 06:15:58 qcheng Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


package com.sun.identity.wsfederation.plugins;

import java.util.Set;

/**
 * 
 * The class <code>IDPAuthenticationTypeInfo</code> consists of the mapping 
 * between <code>AuthenticationType</code> and the actual 
 * authentication mechanism at the Identity Provider. 
 * 
 * @supported.all.api 
 */ 

public class IDPAuthenticationTypeInfo {
    String authenticationType;
    Set authnTypeAndValues;
   
   /** 
    * The constructor. 
    *
    * @param authenticationType The <code>AuthnContext</code> that is returned
    *  to the requester.
    * @param authnTypeAndValues The set of authentication mechanism
    */ 
    public IDPAuthenticationTypeInfo(String authenticationType,
                            Set authnTypeAndValues) {
        this.authenticationType = authenticationType;
        this.authnTypeAndValues = authnTypeAndValues;
    }

   /** 
    * Returns the returning <code>AuthnContext</code>
    *
    * @return the returning <code>AuthnContext</code>
    */ 
    public String getAuthenticationTypet() {
        return authenticationType;
    }

   /** 
    * Returns the set of authentication mechanism
    *
    * @return the set of authentication mechanism
    */ 
    public Set getAuthnTypeAndValues() {
        return authnTypeAndValues;
    }
}

