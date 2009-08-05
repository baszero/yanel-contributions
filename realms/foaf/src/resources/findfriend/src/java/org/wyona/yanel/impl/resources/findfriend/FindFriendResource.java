/*
 * Copyright 2007 Wyona
 */

package org.wyona.yanel.impl.resources.findfriend;

import org.wyona.yanel.core.Resource;
import org.wyona.yanel.core.api.attributes.ViewableV2;
import org.wyona.yanel.core.attributes.viewable.View;
import org.wyona.yanel.core.attributes.viewable.ViewDescriptor;

import org.wyona.meguni.parser.Parser;
import org.wyona.meguni.util.ResultSet;

import org.wyona.yarep.core.Node;
import org.wyona.yarep.core.Repository;
import org.wyona.yarep.core.RepositoryFactory;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import java.io.File;

import org.apache.log4j.Category;

/**
 *
 */
public class FindFriendResource extends Resource implements ViewableV2 {

    private static Category log = Category.getInstance(FindFriendResource.class);

    /**
     *
     */
    public FindFriendResource() {
    }

    /**
     *
     */
    public boolean exists() {
        return true;
    }

    /**
     *
     */
    public String getMimeType(String viewId) {
        if (viewId != null && viewId.equals("source")) {
            return "application/xml";
        } else {
            return "application/xhtml+xml";
        }
    }

    /**
     *
     */
    public long getSize() {
        return -1;
    }

    /**
     *
     */
    public View getView(String viewId) throws Exception {
        StringBuffer sb = new StringBuffer("<foaf xmlns=\"http://www.wyona.org/foaf/1.0\">");
        String qs = getRequest().getParameter("q");
        if (qs != null) {
            sb.append(getLocalResults(qs));
            sb.append(getThirdPartyResults(qs));
        }
        sb.append("</foaf>");

        View view = new View();
        if (viewId != null && viewId.equals("source")) {
            view.setInputStream(new java.io.StringBufferInputStream(sb.toString()));
        } else {
            StreamSource source = new StreamSource(new java.io.StringBufferInputStream(sb.toString()));
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(); 
            getTransformer().transform(source, new StreamResult(baos));
            view.setInputStream(new java.io.ByteArrayInputStream(baos.toByteArray()));
        }
        view.setMimeType(getMimeType(viewId));
        return view;
    }

    /**
     *
     */
    public ViewDescriptor[] getViewDescriptors() {
        return null;
    }

    /**
     *
     */
    private ResultSet getSearchResults(String queryString) throws Exception {
        String className = getResourceConfigProperty("parser");
        if (className == null) className = "org.wyona.meguni.parser.impl.MSNParser";
        Parser parser = (Parser) Class.forName(className).newInstance();
        return parser.parse(queryString);
    }

    /**
     * Results from Google, Facebook, ...
     */
    private StringBuffer getThirdPartyResults(String qs) {
        StringBuffer sb = new StringBuffer("");
        try {
            ResultSet resultSet = getSearchResults(qs + "+FOAF");

            if (resultSet != null && resultSet.size() > 0) {
                sb.append("<provider source-name=\"" + resultSet.getSourceName() + "\" source-domain=\"" + resultSet.getSourceDomain() + "\" numberOfResults=\"" + resultSet.size() + "\">");

                int numberOfDisplayedResults = 0;
                for (int k = 0;k < resultSet.size(); k++) {
                    java.net.URL url = resultSet.get(k).url;
                    if (url.toString().endsWith("rdf")) {
                        numberOfDisplayedResults = numberOfDisplayedResults + 1;
                        sb.append("<result number=\"" + (k+1) + "\" source-name=\"" + resultSet.getSourceName() + "\">");
                        sb.append("<title><![CDATA[" + resultSet.get(k).title + "]]></title>");
                        sb.append("<excerpt><![CDATA[" + resultSet.get(k).excerpt + "]]></excerpt>");
                        sb.append("<url><![CDATA[" + resultSet.get(k).url + "]]></url>");
                        sb.append("<last-modified><![CDATA[" + resultSet.get(k).lastModified + "]]></last-modified>");
                        sb.append("<mime-type suffix=\"rdf\">application/rdf+xml</mime-type>");
                        sb.append("</result>");
                    } else {
                        // TODO: Check mime type (application/rdf+xml) or take a look inside ...!
                        log.warn("Does not seem to be a RDF: " + url);
                    }
                }
                if (numberOfDisplayedResults < 1) {
                    sb.append("<no-results/>");
                }
                sb.append("</provider>");
            }
        } catch (Exception e) {
            sb.append("<exception>" + e.getMessage() + "</exception>");
        }
        return sb;
    }

    /**
     * Results from Wyona-FOAF
     */
    private StringBuffer getLocalResults(String qs) throws Exception {
        StringBuffer sb = new StringBuffer("");

        Repository pRepo = getProfilesRepository();
        Node[] pNodes = pRepo.getSearcher().search(qs);
        if (pNodes != null && pNodes.length > 0) {
            sb.append("<provider source-name=\"" + "Wyona-FOAF" + "\" source-domain=\"" + "http://foaf.wyona.org" + "\" numberOfResults=\"" + pNodes.length + "\">");
            for (int i = 0; i < pNodes.length; i++) {
                org.wyona.foaf.api.basics.Person person = new org.wyona.foaf.impl.basics.PersonImpl(pNodes[i].getInputStream());
                sb.append("<result number=\"" + "1" + "\" source-name=\"" + "Wyona-FOAF" + "\">");
                sb.append("<title><![CDATA[" + person.getName() + "]]></title>");
                sb.append("<excerpt><![CDATA[" + "About " + person.getName() + " ..." + "]]></excerpt>");
                sb.append("<url><![CDATA[" + "profiles/" + withoutSuffix(pNodes[i].getName()) + ".html" + "]]></url>");
                sb.append("<last-modified><![CDATA[" + "null" + "]]></last-modified>");
                sb.append("<mime-type suffix=\"html\">application/xhtml+xml</mime-type>");
                sb.append("</result>");
            }
        } else {
            sb.append("<provider source-name=\"" + "Wyona-FOAF" + "\" source-domain=\"" + "http://foaf.wyona.org" + "\" numberOfResults=\"" + "0" + "\">");
            sb.append("<no-results/>");
        }

// TODO: Implement Advanced Search
/*
            Nodes[] nodes = getRealm().getRepository().searchProperty("firstname", firstname);
            Nodes[] nodes = getRealm().getRepository().searchProperty("lastname", lastname);
*/

        sb.append("</provider>");
        return sb;
    }

    /**
     *
     */
    private Transformer getTransformer() throws Exception {
        File xsltFile = org.wyona.commons.io.FileUtil.file(rtd.getConfigFile().getParentFile().getAbsolutePath(), "xslt" + File.separator + "foaf2xhtml.xsl");
        Transformer tf = TransformerFactory.newInstance().newTransformer(new StreamSource(xsltFile));

        if (getRequest().getParameter("q") != null) {
            tf.setParameter("query", getRequest().getParameter("q"));
        }

        if (getResourceConfigProperty("type") != null) {
            tf.setParameter("type", getResourceConfigProperty("type"));
        } else {
            tf.setParameter("type", "simple");
        }

        String username = getUsername();
        if (username != null) tf.setParameter("username", username);

        return tf;
    }

    /**
     *
     */
    private Repository getProfilesRepository() throws Exception {
        Repository repoProfiles = ((org.wyona.yanel.impl.map.FOAFRealm) getRealm()).getProfilesRepository();
        if (repoProfiles != null) return repoProfiles;

        return getRealm().getRepository();
    }

    /**
     *
     */
    private String withoutSuffix(String name) {
        return name.substring(0, name.lastIndexOf("."));
    }

    /**
     * Get username from session
     */
    private String getUsername() {
        org.wyona.security.core.api.Identity identity = getEnvironment().getIdentity();
        if (identity != null) return identity.getUsername();
        return null;
    }
}
