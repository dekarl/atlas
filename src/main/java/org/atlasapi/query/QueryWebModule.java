package org.atlasapi.query;

import org.atlasapi.application.query.ApplicationConfigurationFetcher;
import org.atlasapi.input.DefaultGsonModelReader;
import org.atlasapi.input.DelegatingModelTransformer;
import org.atlasapi.input.ItemModelTransformer;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelGroup;
import org.atlasapi.media.channel.ChannelGroupResolver;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.ContentGroup;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Person;
import org.atlasapi.media.entity.Schedule.ScheduleChannel;
import org.atlasapi.media.entity.Topic;
import org.atlasapi.media.entity.simple.ChannelGroupQueryResult;
import org.atlasapi.media.entity.simple.ChannelQueryResult;
import org.atlasapi.media.entity.simple.ContentGroupQueryResult;
import org.atlasapi.media.entity.simple.ContentQueryResult;
import org.atlasapi.media.entity.simple.PeopleQueryResult;
import org.atlasapi.media.entity.simple.ProductQueryResult;
import org.atlasapi.media.entity.simple.ScheduleQueryResult;
import org.atlasapi.media.entity.simple.TopicQueryResult;
import org.atlasapi.media.product.Product;
import org.atlasapi.media.product.ProductResolver;
import org.atlasapi.media.segment.SegmentResolver;
import org.atlasapi.output.AtlasModelWriter;
import org.atlasapi.output.DispatchingAtlasModelWriter;
import org.atlasapi.output.JaxbXmlTranslator;
import org.atlasapi.output.JsonTranslator;
import org.atlasapi.output.QueryResult;
import org.atlasapi.output.SimpleChannelGroupModelWriter;
import org.atlasapi.output.SimpleChannelModelWriter;
import org.atlasapi.output.SimpleContentGroupModelWriter;
import org.atlasapi.output.SimpleContentModelWriter;
import org.atlasapi.output.SimplePersonModelWriter;
import org.atlasapi.output.SimpleProductModelWriter;
import org.atlasapi.output.SimpleScheduleModelWriter;
import org.atlasapi.output.SimpleTopicModelWriter;
import org.atlasapi.output.rdf.RdfXmlTranslator;
import org.atlasapi.output.simple.ChannelGroupModelSimplifier;
import org.atlasapi.output.simple.ChannelGroupSimplifier;
import org.atlasapi.output.simple.ChannelModelSimplifier;
import org.atlasapi.output.simple.ChannelNumberingChannelGroupModelSimplifier;
import org.atlasapi.output.simple.ChannelNumberingChannelModelSimplifier;
import org.atlasapi.output.simple.ChannelNumberingsChannelGroupToChannelModelSimplifier;
import org.atlasapi.output.simple.ChannelNumberingsChannelToChannelGroupModelSimplifier;
import org.atlasapi.output.simple.ChannelSimplifier;
import org.atlasapi.output.simple.ContainerModelSimplifier;
import org.atlasapi.output.simple.ContentGroupModelSimplifier;
import org.atlasapi.output.simple.ItemModelSimplifier;
import org.atlasapi.output.simple.ProductModelSimplifier;
import org.atlasapi.output.simple.PublisherSimplifier;
import org.atlasapi.output.simple.TopicModelSimplifier;
import org.atlasapi.persistence.content.ContentGroupResolver;
import org.atlasapi.persistence.content.ContentGroupWriter;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.content.PeopleResolver;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.atlasapi.persistence.content.SearchResolver;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.output.AvailableChildrenResolver;
import org.atlasapi.persistence.output.ContainerSummaryResolver;
import org.atlasapi.persistence.output.MongoAvailableChildrenResolver;
import org.atlasapi.persistence.output.MongoContainerSummaryResolver;
import org.atlasapi.persistence.output.MongoRecentlyBroadcastChildrenResolver;
import org.atlasapi.persistence.output.MongoUpcomingChildrenResolver;
import org.atlasapi.persistence.output.RecentlyBroadcastChildrenResolver;
import org.atlasapi.persistence.output.UpcomingChildrenResolver;
import org.atlasapi.persistence.topic.TopicContentLister;
import org.atlasapi.persistence.topic.TopicQueryResolver;
import org.atlasapi.persistence.topic.TopicStore;
import org.atlasapi.query.content.schedule.ScheduleOverlapListener;
import org.atlasapi.query.content.schedule.ScheduleOverlapResolver;
import org.atlasapi.query.topic.PublisherFilteringTopicContentLister;
import org.atlasapi.query.topic.PublisherFilteringTopicResolver;
import org.atlasapi.query.v2.ChannelController;
import org.atlasapi.query.v2.ChannelGroupController;
import org.atlasapi.query.v2.ContentGroupController;
import org.atlasapi.query.v2.ContentWriteController;
import org.atlasapi.query.v2.PeopleController;
import org.atlasapi.query.v2.ProductController;
import org.atlasapi.query.v2.QueryController;
import org.atlasapi.query.v2.ScheduleController;
import org.atlasapi.query.v2.SearchController;
import org.atlasapi.query.v2.TopicController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.metabroadcast.common.ids.NumberToShortStringCodec;
import com.metabroadcast.common.ids.SubstitutionTableNumberCodec;
import com.metabroadcast.common.media.MimeType;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.time.SystemClock;

@Configuration
public class QueryWebModule {
    
    private @Value("${local.host.name}") String localHostName;
    private @Value("${ids.expose}") String exposeIds;
    
    private @Autowired DatabasedMongo mongo;
    private @Autowired ContentGroupWriter contentGroupWriter;
    private @Autowired ContentGroupResolver contentGroupResolver;
    private @Autowired ContentWriter contentWriter;
    private @Autowired ContentResolver contentResolver;
    private @Autowired ChannelResolver channelResolver;
    private @Autowired ChannelGroupResolver channelGroupResolver;
    private @Autowired ScheduleResolver scheduleResolver;
    private @Autowired SearchResolver searchResolver;
    private @Autowired PeopleResolver peopleResolver;
    private @Autowired TopicQueryResolver topicResolver;
    private @Autowired @Qualifier("topicStore") TopicStore topicStore;
    private @Autowired TopicContentLister topicContentLister;
    private @Autowired SegmentResolver segmentResolver;
    private @Autowired ProductResolver productResolver;

    private @Autowired KnownTypeQueryExecutor queryExecutor;
    private @Autowired ApplicationConfigurationFetcher configFetcher;
    private @Autowired AdapterLog log;
    
    @Bean ChannelController channelController() {
        return new ChannelController(configFetcher, log, channelModelWriter(), channelResolver, new SubstitutionTableNumberCodec());
    }

    @Bean AtlasModelWriter<Iterable<Channel>> channelModelWriter() {
        ChannelModelSimplifier channelModelSimplifier = channelModelSimplifier();
        return this.<Iterable<Channel>>standardWriter(
            new SimpleChannelModelWriter(new JsonTranslator<ChannelQueryResult>(), channelModelSimplifier),
            new SimpleChannelModelWriter(new JaxbXmlTranslator<ChannelQueryResult>(), channelModelSimplifier));
    }
    
    @Bean ChannelModelSimplifier channelModelSimplifier() {
        return new ChannelModelSimplifier(channelSimplifier(), new ChannelNumberingsChannelToChannelGroupModelSimplifier(
            channelGroupResolver, 
            new ChannelNumberingChannelGroupModelSimplifier(channelGroupSimplifier())));
    }
    
    @Bean ChannelSimplifier channelSimplifier() {
        return new ChannelSimplifier(new SubstitutionTableNumberCodec(), channelResolver, publisherSimplifier());
    }
    
    @Bean ChannelGroupSimplifier channelGroupSimplifier() {
        return new ChannelGroupSimplifier(new SubstitutionTableNumberCodec(), channelGroupResolver, publisherSimplifier());
    }


    @Bean
    ChannelGroupController channelGroupController() {
        NumberToShortStringCodec idCodec = new SubstitutionTableNumberCodec();
        return new ChannelGroupController(configFetcher, log, channelGroupModelWriter(), channelGroupResolver, idCodec);
    }

    @Bean AtlasModelWriter<Iterable<ChannelGroup>> channelGroupModelWriter() {
        ChannelGroupModelSimplifier channelGroupModelSimplifier = ChannelGroupModelSimplifier();
        return this.<Iterable<ChannelGroup>>standardWriter(
            new SimpleChannelGroupModelWriter(new JsonTranslator<ChannelGroupQueryResult>(), channelGroupModelSimplifier),
            new SimpleChannelGroupModelWriter(new JaxbXmlTranslator<ChannelGroupQueryResult>(), channelGroupModelSimplifier));
    }

    @Bean ChannelGroupModelSimplifier ChannelGroupModelSimplifier() {
        return new ChannelGroupModelSimplifier(channelGroupSimplifier(), new ChannelNumberingsChannelGroupToChannelModelSimplifier(
            channelResolver,
            new ChannelNumberingChannelModelSimplifier(channelSimplifier())));
    }
    
    @Bean PublisherSimplifier publisherSimplifier() {
        return new PublisherSimplifier();
    }

    @Bean
    QueryController queryController() {
        return new QueryController(queryExecutor, configFetcher, log, contentModelOutputter(), contentWriteController());
    }
    
    ContentWriteController contentWriteController() {
        return new ContentWriteController(configFetcher, contentResolver, contentWriter, new DefaultGsonModelReader(), new DelegatingModelTransformer(new ItemModelTransformer(contentResolver, topicStore, new SystemClock())));
    }

    @Bean
    ScheduleOverlapListener scheduleOverlapListener() {
        return new ScheduleOverlapListener() {

            @Override
            public void itemRemovedFromSchedule(Item item, Broadcast broadcast) {
            }
        };
//        BroadcastRemovingScheduleOverlapListener broadcastRemovingListener = new BroadcastRemovingScheduleOverlapListener(contentResolver, contentWriter);
//        return new ThreadedScheduleOverlapListener(broadcastRemovingListener, log);
    }

    @Bean
    ScheduleController schedulerController() {
        ScheduleOverlapResolver resolver = new ScheduleOverlapResolver(scheduleResolver, scheduleOverlapListener(), log);
        return new ScheduleController(resolver, channelResolver, configFetcher, log, scheduleChannelModelOutputter());
    }

    @Bean
    PeopleController peopleController() {
        return new PeopleController(peopleResolver, configFetcher, log, personModelOutputter());
    }

    @Bean
    SearchController searchController() {
        return new SearchController(searchResolver, configFetcher, log, contentModelOutputter());
    }

    @Bean
    TopicController topicController() {
        return new TopicController(new PublisherFilteringTopicResolver(topicResolver), new PublisherFilteringTopicContentLister(topicContentLister), configFetcher, log, topicModelOutputter(), queryController());
    }

    @Bean
    ProductController productController() {
        return new ProductController(productResolver, queryExecutor, configFetcher, log, productModelOutputter(), queryController());
    }

    @Bean
    ContentGroupController contentGroupController() {
        return new ContentGroupController(contentGroupResolver, queryExecutor, configFetcher, log, contentGroupOutputter(), queryController());
    }

    @Bean
    AtlasModelWriter<QueryResult<Content, ? extends Identified>> contentModelOutputter() {
        return this.<QueryResult<Content, ? extends Identified>>standardWriter(
                new SimpleContentModelWriter(new JsonTranslator<ContentQueryResult>(), itemModelSimplifier(), containerSimplifier(), topicSimplifier(), productSimplifier()),
                new SimpleContentModelWriter(new JaxbXmlTranslator<ContentQueryResult>(), itemModelSimplifier(), containerSimplifier(), topicSimplifier(), productSimplifier()));
    }

    @Bean
    ContainerModelSimplifier containerSimplifier() {
        AvailableChildrenResolver availableChildren = new MongoAvailableChildrenResolver(mongo);
        UpcomingChildrenResolver upcomingChildren = new MongoUpcomingChildrenResolver(mongo);
        RecentlyBroadcastChildrenResolver recentChildren = new MongoRecentlyBroadcastChildrenResolver(mongo);
        ContainerModelSimplifier containerSimplier = new ContainerModelSimplifier(itemModelSimplifier(), localHostName, contentGroupResolver, topicResolver, availableChildren, upcomingChildren, productResolver, recentChildren);
        containerSimplier.exposeIds(Boolean.valueOf(exposeIds));
        return containerSimplier;
    }

    @Bean
    ItemModelSimplifier itemModelSimplifier() {
        NumberToShortStringCodec idCodec = new SubstitutionTableNumberCodec();
        ContainerSummaryResolver containerSummary = new MongoContainerSummaryResolver(mongo);
        ItemModelSimplifier itemSimplifier = new ItemModelSimplifier(localHostName, contentGroupResolver, topicResolver, productResolver, segmentResolver, containerSummary, channelResolver, idCodec);
        itemSimplifier.exposeIds(Boolean.valueOf(exposeIds));
        return itemSimplifier;
    }

    @Bean
    AtlasModelWriter<Iterable<Person>> personModelOutputter() {
        return this.<Iterable<Person>>standardWriter(
                new SimplePersonModelWriter(new JsonTranslator<PeopleQueryResult>()),
                new SimplePersonModelWriter(new JaxbXmlTranslator<PeopleQueryResult>()));
    }

    @Bean
    AtlasModelWriter<Iterable<ScheduleChannel>> scheduleChannelModelOutputter() {
        return this.<Iterable<ScheduleChannel>>standardWriter(
                new SimpleScheduleModelWriter(new JsonTranslator<ScheduleQueryResult>(), itemModelSimplifier(), channelSimplifier()),
                new SimpleScheduleModelWriter(new JaxbXmlTranslator<ScheduleQueryResult>(), itemModelSimplifier(), channelSimplifier()));
    }

    @Bean
    AtlasModelWriter<Iterable<Topic>> topicModelOutputter() {
        TopicModelSimplifier topicModelSimplifier = topicSimplifier();
        return this.<Iterable<Topic>>standardWriter(
                new SimpleTopicModelWriter(new JsonTranslator<TopicQueryResult>(), contentResolver, topicModelSimplifier),
                new SimpleTopicModelWriter(new JaxbXmlTranslator<TopicQueryResult>(), contentResolver, topicModelSimplifier));
    }

    @Bean
    AtlasModelWriter<Iterable<Product>> productModelOutputter() {
        ProductModelSimplifier modelSimplifier = productSimplifier();
        return this.<Iterable<Product>>standardWriter(
                new SimpleProductModelWriter(new JsonTranslator<ProductQueryResult>(), contentResolver, modelSimplifier),
                new SimpleProductModelWriter(new JaxbXmlTranslator<ProductQueryResult>(), contentResolver, modelSimplifier));
    }

    @Bean
    AtlasModelWriter<Iterable<ContentGroup>> contentGroupOutputter() {
        ContentGroupModelSimplifier modelSimplifier = contentGroupSimplifier();
        return this.<Iterable<ContentGroup>>standardWriter(
                new SimpleContentGroupModelWriter(new JsonTranslator<ContentGroupQueryResult>(), modelSimplifier),
                new SimpleContentGroupModelWriter(new JaxbXmlTranslator<ContentGroupQueryResult>(), modelSimplifier));
    }

    @Bean
    ContentGroupModelSimplifier contentGroupSimplifier() {
        ContentGroupModelSimplifier contentGroupModelSimplifier = new ContentGroupModelSimplifier();
        return contentGroupModelSimplifier;
    }

    @Bean
    TopicModelSimplifier topicSimplifier() {
        TopicModelSimplifier topicModelSimplifier = new TopicModelSimplifier(localHostName);
        return topicModelSimplifier;
    }

    @Bean
    ProductModelSimplifier productSimplifier() {
        ProductModelSimplifier productModelSimplifier = new ProductModelSimplifier(localHostName);
        return productModelSimplifier;
    }

    private <I extends Iterable<?>> AtlasModelWriter<I> standardWriter(AtlasModelWriter<I> jsonWriter, AtlasModelWriter<I> xmlWriter) {
        return DispatchingAtlasModelWriter.<I>dispatchingModelWriter().register(new RdfXmlTranslator<I>(), "rdf.xml", MimeType.APPLICATION_RDF_XML).register(jsonWriter, "json", MimeType.APPLICATION_JSON).register(xmlWriter, "xml", MimeType.APPLICATION_XML).build();
    }
}
