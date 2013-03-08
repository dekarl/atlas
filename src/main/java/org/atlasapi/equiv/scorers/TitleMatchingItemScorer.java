package org.atlasapi.equiv.scorers;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.equiv.results.description.ResultDescription;
import org.atlasapi.equiv.results.scores.DefaultScoredCandidates;
import org.atlasapi.equiv.results.scores.Score;
import org.atlasapi.equiv.results.scores.ScoredCandidates;
import org.atlasapi.equiv.results.scores.DefaultScoredCandidates.Builder;
import org.atlasapi.media.entity.Item;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class TitleMatchingItemScorer implements EquivalenceScorer<Item> {
    
    public static final String NAME = "Title";

    public enum TitleType {
        
        DATE("\\d{1,2}/\\d{1,2}/(\\d{2}|\\d{4})"),
        SEQUENCE("((?:E|e)pisode)(?:.*)(\\d+)"),
        DEFAULT(".*");
     
        private Pattern pattern;

        TitleType(String pattern) {
            this.pattern = Pattern.compile(pattern);
        }
        
        public static TitleType titleTypeOf(Item item) {
            return titleTypeOf(item.getTitle());
        }
        
        public static TitleType titleTypeOf(String title) {
            for (TitleType type : ImmutableList.copyOf(TitleType.values())) {
                if(type.matches(title)) {
                    return type;
                }
            }
            return DEFAULT;
        }


        private boolean matches(String title) {
            return pattern.matcher(title).matches();
        }
        
    }
    
    
    @Override
    public ScoredCandidates<Item> score(Item subject, Set<? extends Item> suggestions, ResultDescription desc) {
        Builder<Item> equivalents = DefaultScoredCandidates.fromSource(NAME);
        
        if(!Strings.isNullOrEmpty(subject.getTitle())) {
            for (Item suggestion : Iterables.filter(suggestions, Item.class)) {
                Score score;
                if(Strings.isNullOrEmpty(suggestion.getTitle())) {
                    score = Score.NULL_SCORE;
                    desc.appendText("No Title (%s) scored: %s", suggestion.getCanonicalUri(), score);
                } else {
                    score = score(subject, suggestion);
                    desc.appendText("%s (%s) scored: %s", suggestion.getTitle(), suggestion.getCanonicalUri(), score);
                }
                equivalents.addEquivalent(suggestion, score);
            }
        } else {
            desc.appendText("Item has no title");
        }
        
        return equivalents.build();
    }


    private Score score(Item subject, Item suggestion) {
        
        if(Strings.isNullOrEmpty(suggestion.getTitle())) {
            return Score.NULL_SCORE;
        }
        
        String subjTitle = removeSeq(subject.getTitle());
        String suggTitle = removeSeq(suggestion.getTitle());
        
        TitleType subjectType = TitleType.titleTypeOf(subject.getTitle());
        TitleType suggestionType = TitleType.titleTypeOf(suggestion.getTitle());
        
        subjTitle = removeCommonPrefixes(subjTitle.replaceAll(" & ", " and ").replaceAll("[^A-Za-z0-9\\s]+", "-").toLowerCase());
        suggTitle = removeCommonPrefixes(suggTitle.replaceAll(" & ", " and ").replaceAll("[^A-Za-z0-9\\s]+", "-").toLowerCase());
        
        if(subjectType == suggestionType && Objects.equal(subjTitle, suggTitle)) {
            return Score.valueOf(1.0);
        }
        
        return Score.NULL_SCORE;
    }
    
    private String removeCommonPrefixes(String alphaNumeric) {
        return (alphaNumeric.startsWith("the ") ? alphaNumeric.substring(4) : alphaNumeric).replace(" ", "-");
    }

    private final Pattern seqTitle = Pattern.compile("(\\d+)(\\s?[.:-]{1}\\s?)(.*)");
    
    private String removeSeq(String title) {
        Matcher matcher = seqTitle.matcher(title);
        if(matcher.matches()) {
            return matcher.group(3);
        }
        return title;
    }
    
    @Override
    public String toString() {
        return "Title-matching Item Scorer";
    }
}
