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

public class TournamentAdapter extends RecyclerView.Adapter<TournamentAdapter.VH> {

    public interface Listener {
        void onEdit(Tournament t);
        void onDelete(Tournament t);
    }

    private final List<Tournament> items;
    private final Map<String, double[]> ratingStats; // tournamentId -> {sum, count}
    private final Listener listener;
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public TournamentAdapter(List<Tournament> items, Map<String, double[]> ratingStats, Listener listener) {
        this.items = items;
        this.ratingStats = ratingStats;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tournament, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Tournament t = items.get(position);
        h.tvName.setText(t.getName());
        h.tvInfo.setText(t.getCategory() + " • " + t.getDifficulty());
        h.tvDates.setText(fmt.format(new Date(t.getStartDate())) + " to " + fmt.format(new Date(t.getEndDate())));
        h.tvStatus.setText(status(t));

        String playerRating;
        double[] s = ratingStats.get(t.getId());
        if (s == null || s[1] == 0) {
            playerRating = "no ratings yet";
        } else {
            playerRating = String.format(Locale.getDefault(), "%.1f/5 (%d ratings)", s[0] / s[1], (int) s[1]);
        }
        h.tvRating.setText("Set rating: " + t.getRating() + "/5\nPlayer rating: " + playerRating);

        h.btnEdit.setOnClickListener(v -> listener.onEdit(t));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(t));
    }

    public static String status(Tournament t) {
        long now = System.currentTimeMillis();
        if (now < t.getStartDate()) return "Upcoming";
        if (now <= t.getEndDate()) return "Ongoing";
        return "Past";
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvInfo, tvDates, tvStatus, tvRating;
        Button btnEdit, btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvInfo = itemView.findViewById(R.id.tvInfo);
            tvDates = itemView.findViewById(R.id.tvDates);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvRating = itemView.findViewById(R.id.tvRating);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}