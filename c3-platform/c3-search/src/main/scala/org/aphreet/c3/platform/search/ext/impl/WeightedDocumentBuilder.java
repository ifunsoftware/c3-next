package org.aphreet.c3.platform.search.ext.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.aphreet.c3.platform.search.ext.DocumentBuilder;
import org.aphreet.c3.platform.search.ext.FieldWeights;
import org.aphreet.c3.platform.search.ext.SearchConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Ildar
 * Date: 02.02.2011
 * Time: 22:16:28
 */
public class WeightedDocumentBuilder  implements DocumentBuilder {

    private SearchConfiguration configuration;

    public static final String EXTRACTED_SUFFIX = "_ex";

    private static final Log log = LogFactory.getLog(WeightedDocumentBuilder.class);

    private Set<String> blacklistedMeta = new HashSet<String>();

    {
        blacklistedMeta.add("domain");
        blacklistedMeta.add("c3.address");
        blacklistedMeta.add("lang");
    }

    public WeightedDocumentBuilder(SearchConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Document build(Map<String, String> metadata, Map<String, String> extractedMetadata,
                          String content,
                          String language, String address, String domain) {

        FieldWeights weights = configuration.getFieldWeights();

        Document document = new Document();
        Field field;
        String modifiedKey;

        if(content != null){
            document.add(new Field("content", content, Field.Store.YES, Field.Index.ANALYZED));
        }

        // store extracted Metadata
        for (String key : extractedMetadata.keySet()) {
            if (!key.equalsIgnoreCase("domain")) {
                modifiedKey = key.toLowerCase(); // modify field name if such name already present in metadata
                if (metadata.containsKey(key)) {
                    modifiedKey += EXTRACTED_SUFFIX;
                }
                field = new Field(modifiedKey, extractedMetadata.get(key), Field.Store.YES, Field.Index.ANALYZED);
                if (weights.containsField(key.toLowerCase())) {
                    field.setBoost(weights.getBoostFactor(key.toLowerCase(), 1));
                }
                document.add(field);
            }
        }

        // store user metadata
        for (String key : metadata.keySet()) {
            if (!blacklistedMeta.contains(key)) {

                Field.Index isAnalyzed = Field.Index.ANALYZED;
                if(key.startsWith("__")){
                    isAnalyzed = Field.Index.NOT_ANALYZED;
                }

                String fieldKey = getFieldKey(key);
                Collection<String> values = getFieldValues(key, metadata.get(key));

                for(String value : values){

                    field = new Field(fieldKey.toLowerCase(), value, Field.Store.YES, isAnalyzed);
                    if (weights.containsField(fieldKey.toLowerCase())) {
                        field.setBoost(weights.getBoostFactor(fieldKey.toLowerCase(), 1));
                    }

                    document.add(field);
                }
            }
        }

        if(language != null){
            document.add(new Field("lang", language, Field.Store.YES, Field.Index.NOT_ANALYZED));
        }

        document.add(new Field("c3.address", address, Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field("domain", domain, Field.Store.YES, Field.Index.NOT_ANALYZED));

        return document;
    }

    private String getFieldKey(String key){
        return key.replaceAll("_list$", "");
    }

    private Collection<String> getFieldValues(String key, String value){

        if(key.endsWith("_list")){
            return splitList(value);
        }else{
            return Collections.singleton(value);
        }

    }

    public static Collection<String> splitList(String values){

        List<String> list = new ArrayList<>();

        int valueStart = 0;

        for(int i=0; i<values.length(); i++){
            if(values.charAt(i) == ','){
                if(i == 0 || (i > 0 && values.charAt(i - 1) != '\\')){
                    list.add(values.substring(valueStart, i).replaceAll("\\\\,", ","));
                    valueStart = i+1;
                }
            }
        }

        list.add(values.substring(valueStart, values.length()));

        return list;
    }
}
