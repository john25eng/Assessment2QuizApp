package ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quizapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import adapter.TournamentAdapter;
import model.Question;
import model.Tournament;

public class QuizActivity extends AppCompatActivity {

    private static final int GREEN = Color.parseColor("#2E7D32");
    private static final int RED = Color.parseColor("#C62828");

    private String tid, uid, pid;
    private List<Question> questions;
    private int index = 0, score = 0;
    private boolean answered = false;

    private FirebaseFirestore db;
    private View quizGroup, resultGroup;
    private TextView tvProgress, tvQuestion, tvFeedback, tvScore;
    private LinearLayout answersContainer;
    private Button btnNext;
    private RatingBar ratingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        db = FirebaseFirestore.getInstance();
        tid = getIntent().getStringExtra("tournamentId");
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        pid = uid + "_" + tid;

        quizGroup = findViewById(R.id.quizGroup);
        resultGroup = findViewById(R.id.resultGroup);
        tvProgress = findViewById(R.id.tvProgress);
        tvQuestion = findViewById(R.id.tvQuestion);
        tvFeedback = findViewById(R.id.tvFeedback);
        tvScore = findViewById(R.id.tvScore);
        answersContainer = findViewById(R.id.answersContainer);
        btnNext = findViewById(R.id.btnNext);
        ratingBar = findViewById(R.id.ratingBar);

        btnNext.setOnClickListener(v -> {
            index++;
            if (index < questions.size()) {
                showQuestion();
            } else {
                finishQuiz();
            }
        });
        findViewById(R.id.btnSubmitRating).setOnClickListener(v -> submitRating());
        findViewById(R.id.btnSkip).setOnClickListener(v -> finish());

        if (tid == null) {
            toast("Missing tournament");
            finish();
            return;
        }
        load();
    }

    private void load() {
        db.collection("participations").document(pid).get().addOnSuccessListener(p -> {
            if (p.exists()) {
                toast("You have already played this tournament");
                finish();
                return;
            }
            db.collection("tournaments").document(tid).get().addOnSuccessListener(doc -> {
                Tournament t = doc.toObject(Tournament.class);
                if (t == null || t.getQuestions() == null || t.getQuestions().isEmpty()) {
                    toast("Tournament not found");
                    finish();
                    return;
                }
                if (!"Ongoing".equals(TournamentAdapter.status(t))) {
                    toast("This tournament is not ongoing");
                    finish();
                    return;
                }
                questions = t.getQuestions();

                // record the attempt now so it can't be replayed
                Map<String, Object> part = new HashMap<>();
                part.put("userId", uid);
                part.put("tournamentId", tid);
                part.put("score", 0);
                db.collection("participations").document(pid).set(part)
                        .addOnSuccessListener(x -> showQuestion())
                        .addOnFailureListener(e -> {
                            toast("Could not start quiz");
                            finish();
                        });
            }).addOnFailureListener(e -> {
                toast("Load failed");
                finish();
            });
        }).addOnFailureListener(e -> {
            toast("Load failed");
            finish();
        });
    }

    private void showQuestion() {
        answered = false;
        Question q = questions.get(index);

        tvProgress.setText("Question " + (index + 1) + " of " + questions.size());
        tvQuestion.setText(q.getQuestion());
        tvFeedback.setVisibility(View.GONE);
        btnNext.setVisibility(View.GONE);
        answersContainer.removeAllViews();

        List<String> options = new ArrayList<>(q.getIncorrectAnswers());
        options.add(q.getCorrectAnswer());
        if ("boolean".equals(q.getType())) {
            options.sort((a, b) -> b.compareTo(a)); // true first, then false
        } else {
            Collections.shuffle(options);
        }

        for (String opt : options) {
            MaterialButton b = new MaterialButton(this);
            b.setText(opt);
            b.setAllCaps(false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(10);
            b.setOnClickListener(v -> onAnswer(b, opt, q));
            answersContainer.addView(b, lp);
        }
    }

    private void onAnswer(MaterialButton clicked, String chosen, Question q) {
        if (answered) return;
        answered = true;

        boolean correct = chosen.equals(q.getCorrectAnswer());
        if (correct) {
            score++;
            clicked.setBackgroundTintList(ColorStateList.valueOf(GREEN));
            tvFeedback.setText("Correct!");
            tvFeedback.setTextColor(GREEN);
        } else {
            clicked.setBackgroundTintList(ColorStateList.valueOf(RED));
            for (int i = 0; i < answersContainer.getChildCount(); i++) {
                MaterialButton b = (MaterialButton) answersContainer.getChildAt(i);
                if (b.getText().toString().equals(q.getCorrectAnswer())) {
                    b.setBackgroundTintList(ColorStateList.valueOf(GREEN));
                }
            }
            tvFeedback.setText("Incorrect. The correct answer is: " + q.getCorrectAnswer());
            tvFeedback.setTextColor(RED);
        }
        tvFeedback.setVisibility(View.VISIBLE);

        btnNext.setText(index == questions.size() - 1 ? "Finish" : "Next");
        btnNext.setVisibility(View.VISIBLE);
    }

    private void finishQuiz() {
        quizGroup.setVisibility(View.GONE);
        resultGroup.setVisibility(View.VISIBLE);
        tvScore.setText("Your score: " + score + " / " + questions.size());

        db.collection("participations").document(pid).update("score", score)
                .addOnFailureListener(e -> toast("Could not save score"));
    }

    private void submitRating() {
        int rating = (int) ratingBar.getRating();
        if (rating < 1) {
            toast("Pick a rating from 1 to 5");
            return;
        }
        Map<String, Object> r = new HashMap<>();
        r.put("userId", uid);
        r.put("tournamentId", tid);
        r.put("rating", rating);
        db.collection("ratings").document(pid).set(r)
                .addOnSuccessListener(x -> {
                    toast("Thanks for rating!");
                    finish();
                })
                .addOnFailureListener(e -> toast("Could not save rating"));
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}