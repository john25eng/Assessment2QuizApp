package api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface TriviaApi {
    @GET("api.php")
    Call<OpenTdbResponse> getQuestions(
            @Query("amount") int amount,
            @Query("category") int category,
            @Query("difficulty") String difficulty);
}