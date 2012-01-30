package org.atlasapi.output;

import java.util.Set;

import org.atlasapi.media.entity.Topic;
import org.atlasapi.media.entity.simple.TopicQueryResult;
import org.atlasapi.output.simple.TopicModelSimplifier;
import org.atlasapi.persistence.content.ContentResolver;

public class SimpleTopicModelWriter extends TransformingModelWriter<Iterable<Topic>, TopicQueryResult> {

    private final TopicModelSimplifier topicSimplifier;

    public SimpleTopicModelWriter(AtlasModelWriter<TopicQueryResult> delegate, ContentResolver contentResolver) {
        super(delegate);
        this.topicSimplifier = new TopicModelSimplifier();
    }
    
    @Override
    protected TopicQueryResult transform(Iterable<Topic> fullTopics, Set<Annotation> annotations) {
        TopicQueryResult result = new TopicQueryResult();
        for (Topic fullTopic : fullTopics) {
            result.add(topicSimplifier.simplify(fullTopic, annotations));
        }
        return result;
    }

}