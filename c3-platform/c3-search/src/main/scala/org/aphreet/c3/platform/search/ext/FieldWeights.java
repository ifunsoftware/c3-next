package org.aphreet.c3.platform.search.ext;

import java.util.Collections;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Ildar
 * Date: 02.02.2011
 * Time: 0:37:34
 */
public class FieldWeights {

    private Map<String, Integer> weights;

    public FieldWeights(Map<String, Integer> weights) {
        this.weights = Collections.unmodifiableMap(weights);
    }

    public boolean containsField(String key) {
        return weights.containsKey(key);
    }

    public int getBoostFactor(String key, int defaultValue) {

        Integer value = weights.get(key);

        if(value != null){
            return value;
        }else{
            return defaultValue;
        }
    }

    public String[] getFields() {
        return weights.keySet().toArray(new String[weights.size()]);
    }

}
