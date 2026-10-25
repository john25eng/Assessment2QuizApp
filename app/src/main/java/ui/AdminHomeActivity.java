package ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quizapp.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.Map;

import adapter.TournamentAdapter;
import model.Tournament;

public class AdminHomeActivity extends AppCompatActivity {

    private final List<Tournament> tournaments = new ArrayList<>();
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private TournamentAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration listener;
    private final Map<String, double[]> ratingStats = new HashMap<>();
    private ListenerRegistration ratingListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        db = FirebaseFirestore.getInstance();

        RecyclerView rv = findViewById(R.id.rvTournaments);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TournamentAdapter(tournaments, ratingStats, new TournamentAdapter.Listener() {
            @Override
            public void onEdit(Tournament t) {
                showEditDialog(t);
            }

            @Override
            public void onDelete(Tournament t) {
                confirmDelete(t);
            }
        });
        rv.setAdapter(adapter);

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });

        findViewById(R.id.fabAdd).setOnClickListener(v ->
                startActivity(new Intent(this, CreateTournamentActivity.class)));

        listenForTournaments();
        listenForTournaments();
        listenForRatings();
    }

    private void listenForTournaments() {
        listener = db.collection("tournaments").addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) {
                Toast.makeText(this, "Load failed", Toast.LENGTH_SHORT).show();
                return;
            }
            tournaments.clear();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Tournament t = doc.toObject(Tournament.class);
                if (t != null) {
                    t.setId(doc.getId());
                    tournaments.add(t);
                }
            }
            tournaments.sort((a, b) -> Long.compare(b.getStartDate(), a.getStartDate()));
            adapter.notifyDataSetChanged();
        });
    }

    private void listenForRatings() {
        ratingListener = db.collection("ratings").addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;
            ratingStats.clear();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                String tid = doc.getString("tournamentId");
                Long r = doc.getLong("rating");
                if (tid == null || r == null) continue;
                double[] s = ratingStats.get(tid);
                if (s == null) {
                    s = new double[2];
                    ratingStats.put(tid, s);
                }
                s[0] += r;
                s[1] += 1;
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void showEditDialog(Tournament t) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_tournament, null);
        TextInputEditText etName = view.findViewById(R.id.etEditName);
        Button btnStart = view.findViewById(R.id.btnEditStart);
        Button btnEnd = view.findViewById(R.id.btnEditEnd);

        long[] start = {t.getStartDate()};
        long[] end = {t.getEndDate()};

        etName.setText(t.getName());
        btnStart.setText("Start: " + fmt.format(new Date(start[0])));
        btnEnd.setText("End: " + fmt.format(new Date(end[0])));

        btnStart.setOnClickListener(v -> pickDate(start[0], true, millis -> {
            start[0] = millis;
            btnStart.setText("Start: " + fmt.format(new Date(millis)));
        }));
        btnEnd.setOnClickListener(v -> pickDate(end[0], false, millis -> {
            end[0] = millis;
            btnEnd.setText("End: " + fmt.format(new Date(millis)));
        }));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit tournament")
                .setView(view)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();

        // override so the dialog stays open on validation errors
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText() == null ? "" : etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Enter a name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (end[0] < start[0]) {
                Toast.makeText(this, "End date must be after start date", Toast.LENGTH_SHORT).show();
                return;
            }
            db.collection("tournaments").document(t.getId())
                    .update("name", name, "startDate", start[0], "endDate", end[0])
                    .addOnSuccessListener(r -> {
                        Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
        });
    }

    private void pickDate(long current, boolean isStart, Consumer<Long> onPicked) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(current);
        new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar sel = Calendar.getInstance();
            if (isStart) {
                sel.set(y, m, d, 0, 0, 0);
            } else {
                sel.set(y, m, d, 23, 59, 59);
            }
            sel.set(Calendar.MILLISECOND, 0);
            onPicked.accept(sel.getTimeInMillis());
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void confirmDelete(Tournament t) {
        new AlertDialog.Builder(this)
                .setTitle("Delete tournament")
                .setMessage("Delete \"" + t.getName() + "\"? This can't be undone.")
                .setPositiveButton("Delete", (d, w) ->
                        db.collection("tournaments").document(t.getId()).delete()
                                .addOnSuccessListener(r -> Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
        if (listener != null) listener.remove();
        if (ratingListener != null) ratingListener.remove();
    }
}