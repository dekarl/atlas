package org.atlasapi.remotesite.pa.people;

import java.io.File;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Unmarshaller.Listener;
import javax.xml.parsers.SAXParserFactory;

import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.atlasapi.feeds.upload.persistence.FileUploadResultStore;
import org.atlasapi.persistence.content.PeopleResolver;
import org.atlasapi.persistence.content.people.PersonWriter;
import org.atlasapi.remotesite.pa.data.PaProgrammeDataStore;
import org.atlasapi.remotesite.pa.profiles.bindings.Person;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.XMLReader;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.scheduling.ScheduledTask;

public class PaDailyPeopleUpdater extends ScheduledTask {

    private static final String SERVICE = "PA";
    private static final Pattern FILE_DATE = Pattern.compile("^.*(\\d{8})_profiles.xml$");
    
    private final PaProgrammeDataStore store;
    private final PeopleResolver personResolver;
    private final PersonWriter personWriter;
    private final FileUploadResultStore fileUploadResultStore;
    private final Logger log = LoggerFactory.getLogger(PaDailyPeopleUpdater.class);
    private final DateTimeFormatter dateFormatter = ISODateTimeFormat.basicDate();
    private final Lock lock;
    
    private final Ordering<File> BY_FILE_DATE = new Ordering<File>() {
        @Override
        public int compare(File left, File right) {
            Matcher leftMatcher = FILE_DATE.matcher(left.toURI().toString());
            Matcher rightMatcher = FILE_DATE.matcher(right.toURI().toString());
            if (leftMatcher.matches()) {
                if (rightMatcher.matches()) {
                    DateTime leftTime = dateFormatter.parseDateTime(leftMatcher.group(1));
                    DateTime rightTime = dateFormatter.parseDateTime(rightMatcher.group(1));
                    return leftTime.compareTo(rightTime);
                } else {
                    return 1;
                }
            } else {
                if (rightMatcher.matches()) {
                    return -1;
                } else {
                    return left.getName().compareTo(right.getName());
                }
            }
        }
    };
    
    private PaPeopleProcessor processor;

    public PaDailyPeopleUpdater(PaProgrammeDataStore store, PeopleResolver personResolver, PersonWriter personWriter, FileUploadResultStore fileUploadResultStore, Lock lock) {
        this.store = store;
        this.fileUploadResultStore = fileUploadResultStore; 
        this.personResolver = personResolver;
        this.personWriter = personWriter;
        this.lock = lock;
    }
    
    @Override
    protected void runTask() {
        if (lock.tryLock()) {
            processor = new PaPeopleProcessor(personResolver, personWriter);
            processFiles(store.localProfilesFiles(new Predicate<File>() {
                @Override
                public boolean apply(File input) {
                    Maybe<FileUploadResult> result = fileUploadResultStore.latestResultFor(SERVICE, input.getName());
                    return (result.isNothing() || !FileUploadResultType.SUCCESS.equals(result.requireValue().type()));
                }
            }));
            lock.unlock();
        } else {
            reportStatus("Another PA People ingest is running, this task has not run");
        }
    }
    
    private void processFiles(Iterable<File> files) {
        try { 
            JAXBContext context = JAXBContext.newInstance("org.atlasapi.remotesite.pa.profiles.bindings");
            Unmarshaller unmarshaller = context.createUnmarshaller();

            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XMLReader reader = factory.newSAXParser().getXMLReader();
            reader.setContentHandler(unmarshaller.getUnmarshallerHandler());

            int filesProcessed = 0;
            
            Iterable<File> orderedFiles = BY_FILE_DATE.sortedCopy(files); 

            for (File file : orderedFiles) {
                if(!shouldContinue()) {
                    break;
                }
                try {
                    final String filename = file.toURI().toString();
                    Matcher matcher = FILE_DATE.matcher(filename);

                    if (matcher.matches()) {
                        log.info("Processing file " + file.toString());
                        final File fileToProcess = store.copyForProcessing(file);

                        unmarshaller.setListener(peopleProcessingListener());
                        reader.parse(fileToProcess.toURI().toString());

                        filesProcessed++;
                        FileUploadResult.successfulUpload(SERVICE, file.getName());
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

            reportStatus(String.format("found profiles files, processed %s file", Iterables.size(files), filesProcessed));
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
