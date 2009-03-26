/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PolicyCache.java,v 1.3 2009-03-26 22:50:10 veiming Exp $
 */

package com.sun.identity.entitlement;

import com.iplanet.am.util.Cache;
import com.sun.identity.policy.Policy;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author dennis
 */
public class PolicyCache {
    private Cache cache = new Cache();
    private ReadWriteLock rwlock = new ReentrantReadWriteLock();

    public void cache(String dn, Policy policy) {
        rwlock.writeLock().lock();
        try {
            cache.put(dn, policy);
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    public void cache(Map<String, Policy> policies, boolean force) {
        rwlock.writeLock().lock();
        try {
            for (String dn : policies.keySet()) {
                if (force) {
                    cache.put(dn, policies.get(dn));
                } else {
                    Policy p = (Policy) policies.get(dn);
                    if (p == null) {
                        cache.put(dn, policies.get(dn));
                    }
                }
            }
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    public void delete(String dn) {
        rwlock.writeLock().lock();
        try {
            cache.remove(dn);
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    public Policy getPolicy(String dn) {
        rwlock.readLock().lock();
        try {
            return (Policy)cache.get(dn);
        } finally {
            rwlock.readLock().unlock();
        }
    }
}
