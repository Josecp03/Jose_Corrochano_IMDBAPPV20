package edu.pmdm.corrochano_josimdbapp.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import edu.pmdm.corrochano_josimdbapp.MovieListActivity;
import edu.pmdm.corrochano_josimdbapp.api.TMDbApiService;
import edu.pmdm.corrochano_josimdbapp.databinding.FragmentSearchBinding;
import edu.pmdm.corrochano_josimdbapp.models.Genero;
import edu.pmdm.corrochano_josimdbapp.models.GeneroResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SearchFragment extends Fragment {

    // Atributos de la clase
    private FragmentSearchBinding binding;
    private TMDbApiService tmdbApiService;
    private Spinner spinnerGeneros;
    private List<Genero> generosList = new ArrayList<>();
    private static final String TMDB_API_KEY = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJmMDE2ZGRjOGRhYWZmYzUyYmM1MmUxN2I1MTQ2ZTk3MSIsIm5iZiI6MTczNjUzOTU1MC43NjksInN1YiI6IjY3ODE3ZDllYzVkMmU5NmUyNjdiNGMwZiIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.cP-LiqfqCtg1E7xRX6nPOT3cdttykNkk95N3dvGxkbA";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Vincular el atributo Spinner con el del XML
        spinnerGeneros = binding.spinner;

        // Configurar Retrofit con Interceptor para añadir headers necesarios a las solicitudes
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.themoviedb.org/3/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(new okhttp3.OkHttpClient.Builder()
                        .addInterceptor(chain -> {
                            okhttp3.Request request = chain.request().newBuilder()
                                    .addHeader("Authorization", "Bearer " + TMDB_API_KEY)
                                    .addHeader("accept", "application/json")
                                    .build();
                            return chain.proceed(request);
                        })
                        .build())
                .build();

        // Crear una instancia de la interfaz TMDbApiService para realizar las solicitudes a la API
        tmdbApiService = retrofit.create(TMDbApiService.class);

        // Llamada al método para obtener los géneros y establecerlos en el Spinner
        getGenres();

        // Listener para cuando se pulsa el botón de buscar
        binding.buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Comprobar que el año esté vacío
                if (binding.editTextNumberDate.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), "El año no puede estar vacío", Toast.LENGTH_SHORT).show();
                } else {

                    // Comprobar que el año sea mayor a 1900 (Límite personalizado)
                    if (Integer.parseInt(binding.editTextNumberDate.getText().toString()) < 1900 ) {
                        Toast.makeText(getContext(), "Error. Introduzca una fecha superior 1900", Toast.LENGTH_SHORT).show();
                    } else {

                        // Comprobar que el año sea menor al año actual + 1 (Límite personalizado)
                        if (Integer.parseInt(binding.editTextNumberDate.getText().toString()) > java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + 1) {
                            Toast.makeText(getContext(), "Error. Introduzca una fecha inferior a un año más que el actual", Toast.LENGTH_SHORT).show();
                        } else {

                            // Obtener la fecha
                            String date = binding.editTextNumberDate.getText().toString();

                            // Obtener la posición seleccionada en el Spinner
                            int selectedPosition = spinnerGeneros.getSelectedItemPosition();

                            // Obtener el género seleccionado
                            Genero selectedGenero = generosList.get(selectedPosition);

                            // Crear el Intent para MovieListActivity
                            Intent intent = new Intent(getActivity(), MovieListActivity.class);

                            // Pasar los datos como extras
                            intent.putExtra("year", date);
                            intent.putExtra("genreId", selectedGenero.getId());
                            intent.putExtra("genreName", selectedGenero.getNombre());

                            // Iniciar la actividad
                            startActivity(intent);

                        }

                    }

                }

            }

        });

        return root;
    }

    // Método para obtener los géneros de la API
    private void getGenres() {

        // Realiza una llamada a la API para obtener los géneros, especificando el idioma
        Call<GeneroResponse> call = tmdbApiService.getGenres("en-US");

        // Enqueue para realizar la llamada de forma asíncrona y manejar la respuesta
        call.enqueue(new Callback<GeneroResponse>() {
            @Override
            public void onResponse(Call<GeneroResponse> call, Response<GeneroResponse> response) {

                // Verifica si la respuesta fue exitosa y contiene un cuerpo válido
                if (response.isSuccessful() && response.body() != null) {

                    // Obtiene la lista de géneros de la respuesta de la API
                    generosList = response.body().getGenres();

                    // Crear una lista para almacenar los nombres de los géneros
                    List<String> generoNames = new ArrayList<>();

                    // Recorre la lista de géneros y extrae los nombres para mostrarlos en el Spinner
                    for (Genero genero : generosList) {
                        generoNames.add(genero.getNombre());
                    }

                    // Configura un adaptador para el Spinner usando la lista de nombres de géneros
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            generoNames
                    );

                    // Define el diseño para los elementos desplegables del Spinner
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    // Asigna el adaptador al Spinner para mostrar los datos
                    spinnerGeneros.setAdapter(adapter);

                } else {
                    Toast.makeText(getContext(), "Error al obtener los géneros", Toast.LENGTH_SHORT).show();
                }

            }

            // Muestra un mensaje de error si ocurre un fallo en la conexión o en la llamada a la API
            @Override
            public void onFailure(Call<GeneroResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Error al conectar con la API", Toast.LENGTH_SHORT).show();
            }

        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}