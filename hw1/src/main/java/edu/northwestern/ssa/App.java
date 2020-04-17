package edu.northwestern.ssa;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

import org.archive.io.warc.WARCReaderFactory;
import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveReader;
import org.apache.commons.io.IOUtils;
import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Duration;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
import java.util.Optional;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Future;


public class App {
    public static void main(String[] args) {
        System.out.println("Hello world!");

        //Part1
        String bucketName = "commoncrawl";
        @// TODO: 2020/4/17 auto-crawl
        String object_key =  System.getenv("COMMON_CRAWL_FILENAME") != null
                ? System.getenv("COMMON_CRAWL_FILENAME") : "crawl-data/CC-NEWS/2020/04/CC-NEWS-20200405100433-01526.warc.gz";
        System.out.println(object_key);

        try{
            S3Client s3 = S3Client.builder()
                    .region(Region.US_EAST_1)
                    .overrideConfiguration(ClientOverrideConfiguration.builder()
                            .apiCallTimeout(Duration.ofMinutes(30)).build())
                    .build();


            //ListObjectsV2Request listObjectsV2Request =ListObjectsV2Request.builder().bucket(bucketName).maxKeys(2).build();

            //ListObjectsV2Response listObjectsResponse = s3.listObjectsV2(listObjectsV2Request);

            //listObjectsResponse.contents().forEach(content->{
            //    System.out.println(content.toString());
            //});


            System.out.println("[DOWNLOAD] --- Started ---");
            s3.getObject(GetObjectRequest.builder().bucket(bucketName).
                            key(object_key).build(),
                    ResponseTransformer.toFile(Paths.get("WARC")));
            System.out.println("[DOWNLOAD] --- Finished ---");
        }catch(Exception e){
            System.out.println(e.toString());
        }

        //Part2 & 3 &4
        System.out.println("[INFO] --- Parse Started ---");
        try {
            parseWarc(new File("WARC"));
        } catch (IOException e) {
            System.err.print(e);
        }
        System.out.println("[INFO] --- Parse Finished ---");

@// TODO: 2020/4/17 multiThread
/*
        try{
            //System.out.println(System.getenv("ELASTIC_SEARCH_HOST"));
            ElasticSearch es = new ElasticSearch();
            Map<String, String> map = new HashMap<>();

            //create
            //es.createIndex(SdkHttpMethod.PUT, Optional.ofNullable(map));


            JSONObject body = new JSONObject();
            body.put("title", "888");
            body.put("url", "myUrl");
            body.put("txt", "myTxt");
            //es.postDoc(SdkHttpMethod.POST, Optional.ofNullable(map), Optional.ofNullable(body));


            JSONObject query = new JSONObject(
                    "{\n" +
                    "  \"query\": {\n" +
                    "    \"match\": {\n" +
                    "      \"txt\": {\n" +
                    "        \"query\": \"Evanston\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "}");


            ExecutorService exec = Executors.newSingleThreadExecutor();
            for (int i = 0; i < 10; i++) {
                int index = i;
                Future future = exec.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            es.postDoc(SdkHttpMethod.POST, Optional.ofNullable(map), Optional.ofNullable(body));
                        } catch (Exception e) {
                        }
                        System.out.println(Thread.currentThread() + "----" + index);
                    }
                });
                System.out.println("future.get()=" + future.get());
            }
            exec.shutdown();




            // post doc
            //es.postDoc(SdkHttpMethod.POST, Optional.ofNullable(map), Optional.ofNullable(body));
            //es.postDoc(SdkHttpMethod.PUT, Optional.ofNullable(map), Optional.ofNullable(body));

            // search doc
            es.searchDoc(SdkHttpMethod.POST, Optional.ofNullable(map),Optional.ofNullable(query));
            es.close();

        }catch(Exception ex){
            System.out.println("[ERROR] --- " + ex);
        }
        System.out.println("[INFO] --- Search Finished ---");
    */



    }

    private static void parseWarc(File f) throws IOException {
        ArchiveReader r = WARCReaderFactory.get(f);
        Iterator<ArchiveRecord> it = r.iterator();
        ArchiveRecord record;

        String s;
        String[] strs;
        byte[] bytes;
        ElasticSearch es = new ElasticSearch();
        Map<String, String> map = new HashMap<>();
        es.createIndex(SdkHttpMethod.PUT, Optional.ofNullable(map));
        JSONObject body;
        int count = 0;
        while (it.hasNext()) {
            try {
                record = it.next();
                String url = record.getHeader().getUrl();
                bytes = IOUtils.toByteArray(record);
                s = new String(bytes);
                strs = s.split("\r\n\r\n");
                if (strs.length == 2) s = strs[1];
            //System.out.println("[INFO]---s: " + s);
                Document doc = Jsoup.parse(s);
                String text = doc.text();
                String title = doc.title();
                if (title.length() == 0) continue;
            //System.out.println("[PARSE] --- Parse ---");
            //System.out.println("[PARSE] --- URL: " + record.getHeader().getUrl());
            //System.out.println("[PARSE]--- text: " + text);
            //System.out.println("[PARSE]--- title: " + title);
                body = new JSONObject();
                body.put("title", title);
                body.put("url", url);
                body.put("txt", text);
                es.postDoc(SdkHttpMethod.POST, Optional.ofNullable(map), Optional.ofNullable(body));
                count++;
                System.out.println("Count: " + count);
            } catch (Exception e) {
                //System.err.print(e);
            }
        }
        es.close();
    }

}
