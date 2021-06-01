package com.example.boardtest.models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Post {

    private String doumentId;
    private String title;
    private String contents;
    @ServerTimestamp
    private Date date;

    public Post() {
    }

    public Post(String doumentId, String title, String contents) {
        this.doumentId = doumentId;
        this.title = title;
        this.contents = contents;
    }

    public String getDoumentId() {
        return doumentId;
    }

    public void setDoumentId(String doumentId) {
        this.doumentId = doumentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "Post{" +
                "doumentId='" + doumentId + '\'' +
                ", title='" + title + '\'' +
                ", contents='" + contents + '\'' +
                ", date=" + date +
                '}';
    }
}
