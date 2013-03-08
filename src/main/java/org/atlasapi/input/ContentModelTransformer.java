package org.atlasapi.input;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.List;
import java.util.Set;

import org.atlasapi.media.entity.Actor;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.CrewMember;
import org.atlasapi.media.entity.CrewMember.Role;
import org.atlasapi.media.entity.Described;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.LookupRef;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.media.entity.Topic;
import org.atlasapi.media.entity.Topic.Type;
import org.atlasapi.media.entity.TopicRef;
import org.atlasapi.media.entity.TopicRef.Relationship;
import org.atlasapi.media.entity.simple.Description;
import org.atlasapi.media.entity.simple.Person;
import org.atlasapi.media.entity.simple.PublisherDetails;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.atlasapi.persistence.topic.TopicStore;
import org.joda.time.DateTime;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.time.Clock;

public abstract class ContentModelTransformer<F extends Description,T extends Content> implements ModelTransformer<F, T> {

    private final ContentResolver resolver;
    private final TopicStore topicStore;
    protected final Clock clock;

    public ContentModelTransformer(ContentResolver resolver, TopicStore topicStore, Clock clock) {
        this.resolver = resolver;
        this.topicStore = topicStore;
        this.clock = clock;
    }
    
    @Override
    public T transform(F simple) {
        DateTime now = clock.now();
        T output = createOutput(simple, now);
        output.setLastUpdated(now);
        return setContentFields(output, simple);
    }

    protected abstract T createOutput(F simple, DateTime now);

    private T setContentFields(T result, Description inputContent) {
        result.setCanonicalUri(inputContent.getUri());
        result.setCurie(inputContent.getCurie());
        Publisher publisher = getPublisher(inputContent.getPublisher());
        result.setPublisher(publisher);
        result.setTitle(inputContent.getTitle());
        result.setDescription(inputContent.getDescription());
        result.setImage(inputContent.getImage());
        result.setThumbnail(inputContent.getThumbnail());
        if (inputContent.getSpecialization() != null) {
            result.setSpecialization(Specialization.fromKey(inputContent.getSpecialization()).valueOrNull());
        }
        if (inputContent.getMediaType() != null) {
            result.setMediaType(MediaType.valueOf(inputContent.getMediaType().toUpperCase()));
        }
        result.setPeople(transformPeople(inputContent.getPeople(), publisher));
        result.setEquivalentTo(resolveEquivalences(inputContent.getSameAs()));
        result.setTopicRefs(topicRefs(inputContent.getTopics()));
        return result;
    }

    private Iterable<TopicRef> topicRefs(Set<org.atlasapi.media.entity.simple.TopicRef> topics) {
        return ImmutableSet.copyOf(Iterables.transform(topics, new Function<org.atlasapi.media.entity.simple.TopicRef, TopicRef>() {

            @Override
            public TopicRef apply(org.atlasapi.media.entity.simple.TopicRef input) {
                org.atlasapi.media.entity.simple.Topic inputTopic = input.getTopic();
                String value = inputTopic.getValue();
                String namespace = inputTopic.getNamespace();
                Publisher publisher = getPublisher(inputTopic.getPublisher());
                if (isNullOrEmpty(value) || isNullOrEmpty(namespace)) {
                    throw new IllegalArgumentException("Topic missing value or namespace");
                }
                Maybe<Topic> possibleTopic = topicStore.topicFor(publisher, namespace, value);
                if (possibleTopic.hasValue()) {
                    Topic topic = possibleTopic.requireValue();
                    updateTopic(inputTopic, topic);
                    topicStore.write(topic);
                    return new TopicRef(
                        topic, 
                        input.getWeighting(),
                        input.isSupervised(), 
                        Relationship.fromString(input.getRelationship()).orNull(),
                        input.getOffset()
                    );
                } else {
                    throw new IllegalStateException(
                        String.format("No topic for %s/%s/%s",publisher,namespace,value)
                    );
                }
            }

            private void updateTopic(org.atlasapi.media.entity.simple.Topic inputTopic, Topic topic) {
                if (inputTopic.getType() != null && topic.getType() == null) {
                    topic.setType(Type.fromKey(inputTopic.getType()));
                }
                if (topic.getTitle() == null) {
                    topic.setTitle(inputTopic.getTitle());
                }
                if (topic.getDescription() == null) {
                    topic.setDescription(inputTopic.getDescription());
                }
                if (topic.getImage() == null) {
                    topic.setImage(inputTopic.getImage());
                }
                if (topic.getThumbnail() == null) {
                    topic.setThumbnail(inputTopic.getThumbnail());
                }
            }
            
        }));
    }

    private Set<LookupRef> resolveEquivalences(Set<String> sameAs) {
        ResolvedContent resolvedContent = resolver.findByCanonicalUris(sameAs);
        List<Identified> identified = resolvedContent.getAllResolvedResults();
        Iterable<Described> described = Iterables.filter(identified,Described.class);
        return ImmutableSet.copyOf(Iterables.transform(described,LookupRef.FROM_DESCRIBED));
    }

    private List<CrewMember> transformPeople(List<Person> people, Publisher publisher) {
        List<CrewMember> crew = Lists.newArrayListWithCapacity(people.size());
        for (Person person : people) {
            crew.add(transformPerson(person, publisher));
        }
        return crew;
    }

    private CrewMember transformPerson(Person person, Publisher publisher) {
        CrewMember member;
        checkNotNull(person.getUri(), "person requires uri");
        if ("actor".equals(person.getType())) {
            member = new Actor().withCharacter(person.getCharacter());
        } else {
            member = new CrewMember().withRole(Role.fromPossibleKey(person.getRole()).valueOrNull());
        }
        member.withPublisher(publisher);
        member.withName(person.getName());
        // TODO new alias
        member.setAliasUrls(person.getAliases());
        member.setCanonicalUri(person.getUri());
        member.setCurie(person.getCurie());
        return member;
    }

    protected Publisher getPublisher(PublisherDetails pubDets) {
        if (pubDets == null || pubDets.getKey() == null) {
            throw new IllegalArgumentException("missing publisher");
        }
        Maybe<Publisher> possiblePublisher = Publisher.fromKey(pubDets.getKey());
        if (possiblePublisher.isNothing()) {
            throw new IllegalArgumentException("unknown publisher " + pubDets.getKey());
        }
        return possiblePublisher.requireValue();
    }
}
