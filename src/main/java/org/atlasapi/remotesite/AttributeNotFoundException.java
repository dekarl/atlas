package org.atlasapi.remotesite;

import nu.xom.Element;

public class AttributeNotFoundException extends RuntimeException {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private Element element;
    private String attributeName;
    
    public AttributeNotFoundException(String message) {
        super(message);
    }

    public AttributeNotFoundException(Element element, String attributeName) {
        this("Attribute " + attributeName + " was not found on this xml element: " + element.toString());
        this.element = element;
        this.attributeName = attributeName;
    }
    
    public Element getElement() {
        return element;
    }
    
    public String getAttributeName() {
        return attributeName;
    }
}
