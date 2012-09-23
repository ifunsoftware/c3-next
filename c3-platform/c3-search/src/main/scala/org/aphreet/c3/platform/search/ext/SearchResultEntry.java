package org.aphreet.c3.platform.search.ext;

import java.util.Map;


public class SearchResultEntry {

    public final String address;

    public final float score;

    /**
     * Field name <--> Text fragments
     */
    public final Map<String,String[]> fragments;

    public SearchResultEntry(String address, float score, Map<String,String[]> fragments){
        this.address = address;
        this.score = score;
        this.fragments = fragments;
    }
}
