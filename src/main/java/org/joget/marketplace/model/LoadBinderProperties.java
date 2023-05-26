package org.joget.marketplace.model;

import org.json.JSONArray;

public class LoadBinderProperties {
    
    private String jdbcDatasource;
    private String jdbcDriver;
    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;

    private String sql;
    private String autoHandleWorkflowVariable;
    private String autoHandleFiles;
    
    private String tableName;
    private String keyColumn;
    private JSONArray filters;
    private String extraCondition;
    

    public String getJdbcDatasource() {
        return jdbcDatasource;
    }

    public void setJdbcDatasource(String jdbcDatasource) {
        this.jdbcDatasource = jdbcDatasource;
    }

    public String getJdbcDriver() {
        return jdbcDriver;
    }

    public void setJdbcDriver(String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getJdbcUser() {
        return jdbcUser;
    }

    public void setJdbcUser(String jdbcUser) {
        this.jdbcUser = jdbcUser;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public void setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getAutoHandleWorkflowVariable() {
        return autoHandleWorkflowVariable;
    }

    public void setAutoHandleWorkflowVariable(String autoHandleWorkflowVariable) {
        this.autoHandleWorkflowVariable = autoHandleWorkflowVariable;
    }

    public String getAutoHandleFiles() {
        return autoHandleFiles;
    }

    public void setAutoHandleFiles(String autoHandleFiles) {
        this.autoHandleFiles = autoHandleFiles;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getKeyColumn() {
        return keyColumn;
    }

    public void setKeyColumn(String keyColumn) {
        this.keyColumn = keyColumn;
    }

    public JSONArray getFilters() {
        return filters;
    }

    public void setFilters(JSONArray filters) {
        this.filters = filters;
    }

    public String getExtraCondition() {
        return extraCondition;
    }

    public void setExtraCondition(String extraCondition) {
        this.extraCondition = extraCondition;
    }
    
    private class LoadBinderFilter {
        
    }
}
