package org.atlasapi.remotesite.channel4.epg.model;

import java.util.List;

import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;

import org.atlasapi.remotesite.channel4.C4RelatedEntry;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.metabroadcast.common.time.DateTimeZones;

public class C4EpgEntryElement extends Element {

    private static final String DC_NS = "http://purl.org/dc/elements/1.1/";
    private static final String ATOM_NS = "http://www.w3.org/2005/Atom";

    public C4EpgEntryElement(Element element) {
        super(element);
    }

    public C4EpgEntryElement(String namespace, String name) {
        super(namespace, name);
    }

    public C4EpgEntryElement(String name) {
        super(name);
    }

    @Override
    public Node copy() {
        return new C4EpgEntryElement(this);
    }

    public C4MediaGroupElement mediaGroup() {
        Elements childElements = this.getChildElements("group", "http://search.yahoo.com/mrss/");
        if (childElements.size() == 0) {
            return null;
        }
        return (C4MediaGroupElement) childElements.get(0);
    }

    public C4MediaContentElement mediaContent() {
        Elements childElements = this.getChildElements("content", "http://search.yahoo.com/mrss/");
        if (childElements.size() == 0) {
            return null;
        }
        return (C4MediaContentElement) childElements.get(0);
    }

    private String getAtomElementValue(String elementName) {
        Elements elements = this.getChildElements(elementName, ATOM_NS);
        if (elements.size() == 0) {
            return null;
        }
        return elements.get(0).getValue();
    }

    public String id() {
        return getAtomElementValue("id");
    }

    public String title() {
        return getAtomElementValue("title");
    }

    public DateTime updated() {
        String dateString = getAtomElementValue("updated");
        return dateString != null ? new DateTime(dateString, DateTimeZones.UTC) : null;
    }

    public String summary() {
        return getAtomElementValue("summary");
    }
    
    public C4RelatedEntry relatedEntry() {
        Elements elements = getChildElements("relation.RelatedEntryId",  DC_NS);
        if (elements.size() == 0) {
            return null;
        }
        Element element = elements.get(0);
        return new C4RelatedEntry(element.getAttributeValue("feedId"), element.getValue());
    }

    public List<TypedLink> links() {
        Elements elements = this.getChildElements("link", ATOM_NS);
        List<TypedLink> linkHrefs = Lists.newArrayListWithExpectedSize(elements.size());
        for (int i = 0; i < elements.size(); i++) {
            String target = elements.get(i).getAttribute("href").getValue();
            String relation = elements.get(i).getAttribute("rel").getValue();
            linkHrefs.add(new TypedLink(target, relation));
        }
        return linkHrefs;
    }

    private String getDublinCoreElementValue(String elementName) {
        Elements elements = this.getChildElements(elementName, DC_NS);
        if (elements.size() == 0) {
            return null;
        }
        return elements.get(0).getValue();
    }

    public String brandTitle() {
        return getDublinCoreElementValue("relation.BrandTitle");
    }

    public Integer seriesNumber() {
        String value = getDublinCoreElementValue("relation.SeriesNumber");
        return !Strings.isNullOrEmpty(value) ? Integer.parseInt(value) : null;
    }

    public Integer episodeNumber() {
        String value = getDublinCoreElementValue("relation.EpisodeNumber");
        return !Strings.isNullOrEmpty(value) ? Integer.parseInt(value) : null;
    }

    public Integer ageRating() {
        String value = getDublinCoreElementValue("relation.AgeRating");
        return !Strings.isNullOrEmpty(value) ? Integer.parseInt(value) : null;
    }

    public DateTime txDate() {
        String value = getDublinCoreElementValue("date.TXDate");
        return !Strings.isNullOrEmpty(value) ? new DateTime(value, DateTimeZones.UTC) : null;
    }

    public String txChannel() {
        return getDublinCoreElementValue("relation.TXChannel");
    }

    public Boolean subtitles() {
        String value = getDublinCoreElementValue("relation.Subtitles");
        return !Strings.isNullOrEmpty(value) ? Boolean.parseBoolean(value) : null;
    }

    public Boolean audioDescription() {
        String value = getDublinCoreElementValue("relation.AudioDescription");
        return !Strings.isNullOrEmpty(value) ? Boolean.parseBoolean(value) : null;
    }

    public Duration duration() {
        String value = getDublinCoreElementValue("relation.Duration");
        if (value != null) {
            String[] durationParts = value.split(":");
            return new Duration(Integer.parseInt(durationParts[0]) * 60 * 1000 + Integer.parseInt(durationParts[1]) * 1000);
        }
        return null;
    }

    public Boolean wideScreen() {
        String value = getDublinCoreElementValue("relation.WideScreen");
        return !Strings.isNullOrEmpty(value) ? Boolean.parseBoolean(value) : null;
    }

    public Boolean signing() {
        String value = getDublinCoreElementValue("relation.Signing");
        return !Strings.isNullOrEmpty(value) ? Boolean.parseBoolean(value) : null;
    }

    public Boolean repeat() {
        String value = getDublinCoreElementValue("relation.Repeat");
        return !Strings.isNullOrEmpty(value) ? Boolean.parseBoolean(value) : null;
    }

    public String available() {
        Elements availableElements = getChildElements("available", "http://purl.org/dc/terms");
        if (availableElements.size() == 0) {
            return null;
        }
        return availableElements.get(0).getValue();
    }

    @Override
    public String toString() {
        return "Epg Entry Element";
    }

    public String programmeId() {
        return getDublinCoreElementValue("relation.programmeId");
    }

    public Boolean simulcastRights() {
        String value = getDublinCoreElementValue("relation.SimulcastRights");
        return !Strings.isNullOrEmpty(value) ? Boolean.parseBoolean(value) : null;
    }
}
