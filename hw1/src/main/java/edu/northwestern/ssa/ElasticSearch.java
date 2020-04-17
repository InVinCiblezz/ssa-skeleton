package edu.northwestern.ssa;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class ElasticSearch extends AwsSignedRestRequest {
    /**
     * @param serviceName would be "es" for Elasticsearch
     */
    private String indexPath = System.getenv("ELASTIC_SEARCH_INDEX");
    ElasticSearch() {super("es");}

    public void createIndex(SdkHttpMethod method,  Optional<Map<String, String>> params) {
        String host = System.getenv("ELASTIC_SEARCH_HOST");
        try {
            //printResponse(restRequest(method, host, indexPath, params));
            restRequest(method, host, System.getenv("ELASTIC_SEARCH_INDEX"), params);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //@// TODO: 2020/4/17 bulk
    public void bulkDoc(SdkHttpMethod method,  Optional<Map<String, String>> params, Optional<JSONObject> body) {
        String path = System.getenv("ELASTIC_SEARCH_INDEX") + "/_bulk";
        String host = System.getenv("ELASTIC_SEARCH_HOST");
        try {
            //printResponse(restRequest(method, host, path, params, body));
            HttpExecuteResponse response = restRequest(method, host, path, params, body);
            printResponse(response);
            response.responseBody().get().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void postDoc(SdkHttpMethod method,  Optional<Map<String, String>> params, Optional<JSONObject> body) {
        String path = System.getenv("ELASTIC_SEARCH_INDEX") + "/_doc";
        String host = System.getenv("ELASTIC_SEARCH_HOST");
        //System.out.println(body);
        try {
            //printResponse(restRequest(method, host, path, params, body));
            HttpExecuteResponse response = restRequest(method, host, path, params, body);
            while (!response.httpResponse().isSuccessful()) {
                response.responseBody().get().close();
                response = restRequest(method, host, path, params, body);
            }
            //String result = printResponse(response);
            //JSONObject b = new JSONObject(result);
            //b = (JSONObject)b.get("successful");
            //System.out.println("b : " + b.toString(4));
            response.responseBody().get().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public  void searchDoc(SdkHttpMethod method,  Optional<Map<String, String>> params, Optional<JSONObject> query){
        String path = indexPath + "/_search";
        String host = System.getenv("ELASTIC_SEARCH_HOST");
        System.out.println(host);
        try {
            String result = printResponse(restRequest(method, host, path, params, query));
            JSONObject body = new JSONObject(result);
            body = (JSONObject)body.get("hits");
            System.out.println(body.toString(4));
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static String printResponse(HttpExecuteResponse executeResponse) throws IOException {
        AbortableInputStream inputStream = executeResponse.responseBody().get();
        StringWriter writer = new StringWriter();
        String result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        JSONObject body = new JSONObject(result);
        System.out.println(result);
        return result;
    }
}