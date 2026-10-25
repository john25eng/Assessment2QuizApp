package ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quizapp.R;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import adapter.PlayerTournamentAdapter;
import adapter.TournamentAdapter;
import model.Tournament;

public class PlayerHomeActivity extends AppCompatActivity {

    private final String[] tabNames = {"Ongoing", "Upcoming", "Past", "Participated"};
    private final String[] emptyMsgs = {
            "No ongoing tournaments", "No upcoming tournaments",
            "No past tournaments", "You haven't played any tournaments yet"};

    private final List<Tournament> all = new ArrayList<>();
    private final List<Tournament> shown = new ArrayList<>();
    private final Map<String, Long> scores = new HashMap<>();

    private PlayerTournamentAdapter adapter;
    private TabLayout tabs;
    private TextView tvEmpty;
    private FirebaseFirestore db;
    private ListenerRegistration tourListener, partListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_home);

        db = FirebaseFirestore.getInstance();
        tabs = findViewById(R.id.tabs);
        tvEmpty = findViewById(R.id.tvEmpty);

        for (String n : tabNames) tabs.addTab(tabs.newTab().setText(n));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { refresh(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        RecyclerView rv = findViewById(R.id.rvTournaments);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PlayerTournamentAdapter(shown, scores, t -> {
            Intent i = new Intent(this, QuizActivity.class);
            i.putExtra("tournamentId", t.getId());
            startActivity(i);
        });
        rv.setAdapter(adapter);

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });

        listen();
    }

    private void listen() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        tourListener = db.collection("tournaments").addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) {
                Toast.makeText(this, "Load failed", Toast.LENGTH_SHORT).show();
                return;
            }
            all.clear();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Tournament t = doc.toObject(Tournament.class);
                if (t != null) {
                    t.setId(doc.getId());
                    all.add(t);
                }
            }
            all.sort((a, b) -> Long.compare(b.getStartDate(), a.getStartDate()));
            refresh();
        });

        partListener = db.collection("participations").whereEqualTo("userId", uid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    scores.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String tid = doc.getString("tournamentId");
                        Long score = doc.getLong("score");
                        if (tid != null && score != null) scores.put(tid, score);
                    }
                    refresh();
                });
    }

    private void refresh() {
        int tab = tabs.getSelectedTabPosition();
        if (tab < 0) tab = 0;

        shown.clear();
        for (Tournament t : all) {
            boolean done = scores.containsKey(t.getId());
            String status = TournamentAdapter.status(t);
            boolean match;
            switch (tab) {
                case 0: match = !done && status.equals("Ongoing"); break;
                case 1: match = !done && status.equals("Upcoming"); break;
                case 2: match = !done && status.equals("Past"); break;
                default: match = done; break;
            }
            if (match) shown.add(t);
        }
        adapter.notifyDataSetChanged();

        tvEmpty.setText(emptyMsgs[tab]);
        tvEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tourListener != null) tourListener.remove();
        if (partListener != null) partListener.remove();
    }
}