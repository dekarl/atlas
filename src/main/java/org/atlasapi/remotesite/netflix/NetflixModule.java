package org.atlasapi.remotesite.netflix;

import java.util.Set;

import javax.annotation.PostConstruct;

import nu.xom.Element;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Series;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.remotesite.ContentExtractor;
import org.atlasapi.s3.DefaultS3Client;
import org.atlasapi.s3.S3Client;
import org.joda.time.LocalTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.metabroadcast.common.properties.Configurer;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.RepetitionRules.Daily;
import com.metabroadcast.common.scheduling.SimpleScheduler;
import com.metabroadcast.common.url.QueryStringParameters;

@Configuration
public class NetflixModule {

    private final static Daily DAILY = RepetitionRules.daily(new LocalTime(4, 30, 0));
    private static final String BASE_API_URL = "http://api.netflix.com/catalog/indices/en_GB.xml";
    private static final String OAUTH_KEY = "oauth_consumer_key";
    
    private @Autowired SimpleScheduler scheduler;
    private @Autowired AdapterLog log;
    private @Autowired ContentWriter contentWriter;
    private @Autowired ContentResolver resolver;
    
    
    @PostConstruct
    public void startBackgroundTasks() {
        scheduler.schedule(netflixUpdater().withName("Netflix Updater"), DAILY);
    }

    private NetflixUpdater netflixUpdater() {
        int timeout = Configurer.get("netflix.connectionTimeout").toInt();
        String netflixConsumerKey = Configurer.get("netflix.consumerKey").get();
        String netflixFileName = Configurer.get("netflix.fileName").get();
        String localFilesPath = Configurer.get("netflix.filesPath").get();
        String s3access = Configurer.get("s3.access").get();
        String s3secret = Configurer.get("s3.secret").get();
        String s3bucket = Configurer.get("netflix.s3.bucket").get();
        
        QueryStringParameters params = new QueryStringParameters();
        params.add(OAUTH_KEY, netflixConsumerKey);
        String netflixUrl = BASE_API_URL + "?" + params.toQueryString();
        
        S3Client s3client = new DefaultS3Client(s3access, s3secret, s3bucket);
        NetflixDataStore dataStore = new DefaultNetflixFileStore(netflixFileName, localFilesPath, s3client);
        NetflixFileUpdater fileUpdater = new NetflixFileUpdater(netflixUrl, dataStore, timeout);
        NetflixContentExtractor<Film> filmExtractor = new NetflixFilmExtractor();
        NetflixContentExtractor<Brand> brandExtractor = new NetflixBrandExtractor();
        NetflixContentExtractor<Episode> episodeExtractor = new NetflixEpisodeExtractor();
        NetflixContentExtractor<Series> seriesExtractor = new NetflixSeriesExtractor();
        ContentExtractor<Element, Set<? extends Content>> extractor = new NetflixXmlElementContentExtractor(filmExtractor, brandExtractor, episodeExtractor, seriesExtractor);
        NetflixXmlElementHandler xmlHandler= new DefaultNetflixXmlElementHandler(extractor, resolver, contentWriter);
        return new NetflixUpdater(fileUpdater, xmlHandler);
    }
    
}
