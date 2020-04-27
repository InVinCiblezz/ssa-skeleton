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

    public JSONObject searchApi(SdkHttpMethod method,  Optional<Map<String, String>> params, Optional<JSONObject> query){
        String index = Config.getParam("ELASTIC_SEARCH_INDEX");
        String host = Config.getParam("ELASTIC_SEARCH_HOST");
        String path = index + "/_search";

        JSONObject results = new JSONObject();
        JSONArray articles = new JSONArray();
        int returned_results = 0, total_results = 0;

        try {
            HttpExecuteResponse response = restRequest(method, host, path, params, query);
            if (response.httpResponse().isSuccessful()) {
                AbortableInputStream inputStream = response.responseBody().get();
                JSONObject body = new JSONObject(IoUtils.toUtf8String(inputStream));

                body = body.getJSONObject("hits");
                total_results = body.getJSONObject("total").getInt("value");
                JSONArray hits = body.getJSONArray("hits");
                returned_results = hits.length();
                for (int i = 0; i < returned_results; i++)
                    articles.put(hits.getJSONObject(i).getJSONObject("_source"));
            }
            close();
        } catch (IOException e) {
            e.printStackTrace();
        } /*finally {
            close();
        }*/

        results.put("returned_results", returned_results);
        results.put("total_results", total_results);
        results.put(index, articles);

        return results;
    }
}