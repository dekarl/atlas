package org.atlasapi.remotesite.channel4.epg.model;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.google.common.collect.ImmutableList;

public class C4EpgEntry {

    private final String id;
    private String title;
    private DateTime updated;
    private String summary;
    private List<TypedLink> links = ImmutableList.of();
    private C4EpgMedia media;
    private String brandTitle;
    private String available;
    private Integer seriesNumber;
    private Integer episodeNumber;
    private Integer ageRating;
    private DateTime txDate;
    private String txChannel;
    private Boolean subtitles;
    private Boolean audioDescription;
    private Duration duration;
    private Boolean wideScreen;
    private Boolean signing;
    private Boolean repeat;
    private String programmeId;
    private Boolean simulcastRights;

    public static C4EpgEntry from(C4EpgEntryElement element) {
        C4EpgEntry entry = new C4EpgEntry(element.id());
        entry.withTitle(element.title())
                .withUpdated(element.updated())
                .withSummary(element.summary())
                .withLinks(element.links())
                .withMedia(C4EpgMedia.from(element.mediaGroup(), element.mediaContent()))
                .withBrandTitle(element.brandTitle())
                .withAvailable(element.available())
                .withSeriesNumber(element.seriesNumber())
                .withEpisodeNumber(element.episodeNumber())
                .withAgeRating(element.ageRating())
                .withTxDate(element.txDate())
                .withTxChannel(element.txChannel())
                .withSubtitles(element.subtitles())
                .withAudioDescription(element.audioDescription())
                .withWideScreen(element.wideScreen())
                .withSigning(element.signing())
                .withRepeat(element.repeat())
                .withProgrammeId(element.programmeId())
                .withSimulcastRights(element.simulcastRights())
                .withDuration(element.duration());
        return entry;
    }

    public C4EpgEntry(String id) {
        this.id = id;
    }
    
    
    public C4EpgEntry withDuration(Duration duration) {
        this.duration = duration;
        return this;
    }

    public C4EpgEntry withAudioDescription(Boolean audioDescription) {
        this.audioDescription = audioDescription;
        return this;
    }

    public C4EpgEntry withSubtitles(Boolean subtitles) {
        this.subtitles = subtitles;
        return this;
    }
    
    public C4EpgEntry withWideScreen(Boolean wideScreen) {
        this.wideScreen = wideScreen;
        return this;
    }

    public C4EpgEntry withSigning(Boolean signing) {
        this.signing = signing;
        return this;
    }

    public C4EpgEntry withRepeat(Boolean repeat) {
        this.repeat = repeat;
        return this;
    }

    public C4EpgEntry withProgrammeId(String programmeId) {
        this.programmeId = programmeId;
        return this;
    }

    public C4EpgEntry withSimulcastRights(Boolean simulcastRights) {
        this.simulcastRights = simulcastRights;
        return this;
    }

    public C4EpgEntry withTxChannel(String txChannel) {
        this.txChannel = txChannel;
        return this;
    }

    public C4EpgEntry withTxDate(DateTime txDate) {
        this.txDate = txDate;
        return this;
    }

    public C4EpgEntry withAgeRating(Integer ageRating) {
        this.ageRating = ageRating;
        return this;
    }

    public C4EpgEntry withEpisodeNumber(Integer episodeNumber) {
        this.episodeNumber = episodeNumber;
        return this;
    }

    public C4EpgEntry withSeriesNumber(Integer seriesNumber) {
        this.seriesNumber = seriesNumber;
        return this;
    }

    public C4EpgEntry withAvailable(String available) {
        this.available = available;
        return this;
    }

    public C4EpgEntry withBrandTitle(String brandTitle) {
        this.brandTitle = brandTitle;
        return this;
    }

    public C4EpgEntry withMedia(C4EpgMedia media) {
        this.media = media;
        return this;
    }

    public C4EpgEntry withLinks(List<TypedLink> links) {
        this.links = links;
        return this;
    }

    public C4EpgEntry withSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public C4EpgEntry withUpdated(DateTime updated) {
        this.updated = updated;
        return this;
    }   

    public C4EpgEntry withTitle(String title) {
        this.title = title;
        return this;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public DateTime updated() {
        return updated;
    }

    public String summary() {
        return summary;
    }

    public List<TypedLink> links() {
        return links;
    }
    
    public C4EpgMedia media() {
        return media;
    }

    public String brandTitle() {
        return brandTitle;
    }

    public String available() {
        return available;
    }

    public Integer seriesNumber() {
        return seriesNumber;
    }

    public Integer episodeNumber() {
        return episodeNumber;
    }

    public Integer ageRating() {
        return ageRating;
    }

    public DateTime txDate() {
        return txDate;
    }

    public String txChannel() {
        return txChannel;
    }

    public Boolean subtitles() {
        return subtitles;
    }

    public Boolean audioDescription() {
        return audioDescription;
    }

    public Duration duration() {
        return duration;
    }

    public String getRelatedLink() {
        for (int i = 0; i < links.size(); i++) {
            if (links.get(i).getRelationship().equals("related")) {
                return links.get(i).getTarget();
            }
        }
        return null;
    }
    
    public boolean hasRelatedLink() {
        return getRelatedLink() != null;
    }
    
    public Boolean wideScreen() {
        return this.wideScreen;
    }

    public Boolean signing() {
        return this.signing;
    }

    public Boolean repeat() {
        return this.repeat;
    }

    public String programmeId() {
        return this.programmeId;
    }

    public Boolean simulcastRights() {
        return this.simulcastRights;
    }

}
