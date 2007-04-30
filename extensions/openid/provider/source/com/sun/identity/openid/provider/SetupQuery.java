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
 * $Id: SetupQuery.java,v 1.1 2007-04-30 01:28:32 pbryan Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2007 Paul C. Bryan
 */

package com.sun.identity.openid.provider;

import java.util.Map;

/**
 * TODO: Description.
 *
 * @author pbryan
 */
public class SetupQuery extends CheckidQuery
{
    /**
     * TODO: Description.
     */
    public SetupQuery() {
        super();
    }

    /**
     * TODO: Description.
     *
     * @param map TODO.
     * @throws DecodeException TODO.
     */
    protected void decode(Map<String,String> map) throws DecodeException
    {
        super.decode(map);

        if (!getMode().equals(Mode.CHECKID_SETUP)) {
            throw new DecodeException("mode must be checkid_setup");
        }
    }
}
