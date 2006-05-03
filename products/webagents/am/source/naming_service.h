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
 * Service interface class for the AM "Naming" service.
 *
 */ 

#ifndef NAMING_SERVICE_H
#define NAMING_SERVICE_H

#include "base_service.h"
#include "internal_macros.h"
#include "naming_info.h"
#include "sso_token.h"

BEGIN_PRIVATE_NAMESPACE

class NamingService: public BaseService {
public:
    explicit NamingService(const Properties& props,
                const std::string &cert_passwd,
                const std::string &cert_nick_name,
                bool alwaysTrustServerCert);
    virtual ~NamingService();

    /* Throws XMLTree::ParseException */
    am_status_t getProfile(const ServiceInfo& service,
			      const std::string& ssoToken,
			      const Http::CookieList& cookieList,
			      NamingInfo& namingInfo);

private:

    bool ignoreNamingService;
    std::string namingURL;

    void processAttribute(const std::string& name, const std::string& value,
			  NamingInfo& namingInfo) const;
    /* Throws XMLTree::ParseException */
    am_status_t parseNamingResponse(const std::string& data,
				       const std::string& ssoToken,
				       NamingInfo& namingInfo) const;
	am_status_t NamingService::check_server_alive(std::string hostname, unsigned short portnumber);

    static const BodyChunk prefixChunk;
    static const BodyChunk namingPrefixChunk;
    static const BodyChunk sessidPrefixChunk;
    static const BodyChunk preferredNamingPrefixChunk;
    static const BodyChunk suffixChunk;
    static const std::string loggingAttribute;
    static const std::string policyAttribute;
    static const std::string profileAttribute;
    static const std::string sessionAttribute;
    static const std::string invalidSessionMsgPrefix;
    static const std::string invalidSessionMsgSuffix;
};

END_PRIVATE_NAMESPACE

#endif	/* not NAMING_SERVICE_H */
