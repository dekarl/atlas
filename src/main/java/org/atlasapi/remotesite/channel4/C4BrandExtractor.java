package org.atlasapi.remotesite.channel4;

import java.util.List;

import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Clip;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.remotesite.ContentExtractor;

import com.metabroadcast.common.time.SystemClock;
import com.sun.syndication.feed.atom.Feed;

public class C4BrandExtractor implements ContentExtractor<Feed, BrandSeriesAndEpisodes>{

    private final C4BrandBasicDetailsExtractor basicDetailsExtractor;
    private final C4OdEpisodesAdapter fourOditemAdapter;
    private final C4EpisodeGuideAdapter episodeGuideAdapter;
    private final C4BrandEpgAdapter brandEpgAdatper;
    private final C4BrandClipAdapter clipAdapter;
    
    private final C4OnDemandEpisodesLinker odLinker = new C4OnDemandEpisodesLinker();
    private final C4BrandEpisodeLinker epgLinker = new C4BrandEpisodeLinker();
    private final C4ClipLinker clipLinker = new C4ClipLinker();
    
    public C4BrandExtractor(C4AtomApiClient feedClient, ChannelResolver channelResolver) {
        SystemClock clock = new SystemClock();
        this.basicDetailsExtractor = new C4BrandBasicDetailsExtractor(channelResolver, clock);
        this.episodeGuideAdapter = new C4EpisodeGuideAdapter(feedClient, clock);
        this.fourOditemAdapter = new C4OdEpisodesAdapter(feedClient, clock);
        this.brandEpgAdatper = new C4BrandEpgAdapter(feedClient, clock);
        this.clipAdapter = new C4BrandClipAdapter(feedClient, clock);
    }

    /*
     * Extracts a full content hierarchy for a brand. Attempts to retrieve data
     * from /episode-guide.atom, /4od.atom, /epg.atom and /video.atom (in that order).
     */
    @Override
    public BrandSeriesAndEpisodes extract(Feed source) {
        
        Brand brand = basicDetailsExtractor.extract(source);
        
        List<SeriesAndEpisodes> episodeGuideContent = episodeGuideAdapter.fetch(brand.getCanonicalUri());

        List<Episode> fourOdContent = fourOditemAdapter.fetch(brand.getCanonicalUri());
        List<SeriesAndEpisodes> seriesAndEpisodes = odLinker.link4odToEpg(episodeGuideContent, fourOdContent);
        
        List<Episode> epgContent = brandEpgAdatper.fetch(brand.getCanonicalUri());
        seriesAndEpisodes = epgLinker.populateBroadcasts(seriesAndEpisodes, epgContent, brand);
        
        List<Clip> clips = clipAdapter.fetch(brand.getCanonicalUri());
        seriesAndEpisodes = clipLinker.linkClipsToContent(brand, seriesAndEpisodes, clips);
        
        seriesAndEpisodes = setBrandProperties(seriesAndEpisodes, brand);

        return new BrandSeriesAndEpisodes(brand, seriesAndEpisodes);
    }


    private List<SeriesAndEpisodes> setBrandProperties(List<SeriesAndEpisodes> content, Brand brand) {
        for (SeriesAndEpisodes seriesAndEpisodes : content) {
            for (Episode episode : seriesAndEpisodes.getEpisodes()) {
                if (equivalentTitles(brand, episode)) {
                    setHierarchicalTitle(episode);
                }
                if (episode.getImage() == null) {
                    episode.setImage(brand.getImage());
                    episode.setThumbnail(brand.getThumbnail());
                }
                episode.setGenres(brand.getGenres());
            }
        }
        return content;
    }

    private void setHierarchicalTitle(Episode episode) {
        if (episode.getSeriesNumber() != null && episode.getEpisodeNumber() != null) {
            episode.setTitle("Series " + episode.getSeriesNumber() + " Episode " + episode.getEpisodeNumber());
        }
    }
    

    private boolean equivalentTitles(Brand brand, Episode episode) {
		String notAlphanumeric = "[^\\d\\w]";
		return episode.getTitle().replaceAll(notAlphanumeric, "").equals(brand.getTitle().replaceAll(notAlphanumeric, ""));
	}

}
