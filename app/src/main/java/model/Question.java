package model;

import java.util.List;

public class Question {
    private String type;
    private String question;
    private String correctAnswer;
    private List<String> incorrectAnswers;

    public Question() {}

    public Question(String type, String question, String correctAnswer, List<String> incorrectAnswers) {
        this.type = type;
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.incorrectAnswers = incorrectAnswers;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public List<String> getIncorrectAnswers() { return incorrectAnswers; }
    public void setIncorrectAnswers(List<String> incorrectAnswers) { this.incorrectAnswers = incorrectAnswers; }
}