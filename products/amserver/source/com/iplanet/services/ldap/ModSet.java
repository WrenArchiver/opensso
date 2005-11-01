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
 * $Id: ModSet.java,v 1.1 2005-11-01 00:30:17 arvindp Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.iplanet.services.ldap;

import netscape.ldap.LDAPModification;
import netscape.ldap.LDAPModificationSet;

/**
 * iPlanet-PUBLIC-CLASS Represents a set of modification on attributes
 * 
 */
public class ModSet extends LDAPModificationSet {
    // TODO: This is an incomplete implementation. Currently subclass from
    // LDAPModificationSet is used to get things going. Need internal
    // representation overhaul to move away from "extends LDAPModification"

    /**
     * Modification specifiers for ADD
     */
    public static final int ADD = LDAPModification.ADD;

    /**
     * Modification specifiers for REPLACE
     */
    public static final int REPLACE = LDAPModification.REPLACE;

    /**
     * Modification specifiers for DELETE
     */
    public static final int DELETE = LDAPModification.DELETE;

    /**
     * Default consturctor
     */
    public ModSet() {
        super();
    }

    /**
     * Constructor with an attribute set defaulting all operation types to
     * ModSet.ADD
     * 
     * @param attrSet
     *            Attribute set to construct the modSet. All operations are
     *            default to ModSet.ADD
     */
    public ModSet(AttrSet attrSet) {
        this(attrSet, ModSet.ADD);
    }

    /**
     * Construct ModSet given the same operation on a set of attributes
     * 
     * @param attrSet
     *            Attribute set to construct the ModSet
     * @param op
     *            Operation type for ADD, REPLACE or DELETE
     */
    public ModSet(AttrSet attrSet, int op) {

        super();
        for (int i = 0; i < attrSet.size(); i++) {
            this.add(op, attrSet.elementAt(i).toLDAPAttribute());
        }
    }
}
