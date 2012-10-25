package org.aphreet.c3.platform.search.ext.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.aphreet.c3.platform.search.ext.DocumentBuilder;
import org.aphreet.c3.platform.search.ext.FieldWeights;
import org.aphreet.c3.platform.search.ext.SearchConfiguration;

import java.util.Map;

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

    public WeightedDocumentBuilder(SearchConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Document build(Map<String, String> metadata, Map<String, String> extractedMetadata, String language, String domain) {

        FieldWeights weights = configuration.getFieldWeights();

        Document document = new Document();
        Field field;
        String modifiedKey;

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
            if (!key.equalsIgnoreCase("domain")) {
                if (key.equals("c3.address")) {
                    field = new Field(key.toLowerCase(), metadata.get(key), Field.Store.YES, Field.Index.NOT_ANALYZED);
                } else {
                    field = new Field(key.toLowerCase(), metadata.get(key), Field.Store.YES, Field.Index.ANALYZED);
                    if (weights.containsField(key.toLowerCase())) {
                        field.setBoost(weights.getBoostFactor(key.toLowerCase(), 1));
                    }
                }
                document.add(field);
            }
        }

        if(language != null){
            document.add(new Field("lang", language, Field.Store.YES, Field.Index.NOT_ANALYZED));
        }

        document.add(new Field("domain", domain, Field.Store.YES, Field.Index.NOT_ANALYZED));

        return document;
    }
}
