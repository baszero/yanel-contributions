package org.wyona.konakart.model.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

import org.wyona.yarep.core.Map;
import org.wyona.yarep.core.NoSuchRevisionException;
import org.wyona.yarep.core.Node;
import org.wyona.yarep.core.NodeStateException;
import org.wyona.yarep.core.NodeType;
import org.wyona.yarep.core.Path;
import org.wyona.yarep.core.Property;
import org.wyona.yarep.core.Repository;
import org.wyona.yarep.core.RepositoryException;
import org.wyona.yarep.core.Revision;
import org.wyona.yarep.core.Storage;
import org.wyona.yarep.core.UID;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Yarep node representing a KonaKart order node using JDBC to retrieve data
 */
public class KonakartOrderJDBCNode extends org.wyona.yarep.impl.AbstractNode {
    private static Logger log = Logger.getLogger(KonakartOrderJDBCNode.class);
    
    public KonakartOrderJDBCNode(Repository repo, String path, String uuid) throws RepositoryException {
        super(repo, path, uuid);
        properties = new java.util.HashMap();
    }

    public Iterator getIteratorOfNodes() throws RepositoryException {
        log.warn("TODO: Not implemented yet!");
        return null;
    }

    public Node[] getNodes() throws RepositoryException {
        log.warn("TODO: Not implemented yet!");
        return null;
    }
    
    public Node addNode(String name, int type) throws RepositoryException {
        log.warn("TODO: Not implemented yet!");
        return null;
    }
    
    public void removeProperty(String name) throws RepositoryException {
        log.warn("TODO: Not implemented yet!");
    }

    /**
     * @see org.wyona.yarep.core.Node#getProperty(String)
     */
    public Property getProperty(String name) throws RepositoryException {
/*
        //log.debug("Property: " + property.getName() + ", " + property.getValueAsString());
        properties.put(property.getName(), property);
*/
        int languageID = ((KonakartJDBCRepository)repository).getLanguageId("de");
        log.warn("DEBUG: Language ID: " + languageID);

        String query = "SELECT * FROM orders WHERE orders_id = '" + getName() + "'";
        //String query = "SELECT * FROM orders WHERE orders_id = '" + getName() + "' AND language_id = '" + languageID + "'";
        try {
            Connection con = ((KonakartJDBCRepository)repository).getConnection();
            Statement stm = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet resultSet = stm.executeQuery(query);
            if(resultSet.next()) { // TODO: filter by language
            //if(resultSet.last() && resultSet.getRow() == 1) {
                String id = resultSet.getString("orders_id");
                log.warn("DEBUG: Order ID: " + id);
                //log.debug("Column count: " + resultSet.getMetaData().getColumnCount());
                
                org.wyona.yarep.core.Property property;
                if (name.equals("store-id")) {
                    property = new org.wyona.yarep.impl.DefaultProperty(name, org.wyona.yarep.core.PropertyType.STRING, this);
                    property.setValue(resultSet.getString("store_id"));
                } else if (name.equals("customer-name")) {
                    property = new org.wyona.yarep.impl.DefaultProperty(name, org.wyona.yarep.core.PropertyType.STRING, this);
                    property.setValue(resultSet.getString("customers_name"));
                } else if (name.equals("customer-id")) {
                    property = new org.wyona.yarep.impl.DefaultProperty(name, org.wyona.yarep.core.PropertyType.STRING, this);
                    property.setValue(resultSet.getString("customers_id"));
                } else {
                    throw new RepositoryException("No such property: " + name);
                }
                return property;
            } else {
                log.warn("No such order: " + query);
            }
            resultSet.close();
            stm.close();
            con.close();
            throw new RepositoryException("No such order: " + query);
        } catch(SQLException e) {
            log.error(e, e);
            throw new RepositoryException(e.getMessage());
        }
    }

    /**
     * @see org.wyona.yarep.core.Node#setProperty(Property)
     */
    public void setProperty(Property property) throws RepositoryException {
        //log.debug("Property: " + property.getName() + ", " + property.getValueAsString());
        properties.put(property.getName(), property);

        int languageID = ((KonakartJDBCRepository)repository).getLanguageId("de");
        log.warn("DEBUG: Language ID: " + languageID);

        if (property.getName().equals("store-id")) {
            updateStoreID(property.getValueAsString(), "orders");
            updateStoreID(property.getValueAsString(), "orders_products");
            updateStoreID(property.getValueAsString(), "orders_products_attributes");
            updateStoreID(property.getValueAsString(), "orders_status_history");
            updateStoreID(property.getValueAsString(), "orders_total");
        } else {
            throw new RepositoryException("No such property: " + name);
        }
    }

    /**
     * Update store ID
     * @param storeId Store ID
     * @param tableName Name of database table
     */
    private void updateStoreID(String storeId, String tableName) throws RepositoryException {
        log.warn("DEBUG: Try to update store id '" + storeId + "' of order ID '" + getName() + "' within table '" + tableName + "'.");

        int languageID = ((KonakartJDBCRepository)repository).getLanguageId("de");
        log.warn("DEBUG: Language ID: " + languageID);

        String query = "SELECT * FROM " + tableName + " WHERE orders_id = " + getName();
        //String query = "SELECT * FROM orders WHERE orders_id = '" + getName() + "' AND language_id = '" + languageID + "'";
        try {
            Connection con = ((KonakartJDBCRepository)repository).getConnection();
            Statement stm = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ResultSet resultSet = stm.executeQuery(query);
            while(resultSet.next()) { // TODO: filter by language
            //if(resultSet.last() && resultSet.getRow() == 1) {
                String id = resultSet.getString("orders_id");
                log.warn("DEBUG: Update store id '" + storeId + "' of order ID '" + id + "' within table '" + tableName + "'.");
                //log.debug("Column count: " + resultSet.getMetaData().getColumnCount());
                
                resultSet.updateString("store_id", storeId);
                resultSet.updateRow();
            } /*else {
                log.warn("No such order: " + query);
            }*/
            resultSet.close();
            stm.close();
            con.close();
        } catch(SQLException e) {
            log.error(e, e);
            throw new RepositoryException(e.getMessage());
        }
    }

    /**
     *
     */
    public InputStream getInputStream() throws RepositoryException {
        log.warn("TODO: Not implemented yet!");
        return null;
    }
    
    public OutputStream getOutputStream() throws RepositoryException {
        log.warn("TODO: Not implemented yet!");
        return null;
    }
    
    public Revision checkin() throws NodeStateException, RepositoryException {
        log.warn("TODO: Not implemented yet!");
        return null;
    }
    
    public Revision checkin(String comment) throws NodeStateException, RepositoryException {
        log.warn("TODO: Not implemented yet!");
        return null;
    }

    public void cancelCheckout() throws NodeStateException, RepositoryException {
        log.warn("TODO: Not implemented yet!");
    }

    public void checkout(String userID) throws NodeStateException, RepositoryException {
        log.warn("TODO: Not implemented yet!");
    }
    
    public void restore(String revisionName) throws NoSuchRevisionException, RepositoryException {
        log.warn("TODO: Not implemented yet!");
    }

    public void delete() throws RepositoryException {
        log.warn("TODO: Not implemented yet!");
    }
}
