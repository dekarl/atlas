package org.atlasapi.remotesite.bbc;

import static org.atlasapi.media.entity.Topic.topicUriForId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import junit.framework.TestCase;

import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Topic;
import org.atlasapi.media.entity.Topic.Type;
import org.atlasapi.persistence.logging.NullAdapterLog;
import org.atlasapi.persistence.topic.TopicStore;
import org.atlasapi.remotesite.bbc.SlashProgrammesRdf.SlashProgrammesDescription;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.base.Maybe;

@RunWith(JMock.class)
public class BbcSlashProgrammesRdfTopicExtractorTest extends TestCase {

    private final Mockery context = new Mockery();
    private final TopicStore topicStore = context.mock(TopicStore.class);
    private final BbcSlashProgrammesRdfTopicExtractor extractor = new BbcSlashProgrammesRdfTopicExtractor(topicStore , new NullAdapterLog());

    @Test
    public void testExtractsTopicFromValidSlashProgrammesTopic() {
        
        final String topicUri = "http://dbpedia.org/resource/Religion";
        String typeUri = "http://purl.org/ontology/po/Person";
        
        SlashProgrammesRdf rdf = new SlashProgrammesRdf().withDescription(new SlashProgrammesDescription().withSameAs(
                ImmutableSet.of(new SlashProgrammesRdf.SlashProgrammesSameAs().withResourceUri(topicUri))
        ).withTypes(
                ImmutableSet.of(new SlashProgrammesRdf.SlashProgrammesType().withResourceUri(typeUri))
        ));
        
        context.checking(new Expectations(){{
            one(topicStore).topicFor("dbpedia", topicUri);will(returnValue(Maybe.just(new Topic(topicUriForId("100")))));
            one(topicStore).write(with(any(Topic.class)));
        }});
        
        Maybe<Topic> extractedTopic = extractor.extract(rdf);
        
        assertTrue(extractedTopic.hasValue());
        assertThat(extractedTopic.requireValue().getValue(), is(equalTo(topicUri)));
        assertThat(extractedTopic.requireValue().getType(), is(equalTo(Type.PERSON)));
        assertThat(extractedTopic.requireValue().getPublishers(), hasItem(Publisher.BBC));
        assertThat(extractedTopic.requireValue().getNamespace(), is(equalTo("dbpedia")));
        assertThat(extractedTopic.requireValue().getTitle(), is(equalTo("Religion")));
        
    }
    
}