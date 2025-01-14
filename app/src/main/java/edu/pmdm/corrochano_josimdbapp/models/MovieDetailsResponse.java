package edu.pmdm.corrochano_josimdbapp.models;

import com.google.gson.annotations.SerializedName;

public class MovieDetailsResponse {

    // Atributos
    private String overview;
    private String release_date;

    @SerializedName("vote_average")
    private double vote_average;

    // Getters y Setters
    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getRelease_date() {
        return release_date;
    }

    public void setRelease_date(String release_date) {
        this.release_date = release_date;
    }

    public double getVote_average() {
        return vote_average;
    }

    public void setVote_average(double vote_average) {
        this.vote_average = vote_average;
    }

}
