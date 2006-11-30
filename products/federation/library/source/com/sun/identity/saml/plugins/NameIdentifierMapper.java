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
 * $Id: NameIdentifierMapper.java,v 1.1 2006-11-30 02:31:19 bina Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.saml.plugins;

import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLException;

/**
 * The class <code>NameIdentifierMapper</code> is an interface
 * that is implemented to map user account to name identifier in
 * assertion subject in Sun Java System Access Manager.  
 *
 * @supported.all.api
 */
public interface NameIdentifierMapper {

    /**
     * Returns name identifier for assertion subject based on user account in
     * the data store.
     *
     * @param session the session of the user performing the SSO operation.
     * @param sourceID source ID for the site from which the assertion
     *        originated.
     * @param destID destination ID for the site for which the assertion will be
     *        created.
     * @return a <code>NameIdentifier</code> for assertion subject.
     * @exception SAMLException if an error occurs
     */
    public NameIdentifier getNameIdentifier(Object session, String sourceID,
        String destID) throws SAMLException;
}
