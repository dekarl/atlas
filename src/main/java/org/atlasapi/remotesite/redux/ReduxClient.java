package org.atlasapi.remotesite.redux;

import java.util.List;

import org.atlasapi.remotesite.redux.model.BaseReduxProgramme;
import org.atlasapi.remotesite.redux.model.FullReduxProgramme;
import org.atlasapi.remotesite.redux.model.PaginatedBaseProgrammes;
import org.joda.time.LocalDate;

import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.query.Selection;

public interface ReduxClient {

    List<BaseReduxProgramme> programmesForDay(LocalDate date) throws HttpException, Exception;
    
    FullReduxProgramme programmeFor(String diskRef) throws HttpException, Exception;
    
    PaginatedBaseProgrammes latest(Selection selection) throws HttpException, Exception;

    PaginatedBaseProgrammes latest(Selection selection, Iterable<String> channels) throws HttpException, Exception;
    
}
