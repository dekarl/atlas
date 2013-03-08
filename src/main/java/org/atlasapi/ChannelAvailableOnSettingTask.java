package org.atlasapi;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterables.addAll;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.metabroadcast.common.persistence.mongo.MongoBuilders.update;
import static com.metabroadcast.common.persistence.mongo.MongoBuilders.where;

import java.net.UnknownHostException;
import java.util.Set;

import org.atlasapi.media.channel.CachingChannelStore;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.channel.ChannelTranslator;
import org.atlasapi.media.channel.MongoChannelGroupStore;
import org.atlasapi.media.channel.MongoChannelStore;
import org.atlasapi.media.entity.Publisher;

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.properties.Configurer;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public class ChannelAvailableOnSettingTask extends ScheduledTask {
    
    private final ChannelResolver resolver;
    private final DBCollection collection;

    public ChannelAvailableOnSettingTask(ChannelResolver resolver, DBCollection collection) {
        this.resolver = resolver;
        this.collection = collection;
    }

    @Override
    protected void runTask() {
        for (Channel channel : resolver.all()) {
            Set<String> availableOn = Sets.newHashSet();
            
            if (channel.broadcaster() != null) {
                availableOn.add(channel.broadcaster().key());
            }
            
            addAll(availableOn, filter(transform(channel.getAliasUrls(), TO_AVAILABLE_ON), notNull()));
            
            collection.update(where().idEquals(channel.getId()).build(), update().setField(ChannelTranslator.AVAILABLE_ON, availableOn).build());
        }
    }
    
    private Function<String, String> TO_AVAILABLE_ON = new Function<String, String>() {
        @Override
        public String apply(String input) {
            if (input.startsWith("http://pressassociation.com")) {
                return Publisher.PA.key();
            } else if (input.startsWith("http://xmltv.radiotimes.com")) {
                return Publisher.RADIO_TIMES.key();
            } else if (input.startsWith("http://devapi.bbcredux.com")) {
                return Publisher.BBC_REDUX.key();
            } else {
                return null;
            }
        }
    };

    public static void main(String[] args) throws UnknownHostException, MongoException {
        
        DatabasedMongo mongo = new DatabasedMongo(new Mongo(Configurer.get("mongo.host").get()), Configurer.get("mongo.dbName").get());
        
        MongoChannelGroupStore store = new MongoChannelGroupStore(mongo);
        
        new ChannelAvailableOnSettingTask(new CachingChannelStore(new MongoChannelStore(mongo, store, store)) ,mongo.collection(MongoChannelStore.COLLECTION)).run();
    }
}
