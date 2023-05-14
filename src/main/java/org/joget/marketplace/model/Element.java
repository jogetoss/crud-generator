
package org.joget.marketplace.model;

import java.util.List;

public class Element {

    private List<SectionElement> elements;
    private String className;
    private SectionProperties properties;

    public List<SectionElement> getElements() {
        return elements;
    }

    public void setElements(List<SectionElement> elements) {
        this.elements = elements;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public SectionProperties getProperties() {
        return properties;
    }

    public void setProperties(SectionProperties properties) {
        this.properties = properties;
    }

}
