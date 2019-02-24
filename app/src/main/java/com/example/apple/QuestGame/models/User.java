package com.example.apple.QuestGame.models;

public class User {
    private String avatar;
    private String full_name;
    private String user_name;
    private String user_id;

    public User(){
    }

    public User(String avatar, String full_name, String user_name, String user_id) {
        this.full_name = full_name;
        this.user_name = user_name;
        this.user_id = user_id;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getFull_name() {
        return full_name;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }
}
