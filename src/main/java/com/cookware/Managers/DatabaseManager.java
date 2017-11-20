package com.cookware.Managers;

import org.apache.log4j.Logger;
import com.cookware.Tools.DirectoryTools;

import java.math.BigInteger;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kody on 8/09/2017.
 */
public class DatabaseManager {
    private DirectoryTools directoryTools = new DirectoryTools();
    private final Logger log = Logger.getLogger(DatabaseManager.class);
    private final List<DatabaseEntryAttribute> databaseAttributes  = new ArrayList<DatabaseEntryAttribute>();
    private String databasePath;
    private String driverPath;

    public DatabaseManager(String mFilePath) {
        this.databasePath = mFilePath;
        this.driverPath = "jdbc:sqlite:" + this.databasePath + "/media.db";
    }

    public void initialise(String table) {
        // TODO: Re-sort Database entries to make it easier to read
        databaseAttributes.add(new DatabaseEntryAttribute("ID", "BIGINT NOT NULL", BigInteger.class));
        databaseAttributes.add(new DatabaseEntryAttribute("NAME", "TEXT NOT NULL", String.class));
        databaseAttributes.add(new DatabaseEntryAttribute("TYPE", "TINYINT NOT NULL", Integer.class));
        databaseAttributes.add(new DatabaseEntryAttribute("URL", "TEXT NOT NULL", String.class));
        databaseAttributes.add(new DatabaseEntryAttribute("QUALITY", "SMALLINT NOT NULL", Integer.class));
        databaseAttributes.add(new DatabaseEntryAttribute("STATE", "TINYINT NOT NULL", Integer.class));
        databaseAttributes.add(new DatabaseEntryAttribute("PRIORITY", "TINYINT NOT NULL", Integer.class));
        databaseAttributes.add(new DatabaseEntryAttribute("RELEASED", "DATE NOT NULL", LocalDate.class));
        databaseAttributes.add(new DatabaseEntryAttribute("ADDED", "DATE NOT NULL", LocalDate.class));
        databaseAttributes.add(new DatabaseEntryAttribute("DOWNLOADED", "DATE", LocalDate.class));
        databaseAttributes.add(new DatabaseEntryAttribute("PATH", "TEXT", String.class));
        databaseAttributes.add(new DatabaseEntryAttribute("PARENTID", "BIGINT", BigInteger.class));
        databaseAttributes.add(new DatabaseEntryAttribute("PARENTNAME", "TEXT", String.class));
        databaseAttributes.add(new DatabaseEntryAttribute("EPISODE", "TEXT", float.class));

        directoryTools.createNewDirectory(databasePath);

        initialiseDatabase(table);
    }


    private void initialiseDatabase(String table){
        boolean querySucceeded = false;

        if(!checkIfDatabaseExists(table)) {
            String sql = String.format("CREATE TABLE %s(", table);
            for (DatabaseEntryAttribute currentDatabaseEntryAttribute : databaseAttributes) {
                sql += String.format("%s %s, ", currentDatabaseEntryAttribute.NAME, currentDatabaseEntryAttribute.INITPARAM);
            }
            sql += String.format("PRIMARY KEY (%s))", databaseAttributes.get(0).NAME);

            querySucceeded = sendSqlQuery(sql);

            if (!querySucceeded) {
                log.error("Failed to initialise Database");
                log.error("shutting down because data base could not be initialised");
                System.exit(1);
            }
            log.error("Created new Database");
        }
        log.info("Successfully opened database");
    }


    public void updateDatabaseValue(String table, String id, String key, Object value){
        DatabaseEntryAttribute associatedDatabaseEntryAttribute = getDatabaseAttributeByName(key);
        String databaseAttributeValueString = getDatabaseAttributeValueAsString(associatedDatabaseEntryAttribute, value);

        final String sql = String.format("UPDATE %s SET %s = %s where ID = %d ;",
                table,
                associatedDatabaseEntryAttribute.NAME,
                databaseAttributeValueString,
                id);

        final boolean querySucceeded = sendSqlQuery(sql);

        if (!querySucceeded) {
            log.error(String.format("Failed to update Database ID: %d Attribute: %s Value: %s",id, key, databaseAttributeValueString));
            log.error("Shutting down due to lack of database access");
            System.exit(1);
        }
    }


    private String getDatabaseAttributeValueAsString(DatabaseEntryAttribute associatedDatabaseEntryAttribute, Object value) {
        try {
            if (associatedDatabaseEntryAttribute.DATATYPE.equals(String.class)) {
                return String.format("'%s'", ((String) value).replace("'", "''"));
            }
            if (associatedDatabaseEntryAttribute.DATATYPE.equals(BigInteger.class)) {
                return String.format("%d", (BigInteger) value);
            }
            if (associatedDatabaseEntryAttribute.DATATYPE.equals(Integer.class)) {
                return String.format("%d", (Integer) value);
            }
            if (associatedDatabaseEntryAttribute.DATATYPE.equals(LocalDate.class)) {
                return String.format("'%s'", Date.valueOf((LocalDate) value));
            }
            if (associatedDatabaseEntryAttribute.DATATYPE.equals(float.class)) {
                return String.format("%02f", (float) value);
            }
        }
        catch (ClassCastException e){
            log.error("Failed to cast value", e);
        }
        log.error("Couldn't convert database attribute to String");
        return null;
    }


    private DatabaseEntryAttribute getDatabaseAttributeByName(String name){
        DatabaseEntryAttribute associatedDatebaseEntryAttribute;
        for(DatabaseEntryAttribute currentDatabaseEntryAttribute: databaseAttributes){
            if (currentDatabaseEntryAttribute.NAME.equals(name)){
                associatedDatebaseEntryAttribute = currentDatabaseEntryAttribute;
                return associatedDatebaseEntryAttribute;
            }
        }
        log.error(String.format("No database entry attribute found for %s", name));
        return null;
    }


    public String[] getEntryItemValue(String table,  String id, String key){
        String query = String.format("SELECT %s " +
                "FROM %s " +
                "WHERE ID = %d ;",key, table, id);

        return receiveSqlRequest(query);
    }


    public String[] getAllEntriesWithMatchedCriteria(String table, String key, Object value){
        final DatabaseEntryAttribute associatedDatabaseEntryAttribute = getDatabaseAttributeByName(key);
        final String databaseAttributeValueString = getDatabaseAttributeValueAsString(associatedDatabaseEntryAttribute, value);

        if(associatedDatabaseEntryAttribute != null) {
            final String query = String.format("SELECT * " +
                            "FROM %s " +
                            "WHERE %s = %s",
                    table,
                    associatedDatabaseEntryAttribute.NAME,
                    databaseAttributeValueString);

            return receiveSqlRequest(query);
        }
        return new String[0];
    }


    private synchronized boolean checkIfDatabaseExists(String table){
        Connection conn = null;
        boolean exists = false;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(this.driverPath);

            DatabaseMetaData meta = conn.getMetaData();
            ResultSet res = meta.getTables(null, null, table,
                    new String[] {"TABLE"});

            if(res.next()){
                exists = true;
            }
            else{
                exists = false;
            }

            conn.close();

        } catch (Exception e) {
            log.error("Issue checking if Database exists", e);
            log.error("Shutting down due to lack of database access");
            System.exit(1);
        }
        return exists;
    }


    private boolean checkIfItemExists(String table, BigInteger id) {
        final String query = String.format("SELECT * FROM %s WHERE ID = %d ;", table, id);
        final String[] items;

        items = receiveSqlRequest(query);

        if(items.length == 0) {
            return false;
        }
        else {
            return true;
        }
    }


    private synchronized boolean sendSqlQuery(String query){
        Connection conn = null;
        Statement stmt = null;

        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(this.driverPath);
            conn.setAutoCommit(false);

            log.debug(String.format("SQL sent to Database: \"%s\"", query));

            stmt = conn.createStatement();
            stmt.executeUpdate(query);
            conn.commit();

            stmt.close();
            conn.close();

        } catch (Exception e) {
            log.error(e);
            return false;
        }
        return true;
    }


    private synchronized String[] receiveSqlRequest(String query) {
        String extractedInfo = "";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        String[] result = null;
        String key;
        String value;
        String entryAsString;

        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(this.driverPath);
            conn.setAutoCommit(false);

            log.debug(String.format("SQL sent to Database: \"%s\"", query));

            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);
            conn.commit();
            result = new String[rs.getFetchSize()];

            int count = 0;
            while (rs.next()) {
                entryAsString = "";
                for (DatabaseEntryAttribute databaseAttribute:databaseAttributes){
                    key = databaseAttribute.NAME;
                    value = rs.getString(key);
                    entryAsString += String.format("\'%s\':\'%s\', ", key, value);
                }
                entryAsString = entryAsString.substring(0, entryAsString.length()-2);
                result[count] = entryAsString;
                count ++;
            }

            stmt.close();
            conn.close();

        }
        catch(Exception e) {
            log.error(e);
        }

        return result;
    }


        private class DatabaseEntryAttribute{
        String NAME;
        String INITPARAM;
        Class DATATYPE;

        public DatabaseEntryAttribute(String mName, String mInitialisationParam, Class mDataType){
            this.NAME = mName;
            this.INITPARAM = mInitialisationParam;
            this.DATATYPE = mDataType;
        }
    }
}
