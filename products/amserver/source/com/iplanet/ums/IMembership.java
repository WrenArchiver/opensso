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
 * $Id: IMembership.java,v 1.1 2005-11-01 00:30:37 arvindp Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.iplanet.ums;

/**
 * Represents a container interface common to groups and roles.
 */
public interface IMembership {
    /**
     * Gets the members of the group.
     * 
     * @return Iterator for unique identifiers in the group
     *         iPlanet-PUBLIC-METHOD
     */
    public SearchResults getMemberIDs() throws UMSException;

    /**
     * Gets the member count of the group.
     * 
     * @return number of members in the group iPlanet-PUBLIC-METHOD
     */
    public int getMemberCount() throws UMSException;

    /**
     * Gets a member.
     * 
     * @return the guid unique identifier for a member iPlanet-PUBLIC-METHOD
     */
    public Guid getMemberIDAt(int index) throws UMSException;

    /**
     * Checks if a given identifier is a member of the group.
     * 
     * @param guid
     *            identity of member to be checked for membership
     * @return <code>true </code>if it is a member
     * @exception UMSException
     *                on failure to evaluate membership iPlanet-PUBLIC-METHOD
     */
    public boolean hasMember(Guid guid) throws UMSException;
}
