package org.atlasapi.remotesite.rovi.model;

import java.util.Locale;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;


public class RoviCulture {
    
    private static final BiMap<String, Locale> cultureToLocale = HashBiMap.create();
    private static final Locale DEFAULT_LOCALE = new Locale("en", "GB");
    private static final String DEFAULT_CULTURE = "English - UK";
    
    static {
        cultureToLocale.put("Arabic Generic", new Locale("ar"));
        cultureToLocale.put("Basque Generic", new Locale("eu"));
        cultureToLocale.put("Cantonese Generic", new Locale("yue"));
        cultureToLocale.put("Catalán Generic", new Locale("ca"));
        cultureToLocale.put("Chinese Generic", new Locale("zh"));
        cultureToLocale.put("Croatian_Generic", new Locale("hr"));
        cultureToLocale.put("Danish Generic", new Locale("da"));
        cultureToLocale.put("Dutch Generic", new Locale("nl"));
        cultureToLocale.put("English - NA", new Locale("en", "US"));
        cultureToLocale.put("English - UK", new Locale("en", "GB"));
        cultureToLocale.put("Finnish Generic", new Locale("fi"));
        cultureToLocale.put("French - Québec", new Locale("fr", "CA"));
        cultureToLocale.put("French Generic", new Locale("fr", "FR"));
        cultureToLocale.put("Gallegan Generic", new Locale("gl"));
        cultureToLocale.put("German Generic", new Locale("de"));
        cultureToLocale.put("Greek Generic", new Locale("el"));
        cultureToLocale.put("Hindi Generic", new Locale("hi"));
        cultureToLocale.put("Hungarian Generic", new Locale("hu"));
        cultureToLocale.put("Irish Generic", new Locale("ga"));
        cultureToLocale.put("Italian Generic", new Locale("it"));
        cultureToLocale.put("Luxembourgish Generic", new Locale("lb"));
        cultureToLocale.put("Mandarin Generic", new Locale("cmn"));
        cultureToLocale.put("Norwegian Generic", new Locale("no"));
        cultureToLocale.put("Polish Generic", new Locale("pl"));
        cultureToLocale.put("Punjabi Generic", new Locale("pa"));
        cultureToLocale.put("Russian Generic", new Locale("ru"));
        cultureToLocale.put("Scots Generic", new Locale("sco"));
        cultureToLocale.put("Scottish Gaelic Generic", new Locale("gd"));
        cultureToLocale.put("Serbian_Generic", new Locale("sr"));
        cultureToLocale.put("Slovak Generic", new Locale("sk"));
        cultureToLocale.put("Spanish Generic", new Locale("es"));
        cultureToLocale.put("Swedish Generic", new Locale("sv"));
        cultureToLocale.put("Tagalog Generic", new Locale("tl"));
        cultureToLocale.put("Taiwanese Generic", new Locale("zh", "TW"));
        cultureToLocale.put("Tamil Generic", new Locale("ta"));
        cultureToLocale.put("Ukrainian Generic", new Locale("uk"));
        cultureToLocale.put("Vietnamese Generic", new Locale("vi"));
        cultureToLocale.put("Welsh Generic", new Locale("cy"));
    }
    
    public static Set<String> cultures() {
        return cultureToLocale.keySet();
    }
    
    public static Set<Locale> locales() {
        return cultureToLocale.values();
    }
    
    public static Locale localeFromCulture(String culture) {
        if (cultureToLocale.containsKey(culture)) {
            return cultureToLocale.get(culture);
        }
        
        return DEFAULT_LOCALE;
    }
    
    public static String cultureFromLocale(Locale locale) {
        BiMap<Locale, String> localeToCulture = cultureToLocale.inverse();
        
        if (localeToCulture.containsKey(locale)) {
            return localeToCulture.get(locale);
        }
        
        return DEFAULT_CULTURE;
    }

}
