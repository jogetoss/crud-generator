
package org.joget.marketplace.model;

public class Properties {

    private String id;
    private String name;
    private String tableName;
    private LoadBinder loadBinder;
    private StoreBinder storeBinder;
    private String description;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public LoadBinder getLoadBinder() {
        return loadBinder;
    }

    public void setLoadBinder(LoadBinder loadBinder) {
        this.loadBinder = loadBinder;
    }

    public StoreBinder getStoreBinder() {
        return storeBinder;
    }

    public void setStoreBinder(StoreBinder storeBinder) {
        this.storeBinder = storeBinder;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
