package com.github.mywebc;

import java.sql.SQLException;

public interface CrawlerDao {

    String getNextLink(String s) throws SQLException;

    String getNextLinkThenDelete() throws SQLException;

    void updateDatabase(String link, String s) throws SQLException;

    void insertNewsIntoDatabase(String url, String title, String content) throws SQLException;

    boolean isLinkProcessed(String link) throws SQLException;

}
