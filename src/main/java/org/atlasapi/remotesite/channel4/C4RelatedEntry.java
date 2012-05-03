package org.atlasapi.remotesite.channel4;

public final class C4RelatedEntry {

    // the location of the feed where the item is described
    private final String feedId;
    private final String episodeIdTag;
    
    public C4RelatedEntry(String feedId, String episodeIdTag) {
        this.feedId = feedId;
        this.episodeIdTag = episodeIdTag;
    }
    
    public String getEpisodeIdTag() {
        return episodeIdTag;
    }

    public String getFeedId() {
        return feedId;
    }
}
