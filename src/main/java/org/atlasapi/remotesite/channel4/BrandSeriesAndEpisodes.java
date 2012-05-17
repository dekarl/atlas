package org.atlasapi.remotesite.channel4;

import java.util.List;

import org.atlasapi.media.entity.Brand;

public class BrandSeriesAndEpisodes {

    private final Brand brand;
    private final List<SeriesAndEpisodes> seriesAndEpisodes;

    public BrandSeriesAndEpisodes(Brand brand, List<SeriesAndEpisodes> seriesAndEpisodes) {
        this.brand = brand;
        this.seriesAndEpisodes = seriesAndEpisodes;
    }

    public Brand getBrand() {
        return this.brand;
    }

    public List<SeriesAndEpisodes> getSeriesAndEpisodes() {
        return this.seriesAndEpisodes;
    }

}
