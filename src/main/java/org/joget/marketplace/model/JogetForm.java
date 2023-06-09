
package org.joget.marketplace.model;

import java.util.List;

public class JogetForm {

    private String className;
    private GeneralProperties properties;
    private List<Element> elements;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public GeneralProperties getProperties() {
        return properties;
    }

    public void setProperties(GeneralProperties properties) {
        this.properties = properties;
    }

    public List<Element> getElements() {
        return elements;
    }

    public void setElements(List<Element> elements) {
        this.elements = elements;
    }

}
