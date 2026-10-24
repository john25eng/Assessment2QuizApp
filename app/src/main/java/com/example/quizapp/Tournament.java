package com.example.quizapp;

import java.util.List;

public class Tournament {
    private String id;
    private String name;
    private String category;
    private String difficulty;
    private long startDate;
    private long endDate;
    private int rating;
    private List<Question> questions;

    public Tournament() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }
    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }
}