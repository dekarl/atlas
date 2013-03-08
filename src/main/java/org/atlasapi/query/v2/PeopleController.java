package org.atlasapi.query.v2;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.application.query.ApplicationConfigurationFetcher;
import org.atlasapi.content.criteria.ContentQuery;
import org.atlasapi.media.entity.Person;
import org.atlasapi.output.AtlasErrorSummary;
import org.atlasapi.output.AtlasModelWriter;
import org.atlasapi.persistence.content.PeopleQueryResolver;
import org.atlasapi.persistence.content.PeopleResolver;
import org.atlasapi.persistence.logging.AdapterLog;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.http.HttpStatusCode;

@Controller
public class PeopleController extends BaseController<Iterable<Person>> {

    private static final AtlasErrorSummary NOT_FOUND = new AtlasErrorSummary(new NullPointerException())
        .withErrorCode("Person not found")
        .withStatusCode(HttpStatusCode.NOT_FOUND);
    private static final AtlasErrorSummary FORBIDDEN = new AtlasErrorSummary(new NullPointerException())
        .withStatusCode(HttpStatusCode.FORBIDDEN);

    private final PeopleQueryResolver resolver;

    public PeopleController(PeopleQueryResolver resolver, ApplicationConfigurationFetcher configFetcher,
                    AdapterLog log, AtlasModelWriter<Iterable<Person>> outputter) {
        super(configFetcher, log, outputter);
        this.resolver = resolver;
    }

    @RequestMapping("/3.0/people.*")
    public void content(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            ContentQuery filter = builder.build(request);

            String uri = request.getParameter("uri");
            if (uri == null) {
                throw new IllegalArgumentException("No uri specified");
            }
            
            Optional<Person> person = resolver.person(uri, appConfig(request));
            
            if(!person.isPresent()) {
                errorViewFor(request, response, NOT_FOUND);
                return;
            } 
            
            if(!appConfig(request).isEnabled(person.get().getPublisher())) {
                errorViewFor(request, response, FORBIDDEN);
                return;
            }
            
            modelAndViewFor(request, response, ImmutableList.of(person.get()), filter.getConfiguration());
        } catch (Exception e) {
            errorViewFor(request, response, AtlasErrorSummary.forException(e));
        }
    }
}
