package org.atlasapi.remotesite.youview;

import nu.xom.Element;

public interface YouViewDataProcessor<T> {
    boolean process(Element element);
    
    T getResult();
}
