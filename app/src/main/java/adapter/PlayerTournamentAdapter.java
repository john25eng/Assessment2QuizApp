package adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quizapp.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import model.Tournament;

public class PlayerTournamentAdapter extends RecyclerView.Adapter<PlayerTournamentAdapter.VH> {

    public interface Listener {
        void onPlay(Tournament t);
    }

    private final List<Tournament> items;
    private final Map<String, Long> scores;
    private final Listener listener;
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public PlayerTournamentAdapter(List<Tournament> items, Map<String, Long> scores, Listener listener) {
        this.items = items;
        this.scores = scores;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_player_tournament, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Tournament t = items.get(position);
        h.tvName.setText(t.getName());
        h.tvInfo.setText(t.getCategory() + " • " + t.getDifficulty());
        h.tvDates.setText(fmt.format(new Date(t.getStartDate())) + " to " + fmt.format(new Date(t.getEndDate())));

        boolean done = scores.containsKey(t.getId());
        boolean ongoing = "Ongoing".equals(TournamentAdapter.status(t));

        if (done) {
            h.tvResult.setVisibility(View.VISIBLE);
            h.tvResult.setText("Score: " + scores.get(t.getId()) + "/10");
        } else {
            h.tvResult.setVisibility(View.GONE);
        }

        // only ongoing tournaments that are not yet played can be entered
        h.btnPlay.setVisibility(!done && ongoing ? View.VISIBLE : View.GONE);
        h.btnPlay.setOnClickListener(v -> listener.onPlay(t));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvInfo, tvDates, tvResult;
        Button btnPlay;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvInfo = itemView.findViewById(R.id.tvInfo);
            tvDates = itemView.findViewById(R.id.tvDates);
            tvResult = itemView.findViewById(R.id.tvResult);
            btnPlay = itemView.findViewById(R.id.btnPlay);
        }
    }
}