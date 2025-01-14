package edu.pmdm.corrochano_josimdbapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import edu.pmdm.corrochano_josimdbapp.adapters.MovieAdapter;
import edu.pmdm.corrochano_josimdbapp.api.TMDbApiService;
import edu.pmdm.corrochano_josimdbapp.database.FavoriteDatabaseHelper;
import edu.pmdm.corrochano_josimdbapp.models.Movie;
import edu.pmdm.corrochano_josimdbapp.models.MovieSearchResponse;
import edu.pmdm.corrochano_josimdbapp.models.TMDBMovie;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MovieListActivity extends AppCompatActivity {

    // Atributos
    private RecyclerView recyclerView;
    private List<Movie> movieList = new ArrayList<>();
    private MovieAdapter adapter;
    private FavoriteDatabaseHelper databaseHelper;
    private static final String TMDB_API_KEY = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJmMDE2ZGRjOGRhYWZmYzUyYmM1MmUxN2I1MTQ2ZTk3MSIsIm5iZiI6MTczNjUzOTU1MC43NjksInN1YiI6IjY3ODE3ZDllYzVkMmU5NmUyNjdiNGMwZiIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.cP-LiqfqCtg1E7xRX6nPOT3cdttykNkk95N3dvGxkbA"; // Reemplaza con tu clave API segura

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_movie_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Asignar el valor del XML del recyclerView
        recyclerView = findViewById(R.id.recyclerView);

        // Inicializar la base de datos
        databaseHelper = new FavoriteDatabaseHelper(this);

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2)); // 2 columnas

        // Obtener el UID del usuario actual
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String idUsuario = currentUser.getUid();

        // Crear el adaptador para mostrar la lista de películas
        adapter = new MovieAdapter(this, movieList, idUsuario, databaseHelper, false);
        recyclerView.setAdapter(adapter);

        // Obtener los extras del Intent
        Intent intent = getIntent();
        String yearStr = intent.getStringExtra("year");
        int genreId = intent.getIntExtra("genreId", -1);
        int year = Integer.parseInt(yearStr);

        // Llamar al método para buscar las películas según los atributos recibidos
        buscarPeliculas(year, genreId);

    }

    private void buscarPeliculas(int year, int genreId) {

        // Configuración de la API
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    okhttp3.Request request = chain.request().newBuilder()
                            .addHeader("Authorization", "Bearer " + TMDB_API_KEY)
                            .addHeader("accept", "application/json")
                            .build();
                    return chain.proceed(request);
                })
                .build();

        // Inicialización de Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.themoviedb.org/3/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Declarar e inicializar la interfaz IMDBApiService para realizar las solicitudes a la API
        TMDbApiService tmdbApiService = retrofit.create(TMDbApiService.class);

        // Realizar una llamada a la API para buscar películas según año, género, idioma, orden y página.
        Call<MovieSearchResponse> call = tmdbApiService.searchMovies(year, genreId, "es-ES", "popularity.desc", 1);

        // Maneja la respuesta de la llamada de forma asíncrona utilizando enqueue
        call.enqueue(new Callback<MovieSearchResponse>() {
            @Override
            public void onResponse(Call<MovieSearchResponse> call, Response<MovieSearchResponse> response) {

                // Verifica si la respuesta fue exitosa y contiene un cuerpo válido
                if (response.isSuccessful() && response.body() != null) {

                    // Obtiene la lista de resultados de películas desde la respuesta
                    List<TMDBMovie> results = response.body().getResults();

                    // Comprobar si hay resultados y no está vacío
                    if (results != null && !results.isEmpty()) {

                        // Limpia la lista actual de películas antes de agregar nuevas
                        movieList.clear();

                        // Recorre las películas obtenidas
                        for (TMDBMovie tmdbMovie : results) {

                            // Crea una clase Movie y asigna los parámetros
                            Movie movie = new Movie();
                            movie.setId(String.valueOf(tmdbMovie.getId()));
                            movie.setTitle(tmdbMovie.getTitle());
                            movie.setOriginalTitle(tmdbMovie.getOriginal_title());
                            movie.setReleaseDate(tmdbMovie.getRelease_date());
                            movie.setDescripcion(tmdbMovie.getOverview());
                            movie.setRating(String.valueOf(tmdbMovie.getVote_average()));
                            movie.setPosterPath("https://image.tmdb.org/t/p/w500" + tmdbMovie.getPoster_path());

                            // Llamada al método para obtener el ID de IMDB de la película
                            obtenerImdbId(String.valueOf(tmdbMovie.getId()), movie);

                            // Agregar la película a la lista
                            movieList.add(movie);

                        }

                        // Notificar al adaptador que los datos han cambiado para actualizar la vista
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(MovieListActivity.this, "No se encontraron películas para los parámetros especificados.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MovieListActivity.this, "Error al cargar películas: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MovieSearchResponse> call, Throwable t) {
                Toast.makeText(MovieListActivity.this, "Error en la llamada API: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void obtenerImdbId(String tmdbId, Movie movie) {

        // Configura Retrofit para realizar llamadas a la API de TMDb sin headers adicionales
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.themoviedb.org/3/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Crea una instancia del servicio API definido en TMDbApiService
        TMDbApiService tmdbApiService = retrofit.create(TMDbApiService.class);

        // Realiza una llamada a la API para obtener los IDs externos
        Call<JsonObject> call = tmdbApiService.getExternalIds(tmdbId, "Bearer " + TMDB_API_KEY);

        // Maneja la respuesta de la llamada de forma asíncrona utilizando enqueue
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                // Comprobar si la respuesta fue exitosa y el cuerpo de la respuesta no es nulo
                if (response.isSuccessful() && response.body() != null) {

                    // Intenta obtener el elemento "imdb_id" del JSON de la respuesta
                    JsonElement imdbIdElement = response.body().get("imdb_id");

                    // Comprobar si "imdb_id" no es nulo ni un valor JSON nulo
                    if (imdbIdElement != null && !imdbIdElement.isJsonNull()) {

                        // Asigna el ID de IMDB al objeto Movie
                        String imdbId = imdbIdElement.getAsString();
                        movie.setId(imdbId);

                    } else {
                        movie.setId("ID no disponible");
                    }
                } else {
                    movie.setId("ID no disponible");
                }

            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                movie.setId("ID no disponible");
            }

        });
    }

}
