package edu.northwestern.ssa;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

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
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Optional;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Future;

public class App {
    private static String bucketName = "commoncrawl";
    private static String objectKey;

    public static void main(String[] args) {
        System.out.println("Hello world!");
        try{
            //Part1
            S3Client s3 = S3Client.builder()
                    .region(Region.US_EAST_1)
                    .overrideConfiguration(ClientOverrideConfiguration.builder()
                            .apiCallTimeout(Duration.ofMinutes(30)).build())
                    .build();
            objectKey = getNewestCraw(s3);
            System.out.println("Key: " + objectKey);
            System.out.println("[INFO] --- DOWNLOAD Started ---");
            s3.getObject(GetObjectRequest.builder().bucket(bucketName).
                            key(objectKey).build(),
                    ResponseTransformer.toFile(Paths.get("WARC")));
            System.out.println("[INFO] --- DOWNLOAD Finished ---");

            //Part2 & 3 &4
            System.out.println("[INFO] --- Parse Started ---");
            parseWarc(new File("WARC"));
            System.out.println("[INFO] --- Parse Finished ---");

        }catch(Exception e){
            System.out.println(e.toString());
        }
    }

/*
//@// TODO: 2020/4/17 multiThread

        try {
            //System.out.println(System.getenv("ELASTIC_SEARCH_HOST"));
            ElasticSearch es = new ElasticSearch();
            Map<String, String> map = new HashMap<>();

            //create
            //es.createIndex(SdkHttpMethod.PUT, Optional.ofNullable(map));


            JSONObject body = new JSONObject();
            body.put("title", "zzzc");
            body.put("url", "myUrl");
            body.put("txt", "myTxt");
            //es.postDoc(SdkHttpMethod.POST, Optional.ofNullable(map), Optional.ofNullable(body));


            JSONObject query = new JSONObject(
                    "{\n" +
                            "  \"query\": {\n" +
                            "    \"match\": {\n" +
                            "      \"title\": {\n" +
                            "        \"query\": \"zzzc\"\n" +
                            "      }\n" +
                            "    }\n" +
                            "  }\n" +
                            "}");

            //for (int c = 0; c < 10000; c ++) {
            //ExecutorService exec = Executors.newSingleThreadExecutor();
            ExecutorService exec = Executors.newFixedThreadPool(10);
            for (int i = 0; i < 10; i++) {
                int index = i;
                exec.execute(new TestRunnable());
                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            es.postDoc(SdkHttpMethod.POST, Optional.ofNullable(map), Optional.ofNullable(body));
                        } catch (Exception e) {
                        }
                        System.out.println("----" + index);
                    }
                });

            }
            exec.shutdown();
            //}


            // post doc
            //es.postDoc(SdkHttpMethod.POST, Optional.ofNullable(map), Optional.ofNullable(body));
            //es.postDoc(SdkHttpMethod.PUT, Optional.ofNullable(map), Optional.ofNullable(body));

            // search doc
            es.searchDoc(SdkHttpMethod.POST, Optional.ofNullable(map), Optional.ofNullable(query));
            es.close();

        } catch (Exception ex) {
            System.out.println("[ERROR] --- " + ex);
        }
        System.out.println("[INFO] --- Search Finished ---");


    }

 */

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

    public static String getNewestCraw(S3Client s3Client) {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println("current time：" + sdf.format(date));
        if (System.getenv("COMMON_CRAWL_FILENAME") != null) return System.getenv("COMMON_CRAWL_FILENAME");
        SimpleDateFormat year = new SimpleDateFormat("yyyy");
        SimpleDateFormat month = new SimpleDateFormat("MM");
        SimpleDateFormat day = new SimpleDateFormat("yyyyMMdd");
        String prefix = "crawl-data/CC-NEWS/" + year.format(date) + '/' + month.format(date) + "/CC-NEWS-" + day.format(date);

        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder().bucket(bucketName).maxKeys(10)
                .prefix(prefix).build();
        ListObjectsV2Response listObjectsResponse = s3Client.listObjectsV2(listObjectsV2Request);

        String newestKey = "";
        for (S3Object content : listObjectsResponse.contents())
            newestKey = content.key();
        return newestKey;
    }
/*
    class TestRunnable implements Runnable {
        public void run() {
            //es.postDoc(SdkHttpMethod.POST, Optional.ofNullable(map), Optional.ofNullable(body));
            System.out.println(Thread.currentThread().getName() + "线程被调用了。");
        }

    }

 */
}
