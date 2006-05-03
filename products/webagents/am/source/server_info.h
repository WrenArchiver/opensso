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
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * Abstract:
 *
 * Information needed to connect with a DSAME server.
 *
 */ 

#ifndef SERVER_INFO_H
#define SERVER_INFO_H

#include <string>

#include "internal_macros.h"

BEGIN_PRIVATE_NAMESPACE

class ServerInfo {
public:

    /* The constructors and methods throw std::invalid_argument 
     * if any url in a URL list argument is invalid.
     */
    ServerInfo(): host(), port(0), use_ssl(false), uri() {}
    ServerInfo(const char *url, std::size_t len = 0);
    ServerInfo(const std::string& url);
    ServerInfo(const ServerInfo &svrInfo):host(svrInfo.host), 
	port(svrInfo.port), use_ssl(svrInfo.use_ssl), uri(svrInfo.uri) {}

    void setFromString(const char *url, std::size_t len = 0);
    void setFromString(const std::string& url);

    void setHost(const std::string& newHost) { host = newHost; }
    void setPort(unsigned short newPort) { port = newPort; }
    void setUseSSL(bool newSSLValue) { use_ssl = newSSLValue; }
    void setURI(const std::string& uri);

    const std::string& getProtocol() const { return use_ssl ? https : http; }
    const std::string& getHost() const { return host; }
    unsigned short getPort() const { return port; }
    bool useSSL() const { return use_ssl; }
    const std::string& getURI() const { return uri; }

    std::string toString() const;

private:
    void parseURL(const char *url, std::size_t len);

    static const std::string http;
    static const std::string https;

    std::string host;
    unsigned short port;
    bool use_ssl;
    std::string uri;
};

END_PRIVATE_NAMESPACE

#endif	// not SERVER_INFO_H
