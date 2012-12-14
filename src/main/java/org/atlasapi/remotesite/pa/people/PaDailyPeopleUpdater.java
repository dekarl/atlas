package org.atlasapi.remotesite.pa.people;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Unmarshaller.Listener;
import javax.xml.parsers.SAXParserFactory;

import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.persistence.content.PeopleResolver;
import org.atlasapi.persistence.content.people.PersonWriter;
import org.atlasapi.remotesite.pa.data.PaProgrammeDataStore;
import org.atlasapi.remotesite.pa.profiles.bindings.Person;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.XMLReader;

import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.scheduling.ScheduledTask;

public class PaDailyPeopleUpdater extends ScheduledTask {

    private static final String SERVICE = "PA";
    private static final Pattern FILE_DATE = Pattern.compile("^.*(\\d{8})_profiles.xml$");
    
    private final PaProgrammeDataStore store;
    private final PeopleResolver personResolver;
    private final PersonWriter personWriter;
    private final Logger log = LoggerFactory.getLogger(PaDailyPeopleUpdater.class);
    private final DateTimeFormatter dateFormatter = ISODateTimeFormat.basicDate();
    
    private PaPeopleProcessor processor;

    public PaDailyPeopleUpdater(PaProgrammeDataStore store, PeopleResolver personResolver, PersonWriter personWriter) {
        this.store = store;
        this.personResolver = personResolver;
        this.personWriter = personWriter;
    }
    
    @Override
    protected void runTask() {
        processor = new PaPeopleProcessor(personResolver, personWriter);
        processFiles(store.localProfilesFiles(Predicates.<File>alwaysTrue()), new LocalDate());
    }
    
    private void processFiles(Iterable<File> files, LocalDate date) {
        try { 
            JAXBContext context = JAXBContext.newInstance("org.atlasapi.remotesite.pa.profiles.bindings");
            Unmarshaller unmarshaller = context.createUnmarshaller();

            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XMLReader reader = factory.newSAXParser().getXMLReader();
            reader.setContentHandler(unmarshaller.getUnmarshallerHandler());

            int filesMatched = 0;
            int filesProcessed = 0;

            for (File file : files) {
                if(!shouldContinue()) {
                    break;
                }
                try {
                    final String filename = file.toURI().toString();
                    Matcher matcher = FILE_DATE.matcher(filename);

                    if (matcher.matches()) {
                        filesMatched++;
                        DateTime fileDate = dateFormatter.parseDateTime(matcher.group(1));
                        if (fileDate.toLocalDate().isEqual(date)) {
                            log.info("Processing file " + file.toString());
                            final File fileToProcess = store.copyForProcessing(file);

                            unmarshaller.setListener(peopleProcessingListener());
                            reader.parse(fileToProcess.toURI().toString());

                            filesProcessed++;
                            FileUploadResult.successfulUpload(SERVICE, file.getName());
                        } else {
                            log.info("Skipping file " + file.toString() + " as it isn't today's file");
                            FileUploadResult.failedUpload(SERVICE, file.getName()).withMessage("Not today's date");
                        }
                    }
                    else {
                        log.info("Not processing file " + file.toString() + " as filename format is not recognised");
                        FileUploadResult.failedUpload(SERVICE, file.getName()).withMessage("Format not recognised");
                    }
                } catch (Exception e) {
                    FileUploadResult.failedUpload(SERVICE, file.getName()).withCause(e);
                    log.error("Error processing file " + file.toString(), e);
                }
            }

            reportStatus(String.format("found profiles files, processed %s file", filesMatched, filesProcessed));
        } catch (Exception e) {
            log.error("Exception running PA People updater", e);
            // this will stop the task
            Throwables.propagate(e);
        }
    }

    private Listener peopleProcessingListener() {
        return new Unmarshaller.Listener() {
            public void beforeUnmarshal(Object target, Object parent) {
            }

            public void afterUnmarshal(Object target, Object parent) {
                if (target instanceof Person) {
                    processor.process(((Person) target));
                }
            }
        };
    }
}
