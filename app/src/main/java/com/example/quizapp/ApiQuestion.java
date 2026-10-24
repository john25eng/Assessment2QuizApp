package com.example.quizapp;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ApiQuestion {
    private String type;
    private String question;

    @SerializedName("correct_answer")
    private String correctAnswer;

    @SerializedName("incorrect_answers")
    private List<String> incorrectAnswers;

    public String getType() { return type; }
    public String getQuestion() { return question; }
    public String getCorrectAnswer() { return correctAnswer; }
    public List<String> getIncorrectAnswers() { return incorrectAnswers; }
}