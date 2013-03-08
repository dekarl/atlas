package org.atlasapi.remotesite.facebook;

import static com.google.common.base.CharMatcher.JAVA_LOWER_CASE;
import static org.atlasapi.media.entity.Publisher.FACEBOOK;
import static org.atlasapi.remotesite.facebook.FacebookCanonicaliser.CANONICAL_PREFIX;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.media.entity.Actor;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.CrewMember;
import org.atlasapi.media.entity.CrewMember.Role;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.remotesite.ContentExtractor;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Maps;
import com.metabroadcast.common.url.UrlEncoding;

public class FacebookBrandExtractor implements ContentExtractor<FacebookPage, Brand> {

    private final Splitter websiteSplitter = Splitter
            .on(CharMatcher.WHITESPACE.or(CharMatcher.anyOf(";,")))
            .omitEmptyStrings()
            .trimResults();
    private final Splitter peopleSplitter = Splitter
            .on(Pattern.compile(" and |,|&|;"))
            .omitEmptyStrings()
            .trimResults(CharMatcher.WHITESPACE.or(CharMatcher.is('.')));
    
    @Override
    public Brand extract(FacebookPage source) {
        
        String id = source.getId();
        Brand brand = new Brand(CANONICAL_PREFIX+id, "fb:"+id, Publisher.FACEBOOK);
        brand.setTitle(source.getName());
        brand.setDescription(source.getPlotOutline());
        
        if (!Strings.isNullOrEmpty(source.getLink())) {
            // TODO new alias
            brand.addAliasUrl(source.getLink());
        }
        if (!Strings.isNullOrEmpty(source.getUsername())) {
            // TODO new alias
            brand.addAliasUrl(CANONICAL_PREFIX + source.getUsername());
        }
        // TODO new alias
        brand.addAliasUrls(extractUrls(source.getWebsite()));
        
        for (CrewMember director : extractDirectors(source.getDirectedBy())) {
            brand.addPerson(director);
        }
        
        for (CrewMember writer : extractWriters(source.getWrittenBy())) {
            brand.addPerson(writer);
        }
        
        for (CrewMember star : extractStars(source.getStarring())) {
            brand.addPerson(star);
        }
        
        return brand;
    }

    private Set<CrewMember> extractDirectors(String directedBy) {
        return extractCrewMembers(directedBy, CrewMember.Role.DIRECTOR);
    }

    private Set<CrewMember> extractWriters(String writtenBy) {
        return extractCrewMembers(writtenBy, CrewMember.Role.WRITER);
    }
    
    private Set<Actor> extractStars(String starring) {
        Builder<Actor> crewMembers = ImmutableSet.builder();
        Map<String, String> namedCharacters = extractNames(starring);
        for (Entry<String,String> namedCharacter : namedCharacters.entrySet()) {
            String name = namedCharacter.getKey();
            Actor actor = new Actor()
                .withName(name)
                .withCharacter(namedCharacter.getValue());
            actor.withPublisher(FACEBOOK);
            actor.setCanonicalUri(Publisher.FACEBOOK.key() + "/people/" + websafe(name));
            crewMembers.add(actor);
        }
        return crewMembers.build();
    }

    private Set<CrewMember> extractCrewMembers(String people, Role role) {
        Builder<CrewMember> crewMembers = ImmutableSet.builder();
        for (Entry<String,String> nameCharacter : extractNames(people).entrySet()) {
            String name = nameCharacter.getKey();
            CrewMember member = new CrewMember()
                .withName(name)
                .withRole(role)
                .withPublisher(FACEBOOK);
            member.setCanonicalUri(Publisher.FACEBOOK.key() + "/people/" + websafe(name));
            crewMembers.add(member);
        }
        return crewMembers.build();
    }

    private String websafe(String name) {
        return JAVA_LOWER_CASE.negate().removeFrom(name.replaceAll(" ", "-").toLowerCase());
    }

    private Map<String,String> extractNames(String people) {
        if (Strings.isNullOrEmpty(people)) {
            return ImmutableMap.of();
        }
        Map<String, String> personCharacters = Maps.newHashMap();
        for (String person : peopleSplitter.split(people)) {
            // Handles 'Actor Name as Character Name'
            String[] nameCharacter = person.split(" as ");
            if (nameCharacter.length == 2) {
                personCharacters.put(nameCharacter[0], nameCharacter[1]);
                continue;
            }
            // Handles ' Actor Name (Character Name)'
            Matcher matcher = Pattern.compile("(.*) \\(([^)]+)\\)").matcher(person);
            if (matcher.matches()) {
                personCharacters.put(matcher.group(1), matcher.group(2));
                continue;
            }
            //Probably not a name
            if (countSpaces(person) > 3) {
                continue;
            }
            //Default
            personCharacters.put(person, null);
        }
        return personCharacters;
    }

    private int countSpaces(String person) {
        int count = 0;
        for (int i = 0; i < person.length(); i++) {
            if (person.charAt(i) == ' ') {
                count++;
            }
        }
        return count;
    }

    private Iterable<String> extractUrls(String website) {
        if (Strings.isNullOrEmpty(website)) {
            return ImmutableSet.of();
        }
        ImmutableSet.Builder<String> urls = ImmutableSet.builder();
        for (String rawUrl : websiteSplitter.split(website)) {
            if (isProbablyUrl(rawUrl)) {
                if (rawUrl.contains("tradedoubler.com")) {
                    String extractedUrl = extractTarget(rawUrl);
                    if (extractedUrl != null) {
                        urls.add(extractedUrl);
                    }
                } else {
                    int paramStartIndex = rawUrl.indexOf('?');
                    if (paramStartIndex > 0) {
                        urls.add(rawUrl.substring(0, paramStartIndex));
                    } else {
                        urls.add(rawUrl);
                    }
                }
            }
        }
        return urls.build();
    }

    private boolean isProbablyUrl(String rawUrl) {
        return rawUrl.startsWith("http://") || rawUrl.startsWith("https://");
    }

    private String extractTarget(String rawUrl) {
        String queryString = rawUrl.substring(rawUrl.indexOf('?')+1);
        Map<String,String> params = UrlEncoding.decodeParams(queryString);
        return params.get("url");
    }

}
