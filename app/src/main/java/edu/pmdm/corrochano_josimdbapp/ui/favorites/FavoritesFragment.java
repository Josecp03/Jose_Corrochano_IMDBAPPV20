package edu.pmdm.corrochano_josimdbapp.ui.favorites;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import java.util.ArrayList;
import java.util.List;

import edu.pmdm.corrochano_josimdbapp.R;
import edu.pmdm.corrochano_josimdbapp.adapters.MovieAdapter;
import edu.pmdm.corrochano_josimdbapp.database.FavoriteDatabaseHelper;
import edu.pmdm.corrochano_josimdbapp.databinding.FragmentFavoritesBinding;
import edu.pmdm.corrochano_josimdbapp.models.Movie;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

public class FavoritesFragment extends Fragment {

    // Atributos
    private FragmentFavoritesBinding binding;
    private Handler mainHandler;
    private boolean favoritos = true;
    private String idUsuario;
    private List<Movie> pelisFavoritas = new ArrayList<>();
    private FavoriteDatabaseHelper database;
    private MovieAdapter adapter;
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;
    private ActivityResultLauncher<Intent> enableBluetoothLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Obtener el id del usuario registrado
        idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Crear la base de datos y un Handler para ejecutar tareas en la hebra principal
        database = new FavoriteDatabaseHelper(getContext());
        mainHandler = new Handler(Looper.getMainLooper());

        // Configurar el RecyclerView
        binding.recyclerViewFavoritos.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new MovieAdapter(getContext(), pelisFavoritas, idUsuario, database, favoritos);
        binding.recyclerViewFavoritos.setAdapter(adapter);

        // Llamada al método para cargar las películas favoritas
        cargarPeliculasFavoritas();

        // Registrar lanzadores para permisos y activar Bluetooth
        registerLaunchers();

        // Listener para cuando se pulsa sobre el botón de compartir
        binding.buttonShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissionsAndShare();
            }
        });

        return root;

    }

    private void requestPermissionsAndShare() {

        // Verificar si ya se tiene los permisos
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

            // Solicitar permisos
            requestPermissionsLauncher.launch(new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            });

        } else {

            // Llamar al método para comprobar si tiene activo el bluettoth
            checkAndEnableBluetooth();

        }
    }


    // Método para pedir los permisos de Bluetooth
    private void registerLaunchers() {

        // Lanzador para solicitar permisos
        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {

                    // Variable booleana para controlar si los permisos han sido ya concedidos
                    boolean allGranted = true;

                    // Si algún permiso no ha sido concedido, actualizar la variable booleana allGranted
                    for (Boolean granted : result.values()) {
                        allGranted = allGranted && granted;
                    }

                    // Verificar si todos los permisos fueron otorgados
                    if (allGranted) {

                        // Verificar que esté encendido el Bluetooth llamando al método
                        checkAndEnableBluetooth();

                    } else {
                        Toast.makeText(getContext(), "Permisos denegados", Toast.LENGTH_SHORT).show();
                    }

                }
        );

        // Lanzador para solicitar activar Bluetooth
        enableBluetoothLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {

                    // Verificar si todos los permisos fueron otorgados
                    if (result.getResultCode() == getActivity().RESULT_OK) {

                        // Llamar al método para mostrar el diálogo
                        showShareDialog();

                    } else {
                        Toast.makeText(getContext(), "Bluetooth no fue activado", Toast.LENGTH_SHORT).show();
                    }

                }
        );
    }

    // Método para comprobar si tiene activado el Bluettoth
    private void checkAndEnableBluetooth() {

        // Obtener el adaptador Bluetooth del dispositivo
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Verificar si el adaptador Bluetooth está disponible en el dispositivo
        if (bluetoothAdapter != null) {

            // Comprobar si Bluetooth está desactivado
            if (!bluetoothAdapter.isEnabled()) {

                // Crear un Intent para solicitar al usuario que active Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBluetoothLauncher.launch(enableBtIntent);

            } else {

                // Llamar al método para mostrar el diálogo
                showShareDialog();

            }
        } else {
            Toast.makeText(getContext(), "Bluetooth no está soportado en este dispositivo.", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para mostrar el diálogo
    private void showShareDialog() {

        // Comprobar que haya películas en favoritos
        if (pelisFavoritas.isEmpty()) {
            Toast.makeText(getContext(), "Error al compartir. No hay películas en favoritos", Toast.LENGTH_SHORT).show();
        } else {

            // Convertir el ArrayList a JSON y luego a String
            Gson gson = new Gson();
            String cadenaJSON = gson.toJson(pelisFavoritas);

            // Crear el diálogo llamando al método pasandole la cadena como parámetro
            AlertDialog dialogoShare = crearDiaogoBluetooth(cadenaJSON);
            dialogoShare.show();

        }

    }

    // Método para crear e inflar el diálogo en el fragmento
    private AlertDialog crearDiaogoBluetooth(String listaPeliculasJSON) {

        // Inicializar Variables
        AlertDialog dialogo = null;

        // Convertir el archivo XML del diseño del diálogo en un objeto View para poder utilizarlo
        View alertCustomDialog = LayoutInflater.from(getContext()).inflate(R.layout.custom_dialog_compartir, null);

        // Constructor del diálogo
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());

        // Establecer la vista personalizada como el contenido del diálogo
        alertDialog.setView(alertCustomDialog);

        // Inicializar variables
        ImageButton cancelButton = alertCustomDialog.findViewById(R.id.cancelID);

        // Asignar valores al diálogo
        TextView txtTitle = alertCustomDialog.findViewById(R.id.TextViewTitle);
        TextView txtContenido = alertCustomDialog.findViewById(R.id.TextViewContent);
        txtContenido.setText(listaPeliculasJSON);
        txtTitle.setText("Películas favoritas en JSON");

        // Crear el Diálogo
        dialogo = alertDialog.create();

        // Establecer fondo del diálogo transparente
        if (dialogo.getWindow() != null) {
            dialogo.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Añadir evento cuando se pulsa el icono de salir
        ImageButton finalCancelButton = cancelButton;
        AlertDialog finalDialogo = dialogo;
        finalCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finalDialogo.cancel();
            }
        });

        // Devolver el dialogo creado
        return dialogo;

    }

    // Método para cargar las películas favoritas de la base de datos
    private void cargarPeliculasFavoritas() {

        // Ejecutar la operación de lectura de la base de datos en un hilo separado
        new Thread(() -> {

            // Obtener una instancia de la base de datos en modo lectura
            SQLiteDatabase db = database.getReadableDatabase();

            // Consultar la tabla de favoritos para obtener las películas del usuario actual
            Cursor cursor = db.rawQuery(
                    "SELECT idPelicula, nombrePelicula, portadaURL FROM " + FavoriteDatabaseHelper.TABLE_FAVORITOS + " WHERE idUsuario=?",
                    new String[]{idUsuario}
            );

            // Limpiar la lista de películas favoritas para que no se dupliquen
            pelisFavoritas.clear();

            // Verificar si hay resultados en la consulta
            if (cursor != null && cursor.moveToFirst()) {

                // Recorrer todas las líneas del cursor
                do {

                    // Obtener los datos de cada fila del cursor.
                    @SuppressLint("Range") String idPelicula = cursor.getString(cursor.getColumnIndex("idPelicula"));
                    @SuppressLint("Range") String titulo = cursor.getString(cursor.getColumnIndex("nombrePelicula"));
                    @SuppressLint("Range") String portada = cursor.getString(cursor.getColumnIndex("portadaURL"));

                    // Crear un objeto Movie con los datos obtenidos
                    Movie movie = new Movie();
                    movie.setId(idPelicula);
                    movie.setTitle(titulo);
                    movie.setPosterPath(portada);

                    // Agregar la película a la lista de favoritas
                    pelisFavoritas.add(movie);

                } while (cursor.moveToNext());
            }

            // Cerrar la base de datos para liberar recursos
            db.close();

            // Volver a la hebra principal para actualizar la interfaz de usuario
            mainHandler.post(() -> {

                // Comprobar si hay películas favoritas guardadas
                if (!pelisFavoritas.isEmpty()) {
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getContext(), "No tienes películas favoritas guardadas", Toast.LENGTH_SHORT).show();
                }

            });

        }).start();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}