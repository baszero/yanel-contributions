/*-
 * Copyright 2010 Wyona
 */

package org.wyona.yanel.resources.konakart.forgotpw;

import java.util.UUID;
import java.util.Date;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.wyona.yarep.core.*;
import org.apache.log4j.Logger;
import com.konakart.appif.KKEngIf;
import com.konakart.appif.NameValueIf;
import com.konakart.appif.EmailOptionsIf;
import com.konakart.app.NameValue;
import com.konakart.app.EmailOptions;
import org.wyona.security.core.api.User;
import javax.servlet.http.HttpServletRequest;
import org.wyona.yanel.resources.konakart.shared.SharedResource;

public class KonakartForgotPwSOAPInfResource extends ForgotPassword {
    private static String KONAKART_NAMESPACE = "http://www.konakart.com/1.0";
    
    private KKEngIf kkEngine;
    private int languageId;
    private int customerId;
    private String sessionId;

    protected InputStream getContentXML(String viewId) throws Exception {
        // Set up variables
        SharedResource shared = new SharedResource();
        kkEngine = shared.getKonakartEngineImpl();
        languageId = shared.getLanguageId(getContentLanguage());
        customerId = shared.getTemporaryCustomerId(getEnvironment().getRequest().getSession(true));
        sessionId = shared.getSessionId(getEnvironment().getRequest().getSession(true));

        // Call parent
        return super.getContentXML(viewId);
    }

    /**
     * Send email to user requesting to reset the password
     */
    protected void sendEmail(String guid, String emailAddress) throws Exception {
        NameValueIf[] nvs = new NameValue[1];
        nvs[0] = new NameValue();
        nvs[0].setName("pwresetid");
        nvs[0].setValue(guid);
        EmailOptionsIf opts = new EmailOptions();
        opts.setCustomAttrs(nvs);
        opts.setCountryCode(getContentLanguage());
        opts.setTemplateName("EmailNewPassword");

        kkEngine.sendNewPassword1(emailAddress, opts);
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
        } else if(kkEngine.doesCustomerExistForEmail(email)) {
            return super.processForgotPW(request);
        } else {
            return "email-not-valid";
        }
    }

    /**
     * @see org.wyona.yanel.resources.konakart.forgotpw.ForgotPassword#validatAndUpdatePassword(HttpServletRequest)
     */
    @Override
    protected String validateAndUpdatePassword(HttpServletRequest request) throws Exception {
        String plainPassword = request.getParameter("newPassword");
        boolean confirmation = plainPassword.equals(request.getParameter("newPasswordConfirmation"));
        if(confirmation && plainPassword.length() >= 5) {
            String guid = request.getParameter("guid");
            User user = getUserForRequest(guid, totalValidHrs);
            String konakartPw = request.getParameter("userid");
            if(user !=null && konakartPw != null) {
                sessionId = kkEngine.login(user.getEmail(), konakartPw);
                if(sessionId != null) {
                    kkEngine.changePassword(sessionId, konakartPw, plainPassword);
                } else {
                    return "no-such-request";
                }
                user.setPassword(plainPassword);
                user.save();
                Node n = getRealm().getRepository().getNode((getPersistentRequestPath(guid)));
                n.delete();
                return "success";
            } else {
                return "no-such-request";
            }
        } else {
            if(plainPassword.length() < 5) return "password-too-short";
            return "password-mismatch";
        }
    }
}