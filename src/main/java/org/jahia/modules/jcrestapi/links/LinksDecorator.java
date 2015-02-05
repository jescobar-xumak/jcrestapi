/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 */
package org.jahia.modules.jcrestapi.links;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.Version;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.jahia.modules.jcrestapi.API;
import org.jahia.modules.jcrestapi.URIUtils;
import org.jahia.modules.json.JSONConstants;
import org.jahia.modules.json.JSONDecorator;
import org.jahia.modules.json.JSONItem;
import org.jahia.modules.json.JSONMixin;
import org.jahia.modules.json.JSONNode;
import org.jahia.modules.json.JSONProperty;
import org.jahia.modules.json.JSONSubElementContainer;
import org.jahia.modules.json.JSONVersion;
import org.jahia.modules.json.Names;

/**
 * @author Christophe Laprun
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class LinksDecorator implements JSONDecorator<LinksDecorator> {

    public static final String JCR__PROPERTY_DEFINITION = "jcr__propertyDefinition";
    @XmlElement(name = "_links")
    private final Map<String, JSONLink> links;

    public LinksDecorator() {
        links = new HashMap<String, JSONLink>(7);
    }

    public LinksDecorator(String uri) {
        this();

        initWith(uri);
    }

    public String getURI() {
        final JSONLink link = getLink(API.SELF);
        return link != null ? link.getURIAsString() : null;
    }

    public void initWith(String uri) {
        addLink(JSONLink.createLink(API.ABSOLUTE, URIUtils.getAbsoluteURI(uri)));
        addLink(JSONLink.createLink(API.SELF, uri));
    }

    public void addLink(JSONLink link) {
        links.put(link.getRel(), link);
    }

    protected JSONLink getLink(String relation) {
        return links.get(relation);
    }

    public Map<String, JSONLink> getLinks() {
        return Collections.unmodifiableMap(links);
    }

    public void initFrom(JSONSubElementContainer<LinksDecorator> container) {
        final String uri = container.getParent().getDecorator().getURI();
        initWith(URIUtils.getChildURI(uri, container.getSubElementContainerName(), false));
        addLink(JSONLink.createLink(API.PARENT, uri));
    }

    public <T extends Item> void initFrom(JSONItem<T, LinksDecorator> jsonItem, T item) throws RepositoryException {
        initWith(URIUtils.getURIFor(item));
        addLink(JSONLink.createLink(API.TYPE, URIUtils.getTypeURI(getTypeChildPath(jsonItem, item))));

        Node parent;
        try {
            parent = item.getParent();
        } catch (ItemNotFoundException e) {
            // expected when the item is root node, specify that parent is itself
            parent = (Node) item;
        }
        addLink(JSONLink.createLink(API.PARENT, URIUtils.getIdURI(parent.getIdentifier())));

        addLink(JSONLink.createLink(API.PATH, URIUtils.getURIFor(item, true)));
    }

    private <T extends Item> String getTypeChildPath(JSONItem<T, LinksDecorator> jsonItem, T item) throws RepositoryException {
        if (item instanceof Node) {
            return Names.escape(jsonItem.getUnescapedTypeName(item));
        } else {
            // get declaring node type
            final NodeType declaringNodeType = ((Property) item).getDefinition().getDeclaringNodeType();

            // get its name and escape it
            final String parentName = Names.escape(declaringNodeType.getName());

            // get its property definitions
            final PropertyDefinition[] parentPropDefs = declaringNodeType.getDeclaredPropertyDefinitions();
            final int numberOfPropertyDefinitions = parentPropDefs.length;

            // if we only have one property definition, we're done
            if (numberOfPropertyDefinitions == 1) {
                return URIUtils.getChildURI(parentName, JCR__PROPERTY_DEFINITION, false);
            } else {
                // we need to figure out which property definition matches ours in the array
                int index = 1; // JCR indexes start at 1
                for (int i = 0; i < numberOfPropertyDefinitions; i++) {
                    PropertyDefinition propDef = parentPropDefs[i];
                    if (propDef.getName().equals(item.getName())) {
                        index = i + 1; // adjust for start at 1 in JCR
                        break;
                    }
                }
                // create the indexed escaped link, if index = 1, no need for an index
                return URIUtils.getChildURI(parentName, Names.escape(JCR__PROPERTY_DEFINITION, index), false);
            }
        }
    }

    public void initFrom(JSONNode<LinksDecorator> jsonNode) {
        addLink(JSONLink.createLink(JSONConstants.PROPERTIES, jsonNode.getJSONProperties().getDecorator().getURI()));
        addLink(JSONLink.createLink(JSONConstants.MIXINS, jsonNode.getJSONMixins().getDecorator().getURI()));
        addLink(JSONLink.createLink(JSONConstants.CHILDREN, jsonNode.getJSONChildren().getDecorator().getURI()));
        addLink(JSONLink.createLink(JSONConstants.VERSIONS, jsonNode.getJSONVersions().getDecorator().getURI()));
    }

    @Override
    public LinksDecorator newInstance() {
        return new LinksDecorator();
    }

    public void initFrom(JSONProperty jsonProperty) throws RepositoryException {
        final boolean reference = jsonProperty.isReference();
        if (reference) {
            if (jsonProperty.isMultiValued()) {
                final String[] values = jsonProperty.getValueAsStringArray();
                final String[] links = new String[values.length];

                for (int i = 0; i < values.length; i++) {
                    final String val = values[i];
                    links[i] = getTargetLink(val, jsonProperty.isPath());
                }

                addLink(JSONLink.createLink(API.TARGET, links));
            } else {
                addLink(JSONLink.createLink(API.TARGET, getTargetLink(jsonProperty.getValueAsString(), jsonProperty.isPath())));
            }
        }
    }

    private String getTargetLink(String valueAsString, boolean path) throws RepositoryException {
        return path ? URIUtils.getByPathURI(valueAsString) : URIUtils.getIdURI(valueAsString);
    }

    public void initFrom(JSONMixin mixin) {
        addLink(JSONLink.createLink(API.TYPE, URIUtils.getTypeURI(Names.escape(mixin.getType()))));
    }

    public void initFrom(JSONVersion jsonVersion, Version version) throws RepositoryException {
        final Version linearPredecessor = version.getLinearPredecessor();
        if (linearPredecessor != null) {
            addLink(JSONLink.createLink("previous", URIUtils.getURIFor(linearPredecessor)));
        }
        final Version linearSuccessor = version.getLinearSuccessor();
        if (linearSuccessor != null) {
            addLink(JSONLink.createLink("next", URIUtils.getURIFor(linearSuccessor)));
        }
        final Node frozenNode = version.getFrozenNode();
        if (frozenNode != null) {
            addLink(JSONLink.createLink("nodeAtVersion", URIUtils.getURIFor(frozenNode)));
        }
    }
}
