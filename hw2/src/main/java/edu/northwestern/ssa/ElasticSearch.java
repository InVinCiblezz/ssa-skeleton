package edu.northwestern.ssa;

import org.json.JSONArray;
import org.json.JSONObject;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.utils.IoUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class ElasticSearch extends AwsSignedRestRequest {
    /**
     * @param serviceName would be "es" for Elasticsearch
     */
    public ElasticSearch() {super("es");}

    public JSONObject searchDoc(SdkHttpMethod method,  Optional<Map<String, String>> params, Optional<JSONObject> query){
        String path = Config.getParam("ELASTIC_SEARCH_INDEX") + "/_search";
        String host = Config.getParam("ELASTIC_SEARCH_HOST");

        JSONObject results = new JSONObject();
        try {
            HttpExecuteResponse response = restRequest(method, host, path, params, query);
            if (response.httpResponse().isSuccessful()) {
                //System.out.println("ok");
                AbortableInputStream inputStream = response.responseBody().get();
                JSONObject body = new JSONObject(IoUtils.toUtf8String(inputStream));
                JSONArray articles = new JSONArray();

                body = body.getJSONObject("hits");
                JSONArray hits = body.getJSONArray("hits");
                int returned_results = hits.length();
                int total_results = body.getJSONObject("total").getInt("value");
                for (int i = 0; i < returned_results; i ++)
                    articles.put(hits.getJSONObject(i).getJSONObject("_source"));

                results.put("returned_results", returned_results);
                results.put("total_results", total_results);
                results.put("articles", articles);
            }
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return results;
    }

}