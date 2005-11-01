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
 * $Id: IdSearchControl.java,v 1.1 2005-11-01 00:31:11 arvindp Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.idm;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;

/* iPlanet-PUBLIC-CLASS */

/**
 * This is a helper class which is used in the <code> AMIdentityRepository
 * </code>
 * search method. It is used to to modify search controls for a given search
 * operation. The specific controls that can be modified are: maximum time limit
 * and size limit for the search, attributes that should be returned from the
 * search, simple modifications to be made to the search filter used by each
 * plugin by adding attribute-values which can be either OR'ed or AND'ed to the
 * basic search.
 * 
 */
public final class IdSearchControl {
    private Set returnAttributes = null;

    // Disabled by default
    private static boolean getAllAttributesEnabled = false;

    private int timeOut = 0;

    private int maxResults = 0;

    Map avPairs = null;

    IdSearchOpModifier modifier = IdSearchOpModifier.OR;

    boolean recursive = false;

    /**
     * Creates the <code>IdSearchControl</code> object
     */
    public IdSearchControl() {

    }

    /**
     * Set the return attribute names, if attributes of the entries need to be
     * obtained as part of the search.
     * 
     * NOTE: If the return attribute values are specified as part of
     * <code>AMSearchControl</code>, there could be a significant performance
     * overhead compared to when none are specified. When the return attributes
     * are set, the return attributes can be obtained as a map with DN as
     * map-key and set of attribute values as map-value from
     * <code>AMSearchResults</code> object.
     * 
     * @param attributeNames
     *            Set of attribute names whose values need to be obtained as
     *            part of the search.
     * 
     */
    public void setReturnAttributes(Set attributeNames) {
        if (attributeNames != null && !attributeNames.isEmpty()) {
            returnAttributes = new CaseInsensitiveHashSet();
            returnAttributes.addAll(attributeNames);
        }
    }

    /**
     * Returns the list of attributes requested to be read when the search is
     * performed.
     * 
     * @return Set of attributes requested to be read.
     */
    public Set getReturnAttributes() {
        return returnAttributes;
    }

    /**
     * Sets the specified boolean value to the variable. Boolean value is set to
     * true, if all attributes of the entries need to be obtained as part of the
     * search.
     * 
     * When the option for getting all attributes is set to true, the search
     * results will return a Map, where the Key is the DN of the search results,
     * and value is another Map of attribute names for keys and Sets for values
     * of those attributes.
     * 
     * @param getAllAttributes
     *            Boolean value set to true as part of the
     *            <code>IdSearchControl</code> to obtain all attributes as
     *            part of the search.
     * 
     * 
     */
    public void setAllReturnAttributes(boolean getAllAttributes) {
        getAllAttributesEnabled = true;
    }

    /**
     * Returns true if the option for getting all attributes has been enabled.
     * 
     * @return true if the option for getting all attributes has been enabled.
     */
    public boolean isGetAllReturnAttributesEnabled() {
        return getAllAttributesEnabled;
    }

    /**
     * Sets the maximum number of milliseconds to wait for any operation for the
     * search.
     * 
     * @param timeOut
     *            Max number of milliseconds
     * 
     */
    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    /**
     * Returns current time out setting.
     * 
     * @return current time out setting.
     */
    public int getTimeOut() {
        return timeOut;
    }

    /**
     * Sets the maximum number of search results to return; 0 means there is no
     * limit.
     * 
     * @param maxNumber
     *            Max number of results
     */
    public void setMaxResults(int maxNumber) {
        maxResults = maxNumber;
    }

    /**
     * Returns the maximum number of search results. return 0 means there is no
     * limit.
     * 
     * @return the maximum number of search results.
     */
    public int getMaxResults() {
        return maxResults;
    }

    /**
     * Set the options for modifying the basic search filter in each plugin. By
     * default, there are no modifications.
     * 
     * @param mod
     *            One of the supported IdSearchOpModifiers
     * @param avMap
     *            Map of attribute-value pairs to be used to modify the search
     *            operation.
     */
    public void setSearchModifiers(IdSearchOpModifier mod, Map avMap) {
        modifier = mod;
        if (avMap != null && !avMap.isEmpty()) {
            this.avPairs = new CaseInsensitiveHashMap();
            Iterator it = avMap.keySet().iterator();
            while (it.hasNext()) {
                String attr = (String) it.next();
                Set values = new HashSet();
                values.addAll((Set) avMap.get(attr));
                this.avPairs.put(attr, values);
            }
        }
    }

    /**
     * Returns the IdSearchOpModifier defined for this SearchControl
     * 
     * @return One of the supported IdSearchOpModifier
     */
    public IdSearchOpModifier getSearchModifier() {
        return modifier;
    }

    /**
     * Returns the map set to be used to modify the search filter in each
     * plugin.
     * 
     * @return Map of attribute values pairs, if it is set. Null otherwise.
     */
    public Map getSearchModifierMap() {
        return avPairs;
    }

    /**
     * Set the recursive flat to be true or false. It is false by default so
     * plugin searches are not recursive.
     * 
     * @param rec
     *            boolean true or false
     */
    public void setRecursive(boolean rec) {
        recursive = rec;
    }

    /**
     * Returns true if recursive is enabled, false otherwise
     * 
     * @return true or false
     */
    public boolean isRecursive() {
        return recursive;
    }
}
