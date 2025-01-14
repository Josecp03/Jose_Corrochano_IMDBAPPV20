package edu.pmdm.corrochano_josimdbapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

import java.util.concurrent.TimeUnit;

import edu.pmdm.corrochano_josimdbapp.api.IMDBApiService;
import edu.pmdm.corrochano_josimdbapp.models.Movie;
import edu.pmdm.corrochano_josimdbapp.models.MovieOverviewResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MovieDetailsActivity extends AppCompatActivity {

    // Atributos
    private Movie pelicula;
    private TextView txtTitle = null;
    private TextView txtDescription = null;
    private TextView txtDate = null;
    private IMDBApiService imdbApiService;
    private ImageView imagen;
    private Button btnSMS = null;
    private double rating;
    private static final int PERMISSION_REQUEST_CODE_CONTACTS = 101;
    private static final int PERMISSION_REQUEST_CODE_SMS = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_movie_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Recoger los datos del intent
        Intent i = getIntent();
        pelicula = i.getParcelableExtra("pelicula");

        // Asignar los valores del XML
        txtTitle = findViewById(R.id.TextViewTitle);
        txtDescription = findViewById(R.id.TextViewDescription);
        txtDate = findViewById(R.id.TextViewDate);
        btnSMS = findViewById(R.id.btnSendSms);
        imagen = findViewById(R.id.ImageViewPortada);

        // Asignar el título a la película comprobando antes si es correcto el valor
        String title;
        if (pelicula != null && pelicula.getTitle() != null && !pelicula.getTitle().isEmpty()) {
            title = pelicula.getTitle();
        } else {
            title = "Título no disponible";
        }
        txtTitle.setText(title);


        // Asignar el título a la película con Glide comprobando antes si es correcto el valor
        String posterPath  = pelicula.getPosterPath();

        // Verificar si el valor de la portada está bien
        if (posterPath != null && !posterPath.endsWith("No+Image") && !posterPath.isEmpty()) {

            // Cargar la imagen desde el posterPath usando Glide
            Glide.with(this)
                    .load(posterPath)
                    .placeholder(R.mipmap.placeholderportada)
                    .error(R.mipmap.placeholderportada)
                    .into(imagen);
        } else {

            // Cargar una imagen predeterminada si el valor de la portada falla
            Glide.with(this)
                    .load(R.mipmap.placeholderportada)
                    .into(imagen);

        }

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

        // Comprobar si los datos de la película se han obtenido correctamente del intent
        if (pelicula != null && pelicula.getId() != null && !pelicula.getId().isEmpty() && !pelicula.getId().equals("ID no disponible")) {

            // Realiza una llamada a la API para obtener los datos detallados de la película usando su ID
            Call<MovieOverviewResponse> call = imdbApiService.obtenerDatos(pelicula.getId());

            // Maneja la respuesta de la llamada de forma asíncrona utilizando enqueue
            call.enqueue(new Callback<MovieOverviewResponse>() {
                @Override
                public void onResponse(Call<MovieOverviewResponse> call, Response<MovieOverviewResponse> response) {

                    // Verifica si la respuesta fue exitosa y contiene un cuerpo válido
                    if (response.isSuccessful() && response.body() != null) {

                        // Obtiene los datos principales de la película
                        MovieOverviewResponse.Data data = response.body().getData();

                        // Verifica si los datos de la película no son nulos
                        if (data != null) {

                            // Obtiene la sección del título de la película desde los datos principales
                            MovieOverviewResponse.Title titleData = data.getTitle();

                            // Verifica si los datos del título no son nulos
                            if (titleData != null) {

                                // Manejo de la descripción con un valor predeterminado en caso de que falte información
                                String descripcion = "Descripción no disponible";
                                if (titleData.getPlot() != null &&
                                        titleData.getPlot().getPlotText() != null &&
                                        titleData.getPlot().getPlotText().getPlainText() != null &&
                                        !titleData.getPlot().getPlotText().getPlainText().isEmpty()) {
                                    descripcion = titleData.getPlot().getPlotText().getPlainText();
                                }
                                txtDescription.setText(descripcion);

                                // Manejo de la fecha de lanzamiento con un valor predeterminado
                                String formattedDate = "Fecha no disponible";
                                MovieOverviewResponse.ReleaseDate releaseDate = titleData.getReleaseDate();
                                if (releaseDate != null) {

                                    // Formatea la fecha en el formato "YYYY-MM-DD"
                                    formattedDate = String.format("%d-%02d-%02d", releaseDate.getYear(), releaseDate.getMonth(), releaseDate.getDay());

                                }
                                txtDate.setText("Release Date: " + formattedDate);

                                // Manejo del rating con un valor predeterminado
                                String ratingText = "Rating: No disponible";
                                MovieOverviewResponse.RatingsSummary ratingsSummary = titleData.getRatingsSummary();
                                if (ratingsSummary != null) {
                                    rating = ratingsSummary.getAggregateRating();
                                    ratingText = "Rating: " + String.format("%.1f", rating);
                                }
                                TextView ratingView = findViewById(R.id.TextViewRating);
                                ratingView.setText(ratingText);

                            } else {
                                asignarValoresPredeterminados();
                            }
                        } else {
                            asignarValoresPredeterminados();
                        }
                    } else {
                        asignarValoresPredeterminados();
                    }
                }

                @Override
                public void onFailure(Call<MovieOverviewResponse> call, Throwable t) {
                    asignarValoresPredeterminados();
                }

            });

        } else {
            asignarValoresPredeterminados();
        }

        // Listener para cuando se pulsa el botón de enviar SMS
        btnSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Verificar permisos de contactos
                if (ContextCompat.checkSelfPermission(MovieDetailsActivity.this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MovieDetailsActivity.this, new String[]{Manifest.permission.READ_CONTACTS}, PERMISSION_REQUEST_CODE_CONTACTS);
                } else if (ContextCompat.checkSelfPermission(MovieDetailsActivity.this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    // Verificar permisos de SMS
                    ActivityCompat.requestPermissions(MovieDetailsActivity.this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_REQUEST_CODE_SMS);
                } else {
                    // Lanzar el método cuando ambos permisos están concedidos
                    sendSms();
                }

            }
        });

    }

    private void sendSms() {
        // Crear y lanzar el intent
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, PERMISSION_REQUEST_CODE_CONTACTS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Verifica si el código de solicitud corresponde al permiso de contactos
        if (requestCode == PERMISSION_REQUEST_CODE_CONTACTS) {

            // Comprueba si el resultado de los permisos no está vacío y si se concedió el permiso
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // Solicitar el permiso para enviar SMS
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_REQUEST_CODE_SMS);

            } else {
                Toast.makeText(this, "Permiso de contactos denegado.", Toast.LENGTH_SHORT).show();
            }

        } else if (requestCode == PERMISSION_REQUEST_CODE_SMS) { // Verifica si el código de solicitud corresponde al permiso para enviar SMS

            // Comprueba si el resultado de los permisos no está vacío y si se concedió el permiso
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendSms();
            } else {
                Toast.makeText(this, "Permiso para enviar SMS denegado.", Toast.LENGTH_SHORT).show();
            }

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Verifica si el código de solicitud corresponde al permiso de contactos y si el resultado fue exitoso
        if (requestCode == PERMISSION_REQUEST_CODE_CONTACTS && resultCode == RESULT_OK && data != null) {

            // Obtiene la URI del contacto seleccionado en el intent de selección de contacto
            Uri contactUri = data.getData();

            // Obtiene el ID del contacto a partir de la URI del contacto llamando al método
            String idContacto = obtenerIdContacto(contactUri);

            // Comprueba si se pudo obtener un ID de contacto válido
            if (idContacto != null) {

                // Obtiene el número de teléfono asociado al ID del contacto
                String numTelefono = obtenerTelefono(idContacto);

                // Comprueba si el número de teléfono no es nulo ni está vacío
                if (numTelefono != null && !numTelefono.isEmpty()) {

                    // Construir el mensaje para el SMS con el rating formateado
                    String textoSMS = "Esta película te gustará: " + txtTitle.getText().toString() +
                            " Rating: " + String.format("%.1f", rating);

                    // Enviar el SMS con los detalles de la película
                    enviarSMS(numTelefono, textoSMS);

                } else {
                    Toast.makeText(this, "El contacto no tiene número de teléfono.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void enviarSMS(String numero, String texto) {

        // Comprueba si los permisos están otorgados
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_REQUEST_CODE_SMS);
        } else {
            abrirAppSMS(numero, texto);
        }
    }

    private void abrirAppSMS(String numero, String texto) {

        // Comprueba que los valores no sean nulos
        if (numero == null || texto == null || numero.isEmpty() || texto.isEmpty()) {
            Toast.makeText(this, "No se tiene número o texto para enviar.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crea un Intent para iniciar la aplicación de SMS con la acción ACTION_SENDTO
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);

        // Define el esquema URI para el número de teléfono requerido por la acción SENDTO
        smsIntent.setData(Uri.parse("smsto:" + numero));

        // Agrega el texto del mensaje como extra usando la clave "sms_body"
        smsIntent.putExtra("sms_body", texto);

        // Lanza el intent
        startActivity(smsIntent);

    }

    private String obtenerIdContacto(Uri contactUri) {

        // Inicializa la variable que contendrá el ID del contacto
        String idContacto = null;

        // Realiza una consulta a la base de datos de contactos utilizando la URI proporcionada
        Cursor cursor = getContentResolver().query(contactUri, null, null, null, null);

        // Verifica si el cursor no es nulo y contiene al menos un resultado
        if (cursor != null && cursor.moveToFirst()) {

            // Obtiene el índice de la columna que contiene el ID del contacto
            int idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);

            // Obtiene el valor del ID del contacto desde el cursor
            idContacto = cursor.getString(idIndex);

        }

        // Devuelve el id del contacto
        return idContacto;

    }

    private String obtenerTelefono(String contactId) {

        // Inicializa la variable que contendrá el número de teléfono
        String numTelefono = null;

        // Realiza una consulta en la base de datos de contactos para obtener los números de teléfono asociados al contacto
        Cursor cursorTelefono = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{contactId},
                null
        );

        // Verifica si el cursor no es nulo y contiene al menos un resultado
        if (cursorTelefono != null && cursorTelefono.moveToFirst()) {
            int numberIndex = cursorTelefono.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            numTelefono = cursorTelefono.getString(numberIndex);
        }

        // Devuelve el número de teléfono del contacto
        return numTelefono;

    }

    private void asignarValoresPredeterminados () {

        // Asigna valores predeterminados por si algun dato falla
        txtDescription.setText("Descripción no disponible");
        txtDate.setText("Release Date: No disponible");
        TextView ratingView = findViewById(R.id.TextViewRating);
        ratingView.setText("Rating: No disponible");

    }

}
