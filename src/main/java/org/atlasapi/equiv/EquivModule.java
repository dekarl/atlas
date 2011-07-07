/* Copyright 2010 Meta Broadcast Ltd

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. */

package org.atlasapi.equiv;

import static org.atlasapi.equiv.results.EquivalenceResultBuilder.resultBuilder;
import static org.atlasapi.equiv.results.extractors.FilteringEquivalenceExtractor.filteringExtractor;

import javax.annotation.PostConstruct;

import org.atlasapi.equiv.generators.BroadcastMatchingItemEquivalenceGenerator;
import org.atlasapi.equiv.generators.ContentEquivalenceGenerator;
import org.atlasapi.equiv.generators.EquivalenceGenerators;
import org.atlasapi.equiv.generators.FilmEquivalenceGenerator;
import org.atlasapi.equiv.generators.ScalingScoringGenerator;
import org.atlasapi.equiv.generators.TitleMatchingEquivalenceScoringGenerator;
import org.atlasapi.equiv.results.BroadcastingEquivalenceResultHandler;
import org.atlasapi.equiv.results.EquivalenceResultBuilder;
import org.atlasapi.equiv.results.EquivalenceResultHandler;
import org.atlasapi.equiv.results.LookupWritingEquivalenceHandler;
import org.atlasapi.equiv.results.ResultWritingEquivalenceHandler;
import org.atlasapi.equiv.results.combining.EquivalenceCombiner;
import org.atlasapi.equiv.results.combining.NullScoreAwareAveragingCombiner;
import org.atlasapi.equiv.results.extractors.EquivalenceExtractor;
import org.atlasapi.equiv.results.extractors.EquivalenceFilter;
import org.atlasapi.equiv.results.extractors.MinimumScoreEquivalenceExtractor;
import org.atlasapi.equiv.results.extractors.PercentThresholdEquivalenceExtractor;
import org.atlasapi.equiv.results.extractors.PublisherFilteringExtractor;
import org.atlasapi.equiv.results.persistence.MongoEquivalenceResultStore;
import org.atlasapi.equiv.results.persistence.RecentEquivalenceResultStore;
import org.atlasapi.equiv.results.probe.EquivalenceProbeStore;
import org.atlasapi.equiv.results.probe.EquivalenceResultProbeController;
import org.atlasapi.equiv.results.probe.MongoEquivalenceProbeStore;
import org.atlasapi.equiv.results.scores.ScoredEquivalent;
import org.atlasapi.equiv.results.www.EquivalenceResultController;
import org.atlasapi.equiv.results.www.RecentResultController;
import org.atlasapi.equiv.scorers.ContentEquivalenceScorer;
import org.atlasapi.equiv.scorers.EquivalenceScorers;
import org.atlasapi.equiv.scorers.SequenceItemEquivalenceScorer;
import org.atlasapi.equiv.scorers.TitleMatchingItemEquivalenceScorer;
import org.atlasapi.equiv.update.ContainerEquivalenceUpdater;
import org.atlasapi.equiv.update.ContentEquivalenceUpdater;
import org.atlasapi.equiv.update.ItemEquivalenceUpdater;
import org.atlasapi.equiv.update.ResultHandlingEquivalenceUpdater;
import org.atlasapi.equiv.update.RootEquivalenceUpdater;
import org.atlasapi.equiv.update.tasks.ContentEquivalenceUpdateTask;
import org.atlasapi.equiv.update.tasks.FilmEquivalenceUpdateTask;
import org.atlasapi.equiv.update.tasks.FilteringContentLister;
import org.atlasapi.equiv.update.www.ContentEquivalenceUpdateController;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.SearchResolver;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.content.schedule.mongo.MongoScheduleStore;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.lookup.LookupWriter;
import org.atlasapi.persistence.lookup.TransitiveLookupWriter;
import org.atlasapi.persistence.lookup.mongo.MongoLookupEntryStore;
import org.atlasapi.query.content.schedule.ManualScheduleUpdateController;
import org.joda.time.Duration;
import org.joda.time.LocalTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.scheduling.RepetitionRule;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.SimpleScheduler;
import com.metabroadcast.common.time.DayOfWeek;

@Configuration
public class EquivModule {

	private @Autowired DatabasedMongo db;
	private @Value("${equiv.updater.enabled}") String updaterEnabled;

	private static final RepetitionRule EQUIVALENCE_REPETITION = RepetitionRules.weekly(DayOfWeek.MONDAY, new LocalTime(9, 00));
    
    private @Autowired MongoScheduleStore scheduleResolver;
    private @Autowired SearchResolver searchResolver;
    private @Autowired ContentLister contentLister;
    private @Autowired ContentResolver contentResolver;
    private @Autowired DatabasedMongo mongo;
    private @Autowired AdapterLog log;
    private @Autowired SimpleScheduler taskScheduler;

    public @Bean RecentEquivalenceResultStore equivalenceResultStore() {
        return new RecentEquivalenceResultStore(new MongoEquivalenceResultStore(db));
    }
    
    public @Bean ItemEquivalenceUpdater<Item> basicItemUpdater() {
        EquivalenceGenerators<Item> itemGenerators = EquivalenceGenerators.from(ImmutableSet.<ContentEquivalenceGenerator<Item>>of(
                new BroadcastMatchingItemEquivalenceGenerator(scheduleResolver, ImmutableSet.copyOf(Publisher.values()), Duration.standardMinutes(10))
        ),log);
        
        EquivalenceScorers<Item> itemScorers = EquivalenceScorers.from(ImmutableSet.<ContentEquivalenceScorer<Item>>of(
                new TitleMatchingItemEquivalenceScorer(),
                new SequenceItemEquivalenceScorer()
        ),log);
        
        EquivalenceResultBuilder<Item> resultBuilder = standardResultBuilder();
        
        return new ItemEquivalenceUpdater<Item>(itemGenerators, itemScorers, resultBuilder);
    }
    
    private <T extends Content> ContentEquivalenceUpdater<T> writingContentUpdater(ContentEquivalenceUpdater<T> delegate) {
        return new ResultHandlingEquivalenceUpdater<T>(delegate, this.<T>standardResultHandlers());
    }

    private <T extends Content> EquivalenceResultHandler<T> standardResultHandlers() {
        return new BroadcastingEquivalenceResultHandler<T>(ImmutableList.of(
                new LookupWritingEquivalenceHandler<T>(lookupWriter()),
                new ResultWritingEquivalenceHandler<T>(equivalenceResultStore())
        ));
    }

    private <T extends Content> EquivalenceResultBuilder<T> standardResultBuilder() {
        EquivalenceCombiner<T> combiner = new NullScoreAwareAveragingCombiner<T>();
        
        EquivalenceExtractor<T> extractor = PercentThresholdEquivalenceExtractor.<T> fromPercent(90);
        extractor = filteringExtractor(extractor, new EquivalenceFilter<T>() {
            @Override
            public boolean apply(ScoredEquivalent<T> input, T target) {
                return input.score().isRealScore() && input.score().asDouble() > 0.2;
            }
        });
        extractor = filteringExtractor(extractor, new EquivalenceFilter<T>() {
            @Override
            public boolean apply(ScoredEquivalent<T> input, T target) {
                T equivalent = input.equivalent();
                return (equivalent.getSpecialization() == null || target.getSpecialization() == null || Objects.equal(equivalent.getSpecialization(), target.getSpecialization())); 
                    //&& (equivalent.getMediaType() == null || target.getMediaType() == null || Objects.equal(equivalent.getMediaType(), target.getMediaType()));
            }
        });
        extractor = new PublisherFilteringExtractor<T>(extractor);
        
        return resultBuilder(combiner, MinimumScoreEquivalenceExtractor.minimumFrom(extractor, 0.2));
    }
    
    public @Bean ContainerEquivalenceUpdater containerUpdater() {
        ScalingScoringGenerator<Container> titleScoringGenerator = ScalingScoringGenerator.from(new TitleMatchingEquivalenceScoringGenerator(searchResolver), new Function<Double, Double>() {
            @Override
            public Double apply(Double input) {
                return input > 0 ? input / 2 : input;
            }
        });
        
        EquivalenceResultBuilder<Container> containerResultBuilder = standardResultBuilder();
        EquivalenceResultHandler<Item> standardResultHandlers = standardResultHandlers();
        
        ContainerEquivalenceUpdater containerUpdater = new ContainerEquivalenceUpdater(contentResolver, basicItemUpdater(), containerResultBuilder,standardResultHandlers, log)
            .withEquivalenceGenerators(EquivalenceGenerators.from(ImmutableSet.<ContentEquivalenceGenerator<Container>>of(titleScoringGenerator),log))
            .withEquivalenceScorers(EquivalenceScorers.from(ImmutableSet.<ContentEquivalenceScorer<Container>>of(titleScoringGenerator),log));
        
        return containerUpdater;
    }

    public @Bean RootEquivalenceUpdater contentUpdater() {
        return new RootEquivalenceUpdater(writingContentUpdater(containerUpdater()), writingContentUpdater(basicItemUpdater()));
    }
    
    public @Bean LookupWriter lookupWriter() {
        return new TransitiveLookupWriter(new MongoLookupEntryStore(db));
    }
    
    private ContentEquivalenceUpdateTask publisherUpdateTask(final Publisher publisher) {
        ContentLister filteringLister = new FilteringContentLister(contentLister, new Predicate<Content>() {
            @Override
            public boolean apply(Content input) {
                if(Publisher.PA == publisher && publisher.equals(input.getPublisher())) {
                    return !(input instanceof Film); 
                }
                return true;
            }
        });
        return new ContentEquivalenceUpdateTask(filteringLister, contentUpdater(), log, db).forPublisher(publisher);
    }
    
    public @Bean FilmEquivalenceUpdateTask filmUpdateTask() {
        EquivalenceGenerators<Film> generators = EquivalenceGenerators.from(ImmutableSet.<ContentEquivalenceGenerator<Film>>of(
                new FilmEquivalenceGenerator(searchResolver)
        ),log);
        EquivalenceScorers<Film> scorers = EquivalenceScorers.from(ImmutableSet.<ContentEquivalenceScorer<Film>>of(), log);
        EquivalenceResultBuilder<Film> resultBuilder = standardResultBuilder();
        
        ContentEquivalenceUpdater<Film> updater = new ItemEquivalenceUpdater<Film>(generators, scorers, resultBuilder);
        return new FilmEquivalenceUpdateTask(contentLister, writingContentUpdater(updater), log, db);
    }
    
    @PostConstruct
    public void scheduleUpdater() {
        if(Boolean.parseBoolean(updaterEnabled)) {
            taskScheduler.schedule(publisherUpdateTask(Publisher.PA).withName("PA Equivalence Updater"), EQUIVALENCE_REPETITION);
            taskScheduler.schedule(publisherUpdateTask(Publisher.BBC).withName("BBC Equivalence Updater"), EQUIVALENCE_REPETITION);
            taskScheduler.schedule(publisherUpdateTask(Publisher.C4).withName("C4 Equivalence Updater"), EQUIVALENCE_REPETITION);
            taskScheduler.schedule(filmUpdateTask().withName("Film Equivalence Updater"), EQUIVALENCE_REPETITION);
            taskScheduler.schedule(new ChildRefUpdateTask(contentLister, mongo).withName("Child Ref Update"), RepetitionRules.NEVER);
        }
    }
    
    //Controllers...
    public @Bean ContentEquivalenceUpdateController updateController() {
        return new ContentEquivalenceUpdateController(contentUpdater(), contentResolver, log);
    }
    
    public @Bean EquivalenceResultController resultController() {
        return new EquivalenceResultController(equivalenceResultStore(), equivProbeStore(), contentResolver);
    }
    
    public @Bean RecentResultController recentController() {
        return new RecentResultController(equivalenceResultStore());
    }
    
    //Probes...
    public @Bean EquivalenceProbeStore equivProbeStore() { 
        return new MongoEquivalenceProbeStore(db);
    }
    
    public @Bean EquivalenceResultProbeController equivProbeController() {
        return new EquivalenceResultProbeController(equivalenceResultStore(), equivProbeStore());
    }

    @Bean ManualScheduleUpdateController scheduleUpdateController() {
        return new ManualScheduleUpdateController(scheduleResolver, contentResolver);
    }
    
    
}
