package org.wyona.yanel.navigation.gwt.lookuptree.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.Resizable;
import com.gwtext.client.widgets.ResizableConfig;
import com.gwtext.client.widgets.event.ResizableListenerAdapter;
import com.gwtext.client.widgets.grid.GridPanel;
import com.gwtext.client.widgets.grid.event.GridRowListenerAdapter;
import com.gwtext.client.widgets.tree.TreeNode;
import com.gwtext.client.widgets.tree.TreePanel;
import com.gwtext.client.widgets.tree.event.TreePanelListenerAdapter;

public class LookupTree implements EntryPoint {

    private String lookupPanelBorder = "false";
    private String lookupPanelPadding = "15";
    private String treePanelWidth = "190";
    private String treepanelHeight = "400";
    private String gridPanelWidth = "190";
    private String gridpanelHeight = "400";
    private String lookupRootNodeLabel = "/";
    private String lookupHook = "lookupHook";
    private String requestParameterType = "";
    private String actionUrl = "";
    private String submitLabel = "submit";
    private String createFolderNameDefault = "New Folder";
    private String createFolderSubmitLabel = "create new Folder";
    private String currentPath = "/";
    private String currentPathLabelString = "Path: ";
    private boolean showUpload = true;
    private boolean showCreateFolder = true;
    

    public void onModuleLoad() {
        // Get config from host/html page via json var 'lookupTreeConfig'
        try {
            Dictionary dict = Dictionary.getDictionary("lookupTreeConfig");
            lookupPanelBorder = dict.get("lookup-panel-border");
            lookupPanelPadding = dict.get("lookup-panel-padding");
            treePanelWidth = dict.get("lookup-treepanel-width");
            treepanelHeight = dict.get("lookup-treepanel-height");
            gridPanelWidth = dict.get("lookup-gridpanel-width");
            gridpanelHeight = dict.get("lookup-gridpanel-height");
            lookupRootNodeLabel = dict.get("lookup-root-node-label");
            lookupHook = dict.get("lookup-hook");
            requestParameterType = dict.get("lookup-request-paramter-type");
            actionUrl = dict.get("lookup-upload-action-url");
            createFolderNameDefault = dict.get("lookup-create-folder-name-default");
            createFolderSubmitLabel = dict.get("lookup-create-folder-submit-label");
            submitLabel = dict.get("lookup-upload-submit-button-label");
            currentPathLabelString = dict.get("lookup-current-path-label");
            showUpload = new Boolean(dict.get("lookup-upload-enabled")).booleanValue();
            showCreateFolder = new Boolean(dict.get("lookup-create-folder-enabled")).booleanValue();
        } catch (java.util.MissingResourceException e) {
            // just use default values
        }
        
        final Panel panel = new Panel();
        final LookupGrid grid = new LookupGrid(requestParameterType);
        
        panel.setBorder(new Boolean(lookupPanelBorder).booleanValue());
        panel.setPaddings(Integer.parseInt(lookupPanelPadding));
        
        grid.setWidth(Integer.parseInt(gridPanelWidth));  
        grid.setHeight(Integer.parseInt(gridpanelHeight)); 
        
        grid.addGridRowListener(new GridRowListenerAdapter(){
            public void onRowClick(GridPanel grid,
                    int rowIndex,
                    EventObject e){
                onNodeClick(grid.getStore().getAt(rowIndex).getAsString("id"));
            }

        });

        final Label currentPathLabel = new Label(this.currentPathLabelString + currentPath);
        
        ResizableConfig config = new ResizableConfig();  
        config.setHandles(Resizable.SOUTH_EAST); 
        
        final Resizable resizable = new Resizable(grid, config);  
        resizable.addListener(new ResizableListenerAdapter() {  
            public void onResize(Resizable self, int width, int height) {  
                grid.setWidth(width);  
                grid.setHeight(height);  
            }  
        }); 
        
        final LookupCreatFolderPanel createFolderPanel = new LookupCreatFolderPanel(actionUrl, createFolderSubmitLabel, createFolderNameDefault);
        createFolderPanel.setHeight("30px");
        createFolderPanel.setGrid(grid);
        
        final LookupUploadPanel form = new LookupUploadPanel(actionUrl, submitLabel);
        form.setGrid(grid);
        final VerticalPanel verticalPanel = new VerticalPanel();
        if (showUpload) {
            form.setHeight("30px");
            verticalPanel.add(form);
        }
        
        final TreePanel treePanel = new LookupTreePanel(lookupRootNodeLabel, requestParameterType);
        treePanel.setEnableDD(false);
        treePanel.setContainerScroll(true);
        treePanel.setAutoScroll(true);
        treePanel.setWidth(Integer.parseInt(treePanelWidth));
        treePanel.setHeight(Integer.parseInt(treepanelHeight));
        treePanel.addListener(new TreePanelListenerAdapter() {
            public void onClick(TreeNode node, EventObject e) {
                currentPath = node.getId();
                grid.setCurrentPath(currentPath);
                grid.updateData();
                form.setCurrentPath(currentPath);
                createFolderPanel.setCurrentPath(currentPath);
                currentPathLabel.setText(currentPathLabelString + currentPath);
            }
        });
            
        final Resizable resizableTreePanel = new Resizable(treePanel, config);  
        resizableTreePanel.addListener(new ResizableListenerAdapter() {  
            public void onResize(Resizable self, int width, int height) {  
                treePanel.setWidth(width);  
                treePanel.setHeight(height);  
            }  
        }); 
        

        
        final HorizontalPanel horizontalPanel = new HorizontalPanel();
        final VerticalPanel gridAndPathPanel = new VerticalPanel();
        
        horizontalPanel.add(treePanel);
        gridAndPathPanel.add(currentPathLabel);
        gridAndPathPanel.add(grid);
        if(showCreateFolder) {
            verticalPanel.add(createFolderPanel);
        }
        horizontalPanel.add(gridAndPathPanel);

        verticalPanel.add(horizontalPanel);
        panel.add(verticalPanel);
        RootPanel.get(lookupHook).add(panel);
    }

    public native void onNodeClick(final String path)/*-{
        $wnd.callback(path); 
    }-*/;
    
}