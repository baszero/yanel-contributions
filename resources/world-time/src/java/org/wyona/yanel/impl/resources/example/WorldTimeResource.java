/*
 * Copyright 2006 Wyona
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

package org.wyona.yanel.impl.resources.example;

import java.util.Calendar;
import java.util.HashMap;
import java.io.StringBufferInputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Category;

import org.wyona.yanel.core.Path;
import org.wyona.yanel.core.Resource;
import org.wyona.yanel.core.api.attributes.CreatableV2;
import org.wyona.yanel.core.api.attributes.ViewableV1;
import org.wyona.yanel.core.attributes.viewable.View;
import org.wyona.yanel.core.attributes.viewable.ViewDescriptor;

/**
 * Simple resource example displaying the time
 */
public class WorldTimeResource extends Resource implements ViewableV1, CreatableV2 {

    private static Category log = Category.getInstance(WorldTimeResource.class);

    /**
     * 
     */
    public WorldTimeResource() {
    }

    /**
     * 
     */
    public ViewDescriptor[] getViewDescriptors() {
        log.warn("Not implemented yet!");
        return null;
    }
    
    /**
     * 
     */
    public View getView(Path path, String viewId) {
        View view = new View();
        StringBuffer sb = new StringBuffer(getTime());
        view.setInputStream(new StringBufferInputStream(sb.toString()));
        view.setMimeType("text/plain");
        return view;
    }

    /**
     * 
     */
    public View getView(HttpServletRequest request, String viewId) throws Exception {
        View view = new View();
        StringBuffer sb = new StringBuffer("<?xml version=\"1.0\"?>");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
        sb.append("<head>");
        sb.append("<title>World Time Resource</title>");
        sb.append("</head>");
        sb.append("<body>");
        sb.append("<div id=\"contenBody\">");
        sb.append("<h1>" + getTime() + "</h1>");
        sb.append("</div>");
        sb.append("</body>");
        sb.append("</html>");

        view.setMimeType("application/xhtml+xml");
        view.setInputStream(new StringBufferInputStream(sb.toString()));
        return view;

    }

    /**
     *
     */
    private String getTime(){
        Calendar cal = Calendar.getInstance(java.util.TimeZone.getDefault());
      
        String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(DATE_FORMAT);

        sdf.setTimeZone(java.util.TimeZone.getDefault());          
            
        return sdf.format(cal.getTime());
    }

    /**
     *
     */
    public HashMap createRTIProperties(HttpServletRequest request) {
        return null;
    }

    /**
     *
     */
    public void create(HttpServletRequest request) {
        Path newPath = new Path(getPath());
        log.error("DEBUG: New path: " + newPath);
    }

    /**
     *
     */
    public String getPropertyType(String propertyName) {
        log.warn("No properties!");
        return null;
    }

    /**
     *
     */
    public Object getProperty(String name) {
        log.warn("No properties!");
        return null;
    }

    /**
     *
     */
    public void setProperty(String name, Object value) {
        log.warn("No properties!");
    }

    /**
     *
     */
    public String[] getPropertyNames() {
        log.warn("No properties!");
        return null;
    }

    /**
     *
     */
    public String getCreateName(String suggestedName) {
        return null;
    }
}
