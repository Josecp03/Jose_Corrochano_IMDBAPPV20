package edu.pmdm.corrochano_josimdbapp.ui.top10;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import edu.pmdm.corrochano_josimdbapp.adapters.MovieAdapter;
import edu.pmdm.corrochano_josimdbapp.api.IMDBApiService;
import edu.pmdm.corrochano_josimdbapp.databinding.FragmentTop10Binding;
import edu.pmdm.corrochano_josimdbapp.database.FavoriteDatabaseHelper;
import edu.pmdm.corrochano_josimdbapp.models.Movie;
import edu.pmdm.corrochano_josimdbapp.models.PopularMoviesResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Top10Fragment extends Fragment {

    // Atributos
    private FragmentTop10Binding binding;
    private IMDBApiService imdbApiService;
    private List<Movie> movieList = new ArrayList<>();
    private MovieAdapter adapter;
    private RecyclerView re;
    private FavoriteDatabaseHelper databaseHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // Obtener el idUsuario desde Firebase Auth
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String idUsuario = mAuth.getCurrentUser().getUid();

        // Inicialización de la base de datos y demás componentes
        binding = FragmentTop10Binding.inflate(inflater, container, false);
        View root = binding.getRoot();
        databaseHelper = new FavoriteDatabaseHelper(getContext());

        // Configurar RecyclerView
        re = binding.recycler;
        re.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new MovieAdapter(getContext(), movieList, idUsuario, databaseHelper, false);
        re.setAdapter(adapter);

        // Configuración de la API
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request modifiedRequest = chain.request().newBuilder()
                            .addHeader("X-RapidAPI-Key", "1d6b8bf5bemsh13bad6e5b669b95p146504jsnaa743711d880")
                            .addHeader("X-RapidAPI-Host", "imdb-com.p.rapidapi.com")
                            .build();
                    return chain.proceed(modifiedRequest);
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        // Inicialización de Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://imdb-com.p.rapidapi.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Crear una instancia de la interfaz IMDBApiService para realizar las solicitudes a la API
        imdbApiService = retrofit.create(IMDBApiService.class);

        // Realiza una llamada a la API para obtener las 10 películas más populares en los Estados Unidos
        Call<PopularMoviesResponse> call = imdbApiService.obtenerTop10("US");

        // Maneja la respuesta de la llamada de forma asíncrona utilizando enqueue
        call.enqueue(new Callback<PopularMoviesResponse>() {
            @Override
            public void onResponse(Call<PopularMoviesResponse> call, Response<PopularMoviesResponse> response) {

                // Verifica si la respuesta fue exitosa y contiene un cuerpo válido
                if (response.isSuccessful() && response.body() != null) {

                    // Extrae la lista de conexiones de las películas desde la respuesta
                    List<PopularMoviesResponse.Edge> edges = response.body().getData().getTopMeterTitles().getEdges();

                    // Comprueba que la lista de edges no sea nula ni esté vacía
                    if (edges != null && !edges.isEmpty()) {

                        // Limpia la lista de películas actual antes de agregar nuevas
                        movieList.clear();

                        // Itera sobre los primeros 10 elementos de la lista de edges o el tamaño total si es menor a 10
                        for (int i = 0; i < Math.min(edges.size(), 10); i++) {

                            // Obtiene el edge actual y extrae el nodo que contiene los datos de la película
                            PopularMoviesResponse.Edge edge = edges.get(i);
                            PopularMoviesResponse.Node node = edge.getNode();

                            // Crea un nuevo objeto Movie y asigna los valores obtenidos del nodo
                            Movie movie = new Movie();
                            movie.setId(node.getId());
                            movie.setTitle(node.getTitleText().getText());
                            movie.setReleaseDate(node.getPrimaryImage().getUrl());
                            movie.setPosterPath(node.getPrimaryImage().getUrl());

                            // Agrega la película a la lista
                            movieList.add(movie);

                        }

                        // Notifica al adaptador que los datos de la lista han cambiado para actualizar la vista
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    Toast.makeText(getContext(), "Error al cargar películas", Toast.LENGTH_SHORT).show();
                }
            }

            // Muestra un mensaje de error si ocurre un fallo en la conexión o en la llamada a la API
            @Override
            public void onFailure(Call<PopularMoviesResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Error en la llamda a la API", Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}