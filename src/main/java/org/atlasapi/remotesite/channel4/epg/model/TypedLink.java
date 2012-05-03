package org.atlasapi.remotesite.channel4.epg.model;

public class TypedLink {

    private final String linkTarget;
    private final String linkRelationship;

    public TypedLink(String linkTarget, String linkRelationship) {
        this.linkTarget = linkTarget;
        this.linkRelationship = linkRelationship;
    }

    public String getTarget() {
        return this.linkTarget;
    }

    public String getRelationship() {
        return this.linkRelationship;
    }
    
}
