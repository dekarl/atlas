package org.atlasapi.output.simple;

import java.util.List;
import java.util.Set;

import org.atlasapi.application.ApplicationConfiguration;
import org.atlasapi.media.entity.ChildRef;
import org.atlasapi.media.entity.Person;
import org.atlasapi.media.entity.simple.ContentIdentifier;
import org.atlasapi.output.Annotation;

import com.google.common.collect.Lists;

public class PersonModelSimplifier extends DescribedModelSimplifier<Person, org.atlasapi.media.entity.simple.Person> {

    @Override
    public org.atlasapi.media.entity.simple.Person simplify(Person fullPerson, Set<Annotation> annotations, ApplicationConfiguration config) {
        org.atlasapi.media.entity.simple.Person person = new org.atlasapi.media.entity.simple.Person();

        person.setType(Person.class.getSimpleName());
        copyBasicDescribedAttributes(fullPerson, person, annotations);

        person.setName(fullPerson.getTitle());
        person.setProfileLinks(fullPerson.getAliases());
        person.setContent(simpleContentListFrom(fullPerson.getContents()));
        person.setGivenName(fullPerson.getGivenName());
        person.setFamilyName(fullPerson.getFamilyName());
        person.setGender(fullPerson.getGender());
        person.setBirthDate(fullPerson.getBirthDate());
        person.setBirthPlace(fullPerson.getBirthPlace());
        person.setQuotes(fullPerson.getQuotes());
        
        return person;
    }

    private List<ContentIdentifier> simpleContentListFrom(Iterable<ChildRef> contents) {
        List<ContentIdentifier> contentList = Lists.newArrayList();
        for (ChildRef ref : contents) {
            contentList.add(ContentIdentifier.identifierFor(ref));
        }
        return contentList;
    }
}
