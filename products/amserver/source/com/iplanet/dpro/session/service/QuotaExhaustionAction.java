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
 * $Id: QuotaExhaustionAction.java,v 1.1 2005-11-01 00:29:55 arvindp Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.iplanet.dpro.session.service;

import java.util.Map;

/**
 * Interface to defined the resulting behavior if the sessiojn quota is
 * exhausted.
 */
public interface QuotaExhaustionAction {

    /**
     * Check if the session quota for a given user has been exhausted and
     * perform necessary actions in such as case.
     * 
     * @param is the to-be-actived InternalSession
     * @param existingsessions all existing sessions beloning to the same uuid
     *          (Map:sid->expiration_time)
     * @return true if the session activation request should be rejected, false
     *         otherwise
     */
    public boolean action(InternalSession is, Map existingsessions);

}
