package org.atlasapi.query.v2;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.application.query.ApplicationConfigurationFetcher;
import org.atlasapi.content.criteria.ContentQuery;
import org.atlasapi.media.entity.Person;
import org.atlasapi.output.AtlasErrorSummary;
import org.atlasapi.output.AtlasModelWriter;
import org.atlasapi.persistence.content.PeopleResolver;
import org.atlasapi.persistence.logging.AdapterLog;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.common.collect.ImmutableList;

@Controller
public class PeopleController extends BaseController<Iterable<Person>> {

    private final PeopleResolver resolver;

    public PeopleController(PeopleResolver resolver, ApplicationConfigurationFetcher configFetcher,
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
            
            Person person = resolver.person(uri).valueOrNull();
            
            modelAndViewFor(request, response, ImmutableList.of(person), filter.getConfiguration());
        } catch (Exception e) {
            errorViewFor(request, response, AtlasErrorSummary.forException(e));
        }
    }
}
