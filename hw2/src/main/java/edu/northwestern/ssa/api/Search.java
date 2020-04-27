package edu.northwestern.ssa.api;

import edu.northwestern.ssa.ElasticSearch;
import software.amazon.awssdk.http.SdkHttpMethod;

import java.util.Optional;
import java.util.Locale;
import java.util.HashMap;
import java.util.Arrays;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/search")
public class Search {

    final static String dateFormat = "yyyy-mm-dd";
    final static String[] ISO = Locale.getISOLanguages();

    /** when testing, this is reachable at http://localhost:8080/api/search?query=hello */
    @GET
    public Response getMsg(@QueryParam("query") String q,
                           @DefaultValue("") @QueryParam("language") String language,
                           @DefaultValue("") @QueryParam("date") String date,
                           @DefaultValue("10") @QueryParam("count") int count,
                           @DefaultValue("0") @QueryParam("offset") int offset)
            throws IOException {

        //query is null, status 400
        if (q == null)
            return Response.status(400).type("text/plain").entity("'query' is missing from url.")
                    .header("Access-Control-Allow-Origin", "*").build();

        //query params construcor
        HashMap<String, String> params = new HashMap<>();
        params.put("q", parseQuery(q, language, date));
        params.put("from", Integer.toString(offset > 0 ? offset : 0));
        params.put("size", Integer.toString(count > 0 ? count : 10));
        params.put("track_total_hits", "true");

        //elasticSearch
        ElasticSearch es = new ElasticSearch();
        JSONObject results = es.searchApi(SdkHttpMethod.GET, Optional.of(params), Optional.empty());

        return Response.status(200).type("application/json").entity(results.toString(4))
                // below header is for CORS
                .header("Access-Control-Allow-Origin", "*").build();
    }

    private static boolean isDateValid(String dateStr) {
        DateFormat sdf = new SimpleDateFormat(dateFormat);
        sdf.setLenient(false);
        try {
            sdf.parse(dateStr);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    private boolean isLanguageValid(String languageStr) {
       return Arrays.asList(ISO).contains(languageStr);
    }

    private String parseQuery(String q, String language, String date) {
        StringBuilder sb = new StringBuilder();
        //querry
        sb.append("txt:(" + q.replace(" ", " AND ") + ")");
        //language
        if (language != null && isLanguageValid(language)) {
            sb.append(" AND ");
            sb.append("lang:" + language);
        }
        //date
        if (date != null && isDateValid(date)) {
            sb.append(" AND ");
            sb.append("date:" + date);
        }
        //System.out.println("string: " + sb.toString());
        return sb.toString();
    }
}
