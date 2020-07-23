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
import java.util.List;

public class Main {
    private static final String USER_NAME = "root";
    private static final String PASSWORD = "root";

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:file:/Users/chenxiaole/learnTrain/java-crawler/news", USER_NAME, PASSWORD);

        while (true) {
            // 待处理的链接池
            List<String> linkPool = loadUrlFromDatabase(connection, "select link from LINKS_TO_BE_PROCESSED");
            // 从数据库加载即将处理链接的代码
            if (linkPool.isEmpty()) {
                break;
            }

            String link = linkPool.remove(linkPool.size() - 1);
            insertLinkIntoDatabase(connection, link, "delete from LINKS_TO_BE_PROCESSED where link = ?");

            // c询问数据库当前链接是否被处理过
            if (isLinkProcessed(connection, link)) {
                continue;
            }

            if (isInterestingLink(link)) {
                Document doc = httpGetAndParseHtml(link);

                parseUrlFromPageAndStoreIntoDatabase(connection, doc);

                storeInToDataBaseIfItIsNewsPage(doc);

                insertLinkIntoDatabase(connection, link, "INSERT INTO LINKS_ALREADY_PROCESSED (link)values(?)");

            }
        }
    }

    private static void parseUrlFromPageAndStoreIntoDatabase(Connection connection, Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            insertLinkIntoDatabase(connection, href, "INSERT INTO LINKS_TO_BE_PROCESSED (link)values(?)");
        }
    }

    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement("SELECT LINK FROM LINKS_ALREADY_PROCESSED where link =  ?")) {
            statement.setString(1, link);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return true;
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return false;
    }

    private static void insertLinkIntoDatabase(Connection connection, String link, String s) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(s)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    private static List<String> loadUrlFromDatabase(Connection connection, String s) throws SQLException {
        List<String> results = new ArrayList<>();
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement(s)) {
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                results.add(resultSet.getString(1));
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return results;
    }

    private static void storeInToDataBaseIfItIsNewsPage(Document doc) {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).child(0).text();
                System.out.println(title);
            }
        }
    }

    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    private static Document httpGetAndParseHtml(String link) throws IOException {
        // 是否是我们感兴趣的
        CloseableHttpClient httpclient = HttpClients.createDefault();
        if (link.startsWith("//")) {
            link = "https:" + link;
        }
        System.out.println(link);
        HttpGet httpGet = new HttpGet("https://sina.cn");
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4181.9 Safari/537.36");
        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            System.out.println(response1.getStatusLine());
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
