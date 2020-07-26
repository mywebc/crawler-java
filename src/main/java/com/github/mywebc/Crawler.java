package com.github.mywebc;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Crawler {

    private CrawlerDao dao = new MyBatisCrawlerDao();

    public void run() throws SQLException, IOException {

        String link;
        // 从数据库中加载下一个链接，如果能加载到，则继续循环
        while ((link = dao.getNextLinkThenDelete()) != null) {

            // c询问数据库当前链接是否被处理过
            if (dao.isLinkProcessed(link)) {
                continue;
            }

            if (isInterestingLink(link)) {

                System.out.println(link);

                Document doc = httpGetAndParseHtml(link);

                parseUrlFromPageAndStoreIntoDatabase(doc);

                storeInToDataBaseIfItIsNewsPage(doc, link);

                dao.insertProcessedLink(link);
//                dao.updateDatabase(link, "INSERT INTO LINKS_ALREADY_PROCESSED (link)values(?)");
            }
        }
    }


    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {
        new Crawler().run();
    }


    private void parseUrlFromPageAndStoreIntoDatabase(Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");

            if (href.startsWith("//")) {
                href = "https:" + href;
            }

            if (!href.toLowerCase().startsWith("javascript")) {
                dao.insertLinkToBeProcessed(href);
//                dao.updateDatabase(href, "INSERT INTO LINKS_TO_BE_PROCESSED (link)values(?)");
            }
        }
    }

    private void storeInToDataBaseIfItIsNewsPage(Document doc, String link) throws SQLException {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).child(0).text();
                String content = articleTag.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                dao.insertNewsIntoDatabase(link, title, content);
            }
        }
    }

    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    private static Document httpGetAndParseHtml(String link) throws IOException {
        // 是否是我们感兴趣的
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet("https://sina.cn");
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4181.9 Safari/537.36");
        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            HttpEntity entity1 = response1.getEntity();
            String html = EntityUtils.toString(entity1);
            return Jsoup.parse(html);
        }
    }

    private static boolean isInterestingLink(String link) {
        return (isNewsPage(link) || isIndexPage(link)) && isNotLogin(link);
    }

    private static boolean isIndexPage(String link) {
        return "https://sina.cn".equals(link);
    }

    private static boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }

    private static boolean isNotLogin(String link) {
        return !link.contains("passport.sina.cn");
    }
}
