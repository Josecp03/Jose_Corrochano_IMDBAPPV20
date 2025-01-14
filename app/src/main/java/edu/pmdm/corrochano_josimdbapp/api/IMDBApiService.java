package edu.pmdm.corrochano_josimdbapp.api;

import edu.pmdm.corrochano_josimdbapp.models.MovieOverviewResponse;
import edu.pmdm.corrochano_josimdbapp.models.PopularMoviesResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface IMDBApiService {

    @GET("title/get-top-meter")
    Call<PopularMoviesResponse> obtenerTop10(@Query("Country") String country);

    @GET("title/get-overview")
    Call<MovieOverviewResponse> obtenerDatos(@Query("tconst") String movieId);

}
