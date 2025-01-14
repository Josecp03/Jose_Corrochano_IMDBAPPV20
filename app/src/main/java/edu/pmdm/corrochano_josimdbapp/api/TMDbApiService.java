package edu.pmdm.corrochano_josimdbapp.api;

import com.google.gson.JsonObject;

import edu.pmdm.corrochano_josimdbapp.models.MovieSearchResponse;
import edu.pmdm.corrochano_josimdbapp.models.GeneroResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TMDbApiService {

    @GET("genre/movie/list")
    Call<GeneroResponse> getGenres(@Query("language") String language);

    @GET("discover/movie")
    Call<MovieSearchResponse> searchMovies(
            @Query("primary_release_year") int primary_release_year,
            @Query("with_genres") int with_genres,
            @Query("language") String language,
            @Query("sort_by") String sort_by,
            @Query("page") int page
    );

    @GET("movie/{movie_id}/external_ids")
    Call<JsonObject> getExternalIds(@Path("movie_id") String movieId, @Header("Authorization") String authHeader);


}