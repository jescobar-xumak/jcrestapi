/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2013 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.jcrestapi;

import org.jahia.modules.jcrestapi.json.JSONNode;
import org.jahia.modules.jcrestapi.path.AccessorPair;
import org.jahia.modules.jcrestapi.path.ItemAccessor;
import org.jahia.modules.jcrestapi.path.NodeAccessor;
import org.jahia.modules.jcrestapi.path.PathParser;
import org.osgi.service.component.annotations.Component;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.Properties;

/**
 * @author Christophe Laprun
 */
@Component
@Path(API.API_PATH)
@Produces({MediaType.APPLICATION_JSON})
public class API {
    public static final String VERSION;

    static final String API_PATH = "/api";

    static {
        Properties props = new Properties();
        try {
            props.load(API.class.getClassLoader().getResourceAsStream("jcrestapi.properties"));
        } catch (Exception e) {
            throw new RuntimeException("Could not load jcrestapi.properties.");
        }

        VERSION = props.getProperty("jcrestapi.version");
    }

    public static final String PROPERTIES = "properties";
    public static final String MIXINS = "mixins";
    public static final String CHILDREN = "children";
    public static final String VERSIONS = "versions";
    public static final String TYPE = "type";

    private SpringBeansAccess beansAccess = SpringBeansAccess.getInstance();

    void setBeansAccess(SpringBeansAccess beansAccess) {
        this.beansAccess = beansAccess;
    }

    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    public String version() {
        return VERSION;
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    /**
     * Needed to get URI without trailing / to work :(
     */
    public Object getRootNode(@Context UriInfo info) throws RepositoryException {
        NodeAccessor.ROOT_ACCESSOR.initWith("/", info.getRequestUriBuilder());
        final Object node = getJSON(NodeAccessor.ROOT_ACCESSOR, ItemAccessor.IDENTITY_ACCESSOR);
        return node;
    }

    @GET
    @Path("{path: .*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Object getNode(@PathParam("path") String path, @Context UriInfo info) throws RepositoryException {
        final AccessorPair accessors = PathParser.getAccessorsForPath(info.getBaseUriBuilder(), info.getPathSegments());

        final Object node = getJSON(accessors.nodeAccessor, accessors.itemAccessor);
        return node;
    }

    private Object getJSON(NodeAccessor nodeAccessor, ItemAccessor itemAccessor) throws RepositoryException {
        final Session session = beansAccess.getRepository().login(new SimpleCredentials("root", new char[]{'r', 'o',
                'o', 't', '1', '2', '3', '4'}));
        try {
            // todo: optimize: we shouldn't need to load the whole node if we only want part of it
            final JSONNode node = new JSONNode(nodeAccessor.getNode(session), nodeAccessor.getUriBuilder(), 1);
            return itemAccessor.getItem(node);
        } finally {
            session.logout();
        }
    }
}
