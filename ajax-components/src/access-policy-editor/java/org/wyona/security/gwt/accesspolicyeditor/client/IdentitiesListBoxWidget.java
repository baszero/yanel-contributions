/*
 * Copyright 2008 Wyona
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.wyona.security.gwt.accesspolicyeditor.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 *
 */
public class IdentitiesListBoxWidget extends Composite implements ClickListener {

    private ListBox lb;

    private VerticalPanel vp = new VerticalPanel();

    /**
     *
     */
    public IdentitiesListBoxWidget(int visibleItemCount, String[] users, String[] groups) {
        initWidget(vp);

        vp.add(new Label("Identities"));

        lb = new ListBox(true);
        lb.addClickListener(this);
        set(visibleItemCount, users, groups);
        vp.add(lb);
    }

    /**
     *
     */
    public void set(int visibleItemCount, String[] users, String[] groups) {
        lb.clear();
        lb.setVisibleItemCount(visibleItemCount);
        if (users != null) {
            for (int i = 0; i < users.length; i++) {
                lb.addItem("u: " + users[i]);
            }
        } else {
            lb.addItem("No users yet!");
        }
        if (groups != null) {
            for (int i = 0; i < groups.length; i++) {
                lb.addItem("g: " + groups[i]);
            }
        } else {
            lb.addItem("No groups yet!");
        }
    }

    /**
     *
     */
    public ListBox getListBox() {
        return lb;
    }

    /**
     *
     */
    public void onClick(Widget sender) {
        //Window.alert("An identity has been selected!");
    }
}
