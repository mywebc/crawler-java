package com.github.mywebc;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {

    public static void main(String[] args) throws IOException {
        // 待处理的链接池
        List<String> linkPool = new ArrayList<>();
        // 已经处理的链接池子
        Set<String> processLinks = new HashSet<>();

        linkPool.add("https://sina.cn");

        while (true) {
            if (linkPool.isEmpty()) {
                break;
            }

            // ArrayList 从尾部删除更有效率
            String link = linkPool.remove(linkPool.size() - 1);

            if (processLinks.contains(link)) {
                continue;
            }

            if (link.contains("news.sina.cn") || "https://sina.cn".equals(link)) {
                // 是否是我们感兴趣的
                CloseableHttpClient httpclient = HttpClients.createDefault();

                if (link.startsWith("//")) {
                    link = "https" + link;
                }

                HttpGet httpGet = new HttpGet("https://sina.cn");
                httpGet.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4181.9 Safari/537.36");

                System.out.println(link);

                try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
                    System.out.println(response1.getStatusLine());
                    HttpEntity entity1 = response1.getEntity();
                    String html = EntityUtils.toString(entity1);

                    Document doc = Jsoup.parse(html);
                    // 拿到所有的a标签
                    ArrayList<Element> links = doc.select("a");

                    // 拿到a标签的href
                    for (Element aTag : links) {
                        linkPool.add(aTag.attr("href"));
                    }

                    // 如果是新闻的详情页，就存入数据库
                    ArrayList<Element> articleTags = doc.select("article");
                    if (!articleTags.isEmpty()) {
                        for (Element articleTag : articleTags) {
                            String title = articleTags.get(0).child(0).text();
                            System.out.println(title);
                        }
                    }
                    processLinks.add(link);
                }
            } else {
                continue;
            }
        }
    }
}
