package org.atlasapi.equiv.results.combining;

import java.util.List;

import org.atlasapi.equiv.results.ScoredEquivalents;
import org.atlasapi.media.entity.Content;

public abstract class FoldingEquivalenceCombiner<T extends Content> implements EquivalenceCombiner<T> {

    @Override
    public ScoredEquivalents<T> combine(List<ScoredEquivalents<T>> scoredEquivalents) {
        if(scoredEquivalents == null || scoredEquivalents.isEmpty()) {
            return null;
        }
        ScoredEquivalents<T> head = scoredEquivalents.get(0);
        for (ScoredEquivalents<T> tailElem : scoredEquivalents.subList(1, scoredEquivalents.size())) {
            head = combine(head, tailElem);
        }
        return head;
    }

    protected abstract ScoredEquivalents<T> combine(ScoredEquivalents<T> head, ScoredEquivalents<T> tailElem);

}