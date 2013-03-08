package org.atlasapi.remotesite.pa.people;

import org.atlasapi.media.entity.LookupRef;
import org.atlasapi.media.entity.Person;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.PeopleResolver;
import org.atlasapi.persistence.content.people.PersonWriter;
import org.atlasapi.remotesite.pa.profiles.bindings.Name;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

public class PaPeopleProcessor {

    public static final String PERSON_URI_PREFIX = "http://people.atlasapi.org/people.pressassociation.com/";
    private static final String PA_PERSON_URI_PREFIX = "http://people.atlasapi.org/pressassociation.com/";
    
    private final PeopleResolver personResolver;
    private final PersonWriter personWriter;
    private final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.date();

    public PaPeopleProcessor(PeopleResolver personResolver, PersonWriter personWriter) {
        this.personResolver = personResolver;
        this.personWriter = personWriter;
    }
    
    public void process(org.atlasapi.remotesite.pa.profiles.bindings.Person paPerson) {
        Person person = ingestPerson(paPerson);
        Optional<Person> existing = personResolver.person(person.getCanonicalUri());
        if (!existing.isPresent()) {
            personWriter.createOrUpdatePerson(person);
        } else {
            merge(existing.get(), person);
            personWriter.createOrUpdatePerson(existing.get());
        }
    }
    
    private void merge(Person existing, Person newPerson) {
        existing.withName(newPerson.name());
        existing.setGivenName(newPerson.getGivenName());
        existing.setFamilyName(newPerson.getFamilyName());
        existing.setGender(newPerson.getGender());
        existing.setBirthDate(newPerson.getBirthDate());
        existing.setBirthPlace(newPerson.getBirthPlace());
        existing.setDescription(newPerson.getDescription());
        existing.setQuotes(newPerson.getQuotes());
        existing.setPublisher(Publisher.PA_PEOPLE);
    }

    private Person ingestPerson(org.atlasapi.remotesite.pa.profiles.bindings.Person paPerson) {
        Person person = new Person();
        person.setCanonicalUri(PERSON_URI_PREFIX + paPerson.getId());
        Name name = paPerson.getName();
        person.withName(name.getFirstname() + " " + name.getLastname());
        person.setGivenName(name.getFirstname());
        person.setFamilyName(name.getLastname());
        person.setGender(paPerson.getGender());
        if (paPerson.getBorn() != null) {
            person.setBirthDate(dateTimeFormatter.parseDateTime(paPerson.getBorn()));
        }
        if (paPerson.getBornIn() != null) {
            person.setBirthPlace(paPerson.getBornIn());
        }
        person.setDescription(paPerson.getEarlyLife() + "\n\n" + paPerson.getCareer());
        person.addQuote(paPerson.getQuote());
        person.setPublisher(Publisher.PA_PEOPLE);
        
        setDirectEquivalentToPAPerson(person, paPerson.getId());
        return person;
    }
    
    /**
     * PA People are ingested separately from PA biogs people. Therefore
     * we set a direct equivalence on the PA person if they exist. In the 
     * future this will change to an equivalence job so the equivalence
     * will be asserted at a later stage even if the PA person doesn't
     * exist at the time when the PA biog person is ingested.
     * 
     * @param person
     */
    private void setDirectEquivalentToPAPerson(Person person, String id) {
        Optional<Person> paPerson = personResolver.person(PA_PERSON_URI_PREFIX + id);
        if(paPerson.isPresent()) {
            person.setEquivalentTo(ImmutableSet.of(LookupRef.from(paPerson.get())));
        }
    }
}
