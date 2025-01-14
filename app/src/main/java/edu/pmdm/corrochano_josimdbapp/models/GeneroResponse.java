package edu.pmdm.corrochano_josimdbapp.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GeneroResponse {

    // Atributos
    @SerializedName("genres")
    private List<Genero> genres;

    // Getters y Setters
    public List<Genero> getGenres() {
        return genres;
    }

    public void setGenres(List<Genero> genres) {
        this.genres = genres;
    }

}
