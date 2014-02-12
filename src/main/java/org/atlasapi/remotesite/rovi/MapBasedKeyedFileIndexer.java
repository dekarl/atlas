package org.atlasapi.remotesite.rovi;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;


/**
 * An implementation that creates an index of type {@link MapBasedKeyedFileIndex}
 *
 */
public class MapBasedKeyedFileIndexer<T, S extends KeyedLine<T>> implements KeyedFileIndexer<T, S>{
    private final static Logger LOG = LoggerFactory.getLogger(MapBasedKeyedFileIndexer.class);
    

    private final File file;
    private final RoviLineParser<S> parser;
    private final Charset charset;

    public MapBasedKeyedFileIndexer(File file, Charset charset, RoviLineParser<S> parser) {
        this.file = checkNotNull(file);
        this.parser = checkNotNull(parser);
        this.charset = checkNotNull(charset);
    }
    
    @Override
    public KeyedFileIndex<T, S> index()
            throws IOException {
        Multimap<T, PointerAndSize> indexMap = buildIndex();
        MapBasedKeyedFileIndex<T, S> index = new MapBasedKeyedFileIndex<T, S>(file, indexMap, charset, parser);
        
        return index;
    }
    
    private Multimap<T, PointerAndSize> buildIndex() throws IOException {
        final HashMultimap<T, PointerAndSize> indexMap = HashMultimap.create();
        
        LOG.info("Start indexing file {}", file.getAbsolutePath());
        
        RoviDataProcessingResult result = Files.readLines(file, charset, new RoviLineIndexer<>(parser, charset, indexMap));
        
        LOG.info("File {} indexed. Result: {}", file.getAbsolutePath(), result);

        return indexMap;
    }

}
