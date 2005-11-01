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
 * $Id: DNValidator.java,v 1.1 2005-11-01 00:30:47 arvindp Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.iplanet.ums.validation;

import netscape.ldap.util.DN;

public class DNValidator implements IValidator {

    /**
     * iPlanet-PUBLIC-METHOD Determines whether the specified string is a valid
     * DN
     * 
     * @param value
     *            string value to validate
     * @param rule
     *            not used by this method
     * @return true if value is an valid DN, else return false
     */
    public boolean validate(String value, String rule) {
        return validate(value);
    }

    /**
     * iPlanet-PUBLIC-METHOD Determines whether the specified string is a valid
     * DN
     * 
     * @param value
     *            string to test
     * @return true if value is an DN
     */
    public boolean validate(String value) {
        if (DN.isDN(value)) {
            return true;
        } else {
            return false;
        }
    }
}
