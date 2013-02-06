/*
 * Copyright 2009 Wyona
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.wyona.org/licenses/APACHE-LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wyona.yanel.resources.konakart.forgotpw;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.management.timer.Timer;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.validator.EmailValidator;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wyona.commons.xml.XMLHelper;
import org.wyona.security.core.api.User;
import org.wyona.yanel.impl.resources.BasicXMLResource;
import org.wyona.yarep.core.Node;
import org.wyona.yarep.core.NodeType;


/**
 * This resource is responsible for managing the forgot password functionality.
 * The following constant control the flow with the UI.
 * {@value #SUBMITFORGOTPASSWORD}  is passed when the user clicks on the first screen to submit email.
 *
 * {@value #SUBMITNEWPW}  is passed when the user enter the new password and submits the form.
 *
 * If the query string has pwresetid then we know that the user clicked on the link sent via email.
 */
public class ForgotPassword extends BasicXMLResource {

    protected static Logger log = Logger.getLogger(ForgotPassword.class);
    protected long totalValidHrs;

    protected static final String PW_RESET_ID = "pwresetid";
    protected static final String SUBMITFORGOTPASSWORD = "submitForgotPW";
    protected static final String SUBMITNEWPW = "submitNewPW";
    protected static final String NAMESPACE = "http://www.wyona.org/yanel/1.0";
    protected static final long  DEFAULT_TOTAL_VALID_HRS = 24L;

    protected static final String SMTP_HOST_PROPERTY_NAME = "smtpHost";

    protected static final String HOURS_VALID_PROPERTY_NAME = "num-hrs-valid";

    /**
     * This is the main method that handles all view request. The first time the request
     * is made to enter the data.
     */
    @Override
    protected InputStream getContentXML(String viewId) throws Exception {
        HttpServletRequest request = getEnvironment().getRequest();

        try {
            String hrsValid = getResourceConfigProperty(HOURS_VALID_PROPERTY_NAME);
            if(hrsValid != null && !hrsValid.equals("")) {
                totalValidHrs = Long.parseLong(hrsValid);
            } else {
                totalValidHrs = DEFAULT_TOTAL_VALID_HRS ;
            }
        } catch(Exception ex) {
            log.error("num-hrs-valid flag not properly set: " + ex, ex);
            totalValidHrs = DEFAULT_TOTAL_VALID_HRS;
        }
        //Node xsltNode = getRealm().getRepository().getNode(getResourceConfigProperty("src"));
        Document adoc = XMLHelper.createDocument(NAMESPACE, "yanel-forgotpw");
        processUserAction(request, adoc);
        DOMSource source = new DOMSource(adoc);
        StringWriter xmlAsWriter = new StringWriter();
        StreamResult result = new StreamResult(xmlAsWriter);
        TransformerFactory.newInstance().newTransformer().transform(source, result);
        // write changes
        ByteArrayInputStream inputStream = new ByteArrayInputStream(xmlAsWriter.toString().getBytes("UTF-8"));
        return inputStream;
    }

    /**
     *
     */
    protected void processUserAction(HttpServletRequest request, Document adoc) throws Exception {
        String action = determineAction(request);
        log.debug("action performed: " + action);

        Element rootElement = adoc.getDocumentElement();

        String guid = request.getParameter(PW_RESET_ID);

        if (action.equals(SUBMITFORGOTPASSWORD)) {
            log.warn("DEBUG: Submit email address in order to receive request UUID...");
            String strVal = processForgotPW(request);
            Element statusElement = (Element) rootElement.appendChild(adoc.createElementNS(NAMESPACE, "show-message"));
            if(!strVal.equals("success")) {
                statusElement.setTextContent(strVal);
            } else {
                statusElement.setTextContent("request-sent");
            }
        } else if (guid != null && !guid.equals("") && !action.equals(SUBMITNEWPW) &&
                   request.getParameter("userid") != null && !request.getParameter("userid").equals("")) {
            log.warn("DEBUG: Request screen (by providing request UUID) in order to enter new password...");
            User usr = getUserForRequest(guid, totalValidHrs);
            if(usr == null) {
                Element statusElement = (Element) rootElement.appendChild(adoc.createElementNS(NAMESPACE, "show-message"));
                statusElement.setTextContent("no-such-request");
            } else {
                if (guid != null) {
                    Element requestpwElement = (Element) rootElement.appendChild(adoc.createElementNS(NAMESPACE, "requestnewpw"));
                    Element guidElement = (Element) requestpwElement.appendChild(adoc.createElementNS(NAMESPACE, "guid"));
                    guidElement.setTextContent(guid);
                }
            }
        } else if(action.equals(SUBMITNEWPW)) {
            log.warn("DEBUG: Submit new password...");
            String retStr = validateAndUpdatePassword(request); // INFO: Validate submitted passwords and if valid, then update password
            Element statusElement = (Element) rootElement.appendChild(adoc.createElementNS(NAMESPACE, "show-message"));
            if(!retStr.equals("success")) { // INFO: Password either did not match or were too short
                statusElement.setTextContent(retStr);
                if (guid != null) {
                    Element requestpwElement = (Element) rootElement.appendChild(adoc.createElementNS(NAMESPACE, "requestnewpw"));
                    Element guidElement = (Element) requestpwElement.appendChild(adoc.createElementNS(NAMESPACE, "guid"));
                    guidElement.setTextContent(guid);
                }
            } else {
                statusElement.setTextContent("success");
            }
        } else {
            log.warn("DEBUG: Request start screen in order to enter email address...");
            String smtpEmailServer = getResourceConfigProperty(SMTP_HOST_PROPERTY_NAME);
            if (smtpEmailServer != null || (getYanel().getSMTPHost() != null && getYanel().getSMTPPort() >= 0)) {
                rootElement.appendChild(adoc.createElementNS(NAMESPACE, "requestemail"));

/* TBD: Does this commented code make any sense?!
                String from = getResourceConfigProperty("smtpFrom");
                if (from != null) {
                    rootElement.appendChild(adoc.createElementNS(NAMESPACE, "requestemail"));
                } else {
                    Element exceptionElement = (Element) rootElement.appendChild(adoc.createElementNS(NAMESPACE, "exception"));
                    String resConfigFilename = "global-resource-configs/user-forgot-pw_yanel-rc.xml";
                    if (getConfiguration().getNode() != null) {
                        resConfigFilename = getConfiguration().getNode().getPath(); 
                    }
                    exceptionElement.setTextContent("The FROM address has not been configured yet. Please make sure to configure the FROM address within: " + resConfigFilename);
                }
*/

            } else {
                Element exceptionElement = (Element) rootElement.appendChild(adoc.createElementNS(NAMESPACE, "exception"));
                String resConfigFilename = "global-resource-configs/user-forgot-pw_yanel-rc.xml";
                if (getConfiguration().getNode() != null) {
                    resConfigFilename = getConfiguration().getNode().getPath(); 
                }
                exceptionElement.setTextContent("SMTP host has not been configured yet. Please make sure to configure the various smtp properties at: " + resConfigFilename + " (Or within WEB-INF/classes/yanel.xml)");
            }
        }
    }

    /**
     * Get user for a specific request ID
     * @param requestID Request ID, e.g. '5b1d7b4d-aa22-4858-9459-3776b7e5d354'
     */
    protected User getUserForRequest(String requestID, long duration_hour) throws Exception {
        log.warn("DEBUG: Find user for request with ID: " + requestID);
        if (getRealm().getRepository().existsNode(getPersistentRequestPath(requestID))) {
            Node requestNode = getRealm().getRepository().getNode(getPersistentRequestPath(requestID));

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = null;
            db = dbf.newDocumentBuilder();
            Document doc = db.parse(requestNode.getInputStream());
            Element rootElem = doc.getDocumentElement();

            Element requestTimeElem = org.wyona.commons.xml.XMLHelper.getChildElements(rootElem, "request-time", null)[0];
            long savedDateTime = new Long(requestTimeElem.getAttribute("millis")).longValue();
            log.warn("DEBUG: Request time: " + savedDateTime);
            if(isExpired(savedDateTime, duration_hour)) {
                log.warn("Request is expired");
                return null;
            }

            String userID = rootElem.getAttribute("id");
            return realm.getIdentityManager().getUserManager().getUser(userID);
        } else {
            log.warn("No such request ID: " + requestID);
            return null;
        }
    }

    /**
     * Check if request is expired
     */
    protected boolean isExpired(long starDT, long duration_hour) throws Exception {
        long currentDT = new Date().getTime();
        long expireTime= starDT + duration_hour * Timer.ONE_HOUR;

        return (expireTime < currentDT);
    }

    protected String getTextValue(Element ele, String tagName) throws Exception {
        String textVal = null;
        NodeList nl = ele.getElementsByTagName(tagName);
        if(nl != null && nl.getLength() > 0) {
            Element el = (Element)nl.item(0);
            textVal = el.getFirstChild().getNodeValue();
        }

        return textVal;
    }


    /**
     * Determine the requested view: defaultView, submitProfile, submitPassword,submitGroup, submitDelete
     *
     * @param request
     * @return name of the desired view
     */
    protected String determineAction(HttpServletRequest request) throws Exception {
       boolean submit = false;
       String action = "defaultView";

       Enumeration<?> enumeration = request.getParameterNames();
       while (enumeration.hasMoreElements() && !submit) {
           action = enumeration.nextElement().toString();
           if (action.startsWith("submit")) {
               submit = true;
           }
       }
       return action;
   }

    /**
     * Updates the user profile
     *
     * @param request The request containing the data to update
     */
    protected String processForgotPW(HttpServletRequest request) throws Exception {
        String email = request.getParameter("email");
        if(email == null || email.length() <= 5) {
            return "email-not-valid";
        } else if (! EmailValidator.getInstance().isValid(email)) {
            return "email-not-valid";
        } else {
            User[] userList = realm.getIdentityManager().getUserManager().getUsers(true);
            for(int i=0; i< userList.length; i++) {
                if (userList[i].getEmail().equals(email)) {
                    String guid = UUID.randomUUID().toString();

                    ResetPWExpire pwexp = new ResetPWExpire(userList[i].getID(), new Date().getTime(), guid, userList[i].getEmail());

                    sendEmail(guid, userList[i].getEmail());

                    writeXMLOutput(getPersistentRequestPath(guid), generateXML(pwexp));
                    //writeXMLOutput(getResetPasswordRequestsBasePath() + "/" + pwexp.getUserId() + ".xml", generateXML(pwexp));
                    return "success";
                }
            }
            return "email-not-valid";
        }
    }

    /**
     * Generate XML containing request information which will be saved persistently
     */
    protected String generateXML(ResetPWExpire resetObj) throws Exception {
        org.w3c.dom.Document adoc = org.wyona.commons.xml.XMLHelper.createDocument(NAMESPACE, "user");
        Element userElement = adoc.getDocumentElement();
        userElement.setAttribute("id", resetObj.getUserId());

        Element emailElement = (Element) userElement.appendChild(adoc.createElement("email"));
        emailElement.setTextContent(resetObj.getEmail());

        Element startTimeElement = (Element) userElement.appendChild(adoc.createElement("request-time"));
        startTimeElement.setAttribute("millis", Long.toString(resetObj.getDateTime()));
        java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        startTimeElement.setTextContent(dateFormat.format(resetObj.getDateTime()));

        Element guidElement = (Element) userElement.appendChild(adoc.createElement("guid"));
        guidElement.setTextContent(resetObj.getGuid());


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer t = factory.newTransformer(); // identity transform
        DOMSource source = new DOMSource(adoc);
        StreamResult result = new StreamResult(baos);
        t.transform(source, result);

        return baos.toString();
    }

    /**
     * Write reset password request into Yarep node
     * @param path Yarep node path
     * @param content XML content
     */
    protected void writeXMLOutput(String path, String content) throws Exception {
        Node fileToStore = null;
        if (getRealm().getRepository().existsNode(path)) {
            fileToStore = getRealm().getRepository().getNode(path);
        } else {
            fileToStore = getRealm().getRepository().getRootNode().addNode(path, NodeType.RESOURCE);
        }
        InputStream in = new ByteArrayInputStream(content.getBytes());
        OutputStream out = fileToStore.getOutputStream();
        byte buffer[] = new byte[8192];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        out.close();
        in.close();
    }

    /**
     * Validate and change user password.
     */
    protected String validateAndUpdatePassword(HttpServletRequest request) throws Exception {
        String plainPassword = request.getParameter("newPassword");
        boolean confirmation = plainPassword.equals(request.getParameter("newPasswordConfirmation"));
        if (confirmation && !plainPassword.equals("")) {
            String guid = request.getParameter("guid");
            User user = getUserForRequest(guid, totalValidHrs);
            if(user !=null) {
                user.setPassword(plainPassword);
                user.save();
                getRealm().getRepository().delete(new org.wyona.yarep.core.Path(getPersistentRequestPath(guid))); // DEPRECATED
                //TODO: YarepUtil.deleteNode(getRealm().getRepository(), getPersistentRequestPath(guid));
                return "success";
            } else {
                return "Unable to find user for password reset.";
            }
        } else {
            return "Either no new password was supplied or the password supplied and its confirmation password do not match.";
        }
    }

    @Override
    public boolean exists() throws Exception {
        return true;
    }

    /**
     * Get forgot password URL which will be sent via E-Mail (also see YanelServlet#getRequestURLQS(HttpServletRequest, String, boolean))
     */
    public String getURL() throws Exception {
        //https://192.168.1.69:8443/yanel" + request.getServletPath().toString()
        URL url = new URL(request.getRequestURL().toString());
        org.wyona.yanel.core.map.Realm realm = getRealm();
        if (realm.isProxySet()) {
            // TODO: Finish proxy settings replacement

            String proxyHostName = realm.getProxyHostName();
            log.warn("DEBUG: Proxy host name: " + proxyHostName);
            if (proxyHostName != null) {
                url = new URL(url.getProtocol(), proxyHostName, url.getPort(), url.getFile());
            }

            int proxyPort = realm.getProxyPort();
            if (proxyPort >= 0) {
                url = new URL(url.getProtocol(), url.getHost(), proxyPort, url.getFile());
            } else {
                url = new URL(url.getProtocol(), url.getHost(), url.getDefaultPort(), url.getFile());
            }

            String proxyPrefix = realm.getProxyPrefix();
            if (proxyPrefix != null) {
                url = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile().substring(proxyPrefix.length()));
            }
        } else {
            log.warn("No proxy set.");
        }
        return url.toString();
    }

    /**
     * Get base path (collection path) where reset password requests will be saved permanently
     */
    protected String getResetPasswordRequestsBasePath() throws Exception {
        String configuredBasePath = getResourceConfigProperty("change-password-requests-path");
        String basePath;
        if (configuredBasePath != null) {
            if (!configuredBasePath.startsWith("/")) {
                basePath = "/" + configuredBasePath;
            } else {
                basePath = configuredBasePath;
            }
        } else {
            String DEFAULT_BASE_PATH = "/reset-password-requests";
            log.warn("No base path configured. Will use default value: " + DEFAULT_BASE_PATH);
            basePath = DEFAULT_BASE_PATH;
        }
        return basePath;
    }

    /**
     * Send email to user requesting to reset the password
     */
    protected void sendEmail(String guid, String emailAddress) throws Exception {
        String emailSubject = "Reset password request needs your confirmation";
        String emailBody = "Please go to the following URL to reset password: <" + getURL() + "?" + PW_RESET_ID + "=" + guid + ">.";
        String hrsValid = getResourceConfigProperty(HOURS_VALID_PROPERTY_NAME);
        emailBody = emailBody + "\n\nNOTE: This link is only available during the next " + hrsValid + " hours!";
        if (log.isDebugEnabled()) log.debug(emailBody);
        String from = getResourceConfigProperty("smtpFrom");
        if (from == null) {
            log.error("The FROM address has not been configured yet. Please make sure to configure the FROM address inside the resource configuration!");
        }
        String to =  emailAddress;

        String emailServer = getResourceConfigProperty(SMTP_HOST_PROPERTY_NAME);
        if (emailServer != null) {
            int port = Integer.parseInt(getResourceConfigProperty("smtpPort"));
            org.wyona.yanel.core.util.MailUtil.send(emailServer, port, from, to, emailSubject, emailBody);
        } else {
            org.wyona.yanel.core.util.MailUtil.send(from, to, emailSubject, emailBody);
        }
    }

    /**
     *
     */
    protected String getPersistentRequestPath(String guid) throws Exception {
        return getResetPasswordRequestsBasePath() + "/" + guid + ".xml";
    }
}
