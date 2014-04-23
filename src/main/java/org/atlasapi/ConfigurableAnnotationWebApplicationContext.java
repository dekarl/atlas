package org.atlasapi;

import org.atlasapi.application.ApplicationModule;
import org.atlasapi.equiv.OddJobRandomTaskModule;
import org.atlasapi.equiv.EquivModule;
import org.atlasapi.equiv.EquivTaskModule;
import org.atlasapi.feeds.AtlasFeedsModule;
import org.atlasapi.feeds.interlinking.delta.InterlinkingDeltaModule;
import org.atlasapi.feeds.radioplayer.RadioPlayerModule;
import org.atlasapi.feeds.xmltv.XmlTvModule;
import org.atlasapi.feeds.youview.YouViewFeedsWebModule;
import org.atlasapi.feeds.youview.YouViewUploadModule;
import org.atlasapi.logging.AtlasLoggingModule;
import org.atlasapi.logging.HealthModule;
import org.atlasapi.persistence.ManualScheduleRebuildModule;
import org.atlasapi.persistence.MongoContentPersistenceModule;
import org.atlasapi.query.QueryModule;
import org.atlasapi.query.QueryWebModule;
import org.atlasapi.query.SearchModule;
import org.atlasapi.remotesite.CreateYouTubeContentGroupModule;
import org.atlasapi.remotesite.RemoteSiteModule;
import org.atlasapi.remotesite.RemoteSiteModuleConfigurer;
import org.atlasapi.remotesite.bbc.nitro.BbcNitroModule;
import org.atlasapi.remotesite.channel4.pmlsd.C4PmlsdModule;
import org.atlasapi.remotesite.health.RemoteSiteHealthModule;
import org.atlasapi.remotesite.metabroadcast.picks.PicksModule;
import org.atlasapi.remotesite.metabroadcast.similar.SimilarContentModule;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.metabroadcast.common.properties.Configurer;

import org.atlasapi.persistence.CassandraPersistenceModule;
import org.atlasapi.remotesite.wikipedia.WikipediaModule;
import org.atlasapi.system.ContentPurgeWebModule;

public class ConfigurableAnnotationWebApplicationContext extends AnnotationConfigWebApplicationContext {

	private static final Function<Class<?>, String> TO_FQN = new Function<Class<?>, String>() {

		@Override
		public String apply(Class<?> clazz) {
			return clazz.getCanonicalName();
		}
	};

	@Override
	public final void setConfigLocation(String location) {
		Builder<Class<?>> builder = ImmutableList.builder();
		configure(builder);
		super.setConfigLocations(Lists.transform(builder.build(), TO_FQN).toArray(new String[0]));
	}

    private void configure(Builder<Class<?>> builder) {
        builder.add(
            AtlasModule.class,
            AtlasLoggingModule.class,
            AtlasWebModule.class,
            SearchModule.class,
            QueryModule.class,
            MongoContentPersistenceModule.class,
            CassandraPersistenceModule.class, 
            AtlasFetchModule.class,
            RemoteSiteModule.class, 
            HealthModule.class,
            RadioPlayerModule.class,
            XmlTvModule.class, 
            RemoteSiteHealthModule.class, 
            EquivModule.class
        );
        
        if(runProcessingOnly()) {
            builder.add(
                ManualScheduleRebuildModule.class, 
                InterlinkingDeltaModule.class,
                EquivTaskModule.class,
                OddJobRandomTaskModule.class,
                C4PmlsdModule.class,
                WikipediaModule.class,
                BbcNitroModule.class,
                PicksModule.class,
                SimilarContentModule.class,
                CreateYouTubeContentGroupModule.class,
                ContentPurgeWebModule.class
            );
            if (Configurer.get("youview.upload.enabled").toBoolean()) {
                builder.add(
                    YouViewUploadModule.class,
                    YouViewFeedsWebModule.class
                );
            }
            builder.addAll(new RemoteSiteModuleConfigurer().enabledModules());
        } else {
            builder.add(
                AtlasFeedsModule.class,
                QueryWebModule.class,
                ApplicationModule.class
            );
        }
    }

	private boolean runProcessingOnly() {
		return Boolean.parseBoolean(System.getProperty("processing.config"));
	}
}
