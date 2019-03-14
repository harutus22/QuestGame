package com.example.apple.QuestGame.models;

public class Quest {

    private String name;
    private String description;
    private long points;
    private long startTime;
    private long endTime;

    public Quest(){
    }

    public Quest(String name, String description, long points, long startTime, long endTime) {
        this.name = name;
        this.description = description;
        this.points = points;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getPoints() {
        return points;
    }

    public void setPoints(long points) {
        this.points = points;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }


}
