package org.atlasapi.remotesite.channel4.pmlsd;

import org.atlasapi.genres.AtlasGenre;
import org.atlasapi.genres.GenreMap;

public class C4CategoryGenreMap extends GenreMap {

    static {
        genres.put("http://www.channel4.com/programmes/categories/animals", AtlasGenre.ANIMALS);
        genres.put("http://www.channel4.com/programmes/categories/animation", AtlasGenre.ANIMATION);
        genres.put("http://www.channel4.com/programmes/categories/art-design-and-literature", AtlasGenre.FACTUAL);
        genres.put("http://www.channel4.com/programmes/categories/business-and-money", AtlasGenre.FACTUAL);
        genres.put("http://www.channel4.com/programmes/categories/chat-shows", AtlasGenre.ENTERTAINMENT);
        genres.put("http://www.channel4.com/programmes/categories/childrens-shows", AtlasGenre.CHILDRENS);
        genres.put("http://www.channel4.com/programmes/categories/comedy", AtlasGenre.COMEDY);
        genres.put("http://www.channel4.com/programmes/categories/disability", AtlasGenre.FACTUAL);
        genres.put("http://www.channel4.com/programmes/categories/documentaries", AtlasGenre.FACTUAL);
        genres.put("http://www.channel4.com/programmes/categories/drama", AtlasGenre.DRAMA);
        genres.put("http://www.channel4.com/programmes/categories/education-and-learning", AtlasGenre.LEARNING);
        genres.put("http://www.channel4.com/programmes/categories/entertainment", AtlasGenre.ENTERTAINMENT);
        genres.put("http://www.channel4.com/programmes/categories/family-and-parenting", AtlasGenre.LIFESTYLE);
        genres.put("http://www.channel4.com/programmes/categories/fashion-and-beauty", AtlasGenre.LIFESTYLE);
        genres.put("http://www.channel4.com/programmes/categories/film", AtlasGenre.FILM);
        genres.put("http://www.channel4.com/programmes/categories/food", AtlasGenre.LIFESTYLE);
        genres.put("http://www.channel4.com/programmes/categories/health-and-wellbeing", AtlasGenre.LIFESTYLE);
        genres.put("http://www.channel4.com/programmes/categories/history", AtlasGenre.FACTUAL);
        genres.put("http://www.channel4.com/programmes/categories/homes-and-gardens", AtlasGenre.LIFESTYLE);
        genres.put("http://www.channel4.com/programmes/categories/lifestyle", AtlasGenre.LIFESTYLE);
        genres.put("http://www.channel4.com/programmes/categories/music", AtlasGenre.MUSIC);
        genres.put("http://www.channel4.com/programmes/categories/news-current-affairs-and-politics", AtlasGenre.NEWS);
        genres.put("http://www.channel4.com/programmes/categories/religion-and-belief", AtlasGenre.FACTUAL);
        genres.put("http://www.channel4.com/programmes/categories/science-nature-and-the-environment", AtlasGenre.FACTUAL);
        genres.put("http://www.channel4.com/programmes/categories/sex-and-relationships", AtlasGenre.LIFESTYLE);
        genres.put("http://www.channel4.com/programmes/categories/society-and-culture", AtlasGenre.FACTUAL);
        genres.put("http://www.channel4.com/programmes/categories/sports-and-games", AtlasGenre.SPORT);
        genres.put("http://www.channel4.com/programmes/categories/factual", AtlasGenre.NEWS); // looks wrong, but it's a renamed news, current affairs and politics
        genres.put("http://www.channel4.com/programmes/categories/sport", AtlasGenre.SPORT);
    }

}
