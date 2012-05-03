package org.atlasapi.remotesite.channel4.epg.model;

import java.util.Set;

import com.metabroadcast.common.intl.Country;

public class C4EpgMedia {

    private String player;
    private String thumbnail;
    private String rating;
    private Set<Country> availableCountries;

    public static C4EpgMedia from(C4MediaGroupElement groupElement, C4MediaContentElement contentElement) {
        C4EpgMedia media = new C4EpgMedia();
        if (contentElement != null) {
            media.withThumbnail(contentElement.thumbnail());
        }
        if (groupElement != null) {
            media.withPlayer(groupElement.player()).withThumbnail(groupElement.thumbnail()).withRating(groupElement.rating()).withRestriction(groupElement.availableCountries());
        }
        return media;
    }

    public C4EpgMedia withRestriction(Set<Country> availableCountries) {
        this.availableCountries = availableCountries;
        return this;
    }

    public C4EpgMedia withRating(String rating) {
        this.rating = rating;
        return this;
    }

    public C4EpgMedia withThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
        return this;
    }

    public C4EpgMedia withPlayer(String player) {
        this.player = player;
        return this;
    }

    public String player() {
        return player;
    }

    public String thumbnail() {
        return thumbnail;
    }

    public String rating() {
        return rating;
    }

    public Set<Country> availableCountries() {
        return availableCountries;
    }

}
