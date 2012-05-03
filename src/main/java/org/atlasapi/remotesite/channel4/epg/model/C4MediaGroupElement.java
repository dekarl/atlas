package org.atlasapi.remotesite.channel4.epg.model;

import java.util.Set;

import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;

import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.intl.Country;

public class C4MediaGroupElement extends Element {

    public C4MediaGroupElement(Element element) {
        super(element);
    }

    public C4MediaGroupElement(String name, String uri) {
        super(name, uri);
    }

    public C4MediaGroupElement(String name) {
        super(name);
    }
    
    @Override
    public Node copy() {
        return new C4MediaGroupElement(this);
    }
    
    @Override
    public String toString() {
        return "Media group element";
    }

    public String thumbnail() {
        return getMediaElementValue("thumbnail").getAttributeValue("url");
    }
    
    public String player() {
        return getMediaElementValue("player").getAttributeValue("url");
    }
    
    public String rating() {
        return getMediaElementValue("rating").getValue();
    }
    
    public Set<Country> availableCountries() {
        return Countries.fromDelimtedList(getMediaElementValue("restriction").getValue());
    }

    private Element getMediaElementValue(String elementName) {
        Elements childElements = this.getChildElements(elementName, "http://search.yahoo.com/mrss/");
        if(childElements.size() == 0) {
            return null;
        }
        return childElements.get(0);
    }

    
}
