package api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OpenTdbResponse {
    @SerializedName("response_code")
    private int responseCode;

    private List<ApiQuestion> results;

    public int getResponseCode() { return responseCode; }
    public List<ApiQuestion> getResults() { return results; }
}