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
 * $Id: EntitlementThreadPool.java,v 1.2 2009-06-09 05:29:15 arviranga Exp $
 *
 */

package com.sun.identity.entitlement;

import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.entitlement.interfaces.IThreadPool;

/**
 * Thread Pool
 */
public class EntitlementThreadPool implements IThreadPool {
    private static ThreadPool thrdPool;
    private static ShutdownListener shutdownListener;
    private static final int DEFAULT_POOL_SIZE = 10;

    static {
        ShutdownManager shutdownMan = ShutdownManager.getInstance(); 
        if (thrdPool != null) {
            if (shutdownMan.acquireValidLock()) {
                shutdownMan.removeShutdownListener(shutdownListener);
                thrdPool.shutdown();
                createThreadPool(shutdownMan);
            }
        } else {
            if (shutdownMan.acquireValidLock()) {
                createThreadPool(shutdownMan);
            }
        }
    }

    private static void createThreadPool(
        ShutdownManager shutdownMan) {
        try {
            // Create a new thread pool
            thrdPool = new ThreadPool("entitlementThreadPool",
                DEFAULT_POOL_SIZE);

            shutdownListener = new ShutdownListener() {
                public void shutdown() {
                    thrdPool.shutdown();
                }
            };
            // Register to shutdown hook
            shutdownMan.addShutdownListener(shutdownListener);
        } finally {
            shutdownMan.releaseLockAndNotify();
        }
    }

    public void submit(Runnable task) {
        try {
            if (isMutiTreaded()) {
                thrdPool.run(task);
            } else {
                task.run();
            }
        } catch (ThreadPoolException e) {
            PrivilegeManager.debug.error("EntitlementThreadPool.submit", e);
        }
    }

    @Override
    public boolean isMutiTreaded() {
        return (DEFAULT_POOL_SIZE > 0);
    }
}
