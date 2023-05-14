
package org.joget.marketplace.model;

import java.util.List;

public class SectionElement {

    private List<FormElement> elements;
    private String className;
    private ColumnProperties properties;

    public List<FormElement> getElements() {
        return elements;
    }

    public void setElements(List<FormElement> elements) {
        this.elements = elements;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public ColumnProperties getProperties() {
        return properties;
    }

    public void setProperties(ColumnProperties properties) {
        this.properties = properties;
    }

}
