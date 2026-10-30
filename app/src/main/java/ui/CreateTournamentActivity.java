package ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import com.example.quizapp.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import api.ApiQuestion;
import api.OpenTdbResponse;
import api.TriviaClient;
import model.Question;
import model.Tournament;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateTournamentActivity extends AppCompatActivity {

    private final String[] catNames = {"General Knowledge", "Books", "Film", "Music",
            "Science & Nature", "Computers", "Mathematics", "Sports", "Geography", "History"};
    private final int[] catIds = {9, 10, 11, 12, 17, 18, 19, 21, 22, 23};
    private final String[] diffs = {"easy", "medium", "hard"};
    private final String[] ratings = {"1", "2", "3", "4", "5"};

    private TextInputEditText etName;
    private Spinner spCategory, spDifficulty, spRating;
    private Button btnStart, btnEnd, btnCreate;
    private ProgressBar progress;
    private long startMillis = 0, endMillis = 0;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_tournament);

        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.etName);
        spCategory = findViewById(R.id.spCategory);
        spDifficulty = findViewById(R.id.spDifficulty);
        spRating = findViewById(R.id.spRating);
        btnStart = findViewById(R.id.btnStart);
        btnEnd = findViewById(R.id.btnEnd);
        btnCreate = findViewById(R.id.btnCreate);
        progress = findViewById(R.id.progress);

        spCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, catNames));
        spDifficulty.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, diffs));
        spRating.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ratings));

        btnStart.setOnClickListener(v -> pickDate(true));
        btnEnd.setOnClickListener(v -> pickDate(false));
        btnCreate.setOnClickListener(v -> createTournament());
    }

    private void pickDate(boolean isStart) {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar sel = Calendar.getInstance();
            if (isStart) {
                sel.set(y, m, d, 0, 0, 0);
            } else {
                sel.set(y, m, d, 23, 59, 59);
            }
            sel.set(Calendar.MILLISECOND, 0);
            String label = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(sel.getTimeInMillis()));
            if (isStart) {
                startMillis = sel.getTimeInMillis();
                btnStart.setText("Start: " + label);
            } else {
                endMillis = sel.getTimeInMillis();
                btnEnd.setText("End: " + label);
            }
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void createTournament() {
        String name = etName.getText() == null ? "" : etName.getText().toString().trim();
        if (name.isEmpty()) {
            toast("Enter a name");
            return;
        }
        if (startMillis == 0 || endMillis == 0) {
            toast("Pick start and end dates");
            return;
        }
        if (endMillis < startMillis) {
            toast("End date must be after start date");
            return;
        }

        int catIndex = spCategory.getSelectedItemPosition();
        String difficulty = diffs[spDifficulty.getSelectedItemPosition()];
        int rating = Integer.parseInt(ratings[spRating.getSelectedItemPosition()]);

        setLoading(true);
        TriviaClient.getApi().getQuestions(10, catIds[catIndex], difficulty)
                .enqueue(new Callback<OpenTdbResponse>() {
                    @Override
                    public void onResponse(Call<OpenTdbResponse> call, Response<OpenTdbResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            setLoading(false);
                            toast("Question server unavailable (code " + response.code() + "), try again later");
                            return;
                        }
                        OpenTdbResponse body = response.body();
                        if (body.getResponseCode() == 5) {
                            setLoading(false);
                            toast("Rate limited, wait 5 seconds and try again");
                            return;
                        }
                        if (body.getResponseCode() != 0 || body.getResults() == null || body.getResults().size() < 10) {
                            setLoading(false);
                            toast("Not enough questions for that category and difficulty, try another");
                            return;
                        }

                        List<Question> questions = new ArrayList<>();
                        for (ApiQuestion q : body.getResults()) {
                            List<String> wrong = new ArrayList<>();
                            for (String w : q.getIncorrectAnswers()) wrong.add(decode(w));
                            questions.add(new Question(q.getType(), decode(q.getQuestion()),
                                    decode(q.getCorrectAnswer()), wrong));
                        }

                        Tournament t = new Tournament();
                        t.setName(name);
                        t.setCategory(catNames[catIndex]);
                        t.setDifficulty(difficulty);
                        t.setStartDate(startMillis);
                        t.setEndDate(endMillis);
                        t.setRating(rating);
                        t.setQuestions(questions);

                        db.collection("tournaments").add(t)
                                .addOnSuccessListener(r -> {
                                    toast("Tournament created");
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    setLoading(false);
                                    toast("Save failed");
                                });
                    }

                    @Override
                    public void onFailure(Call<OpenTdbResponse> call, Throwable t) {
                        setLoading(false);
                        toast("Network error: " + t.getMessage());
                    }
                });
    }

    private String decode(String s) {
        return HtmlCompat.fromHtml(s, HtmlCompat.FROM_HTML_MODE_LEGACY).toString();
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnCreate.setEnabled(!loading);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}