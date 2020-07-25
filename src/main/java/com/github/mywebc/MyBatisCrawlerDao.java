package com.github.mywebc;

import java.sql.SQLException;

public class MyBatisCrawlerDao implements CrawlerDao {
    @Override
    public String getNextLink(String s) throws SQLException {
        return null;
    }

    @Override
    public String getNextLinkThenDelete() throws SQLException {
        return null;
    }

    @Override
    public void updateDatabase(String link, String s) throws SQLException {

    }

    @Override
    public void insertNewsIntoDatabase(String url, String title, String content) throws SQLException {

    }

    @Override
    public boolean isLinkProcessed(String link) throws SQLException {
        return false;
    }
}
