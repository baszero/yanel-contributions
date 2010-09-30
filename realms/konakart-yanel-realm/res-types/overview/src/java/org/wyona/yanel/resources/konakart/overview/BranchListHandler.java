/*
 * Copyright 2010 Wyona
 */

package org.wyona.yanel.resources.konakart.overview;

import java.lang.Integer;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

class BranchListHandler extends DefaultHandler {
    private String element;
    private int lower_bound;
    private int zip;
    public String recipient;

    BranchListHandler(String zip) {
        this.zip = Integer.parseInt(zip);
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException { 
        element = localName;

        if("branch".equalsIgnoreCase(element)) {
           lower_bound = Integer.parseInt(attributes.getValue("start"));
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if("branch".equalsIgnoreCase(element)) { 
            if(zip >= lower_bound) {
                recipient = new String(ch, start, length); 
            }
        }

        element = null;
    }
}
