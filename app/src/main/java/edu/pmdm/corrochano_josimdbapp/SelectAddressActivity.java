package edu.pmdm.corrochano_josimdbapp;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment; // Si usas SupportMapFragment
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class SelectAddressActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private EditText editTextSearch;
    private Button buttonSearch, buttonConfirm;

    // Guardamos la última ubicación elegida
    private LatLng selectedLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_select_address);

        // Ajustar insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editTextSearch = findViewById(R.id.editTextSearchAddress);
        buttonSearch   = findViewById(R.id.buttonSearchAddress);
        buttonConfirm  = findViewById(R.id.buttonConfirmAddress);

        // Iniciar el fragment de mapa
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Botón Buscar
        buttonSearch.setOnClickListener(v -> {
            String query = editTextSearch.getText().toString().trim();
            if (!TextUtils.isEmpty(query)) {
                searchAddress(query);
            } else {
                Toast.makeText(this, "Introduce alguna dirección", Toast.LENGTH_SHORT).show();
            }
        });

        // Botón Confirmar
        buttonConfirm.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                // Obtener la dirección final como String
                String finalAddress = getAddressFromLatLng(
                        selectedLatLng.latitude, selectedLatLng.longitude
                );
                if (!TextUtils.isEmpty(finalAddress)) {
                    // Devolver a EditUserActivity
                    getIntent().putExtra("SELECTED_ADDRESS", finalAddress);
                    setResult(RESULT_OK, getIntent());
                    finish();
                } else {
                    Toast.makeText(this, "No se pudo obtener dirección", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No has seleccionado ninguna ubicación", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Opcional: mover la cámara a España, por ejemplo
        LatLng spain = new LatLng(40.416775, -3.703790);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(spain, 5f));

        // Al pulsar en el mapa => marcamos
        mMap.setOnMapClickListener(latLng -> {
            selectedLatLng = latLng;
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng).title("Ubicación seleccionada"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f));
        });
    }

    /**
     * Buscar con Geocoder la dirección escrita por el usuario
     */
    private void searchAddress(String query) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> results = geocoder.getFromLocationName(query, 1);
            if (results != null && !results.isEmpty()) {
                Address address = results.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                selectedLatLng = latLng;

                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(latLng).title(query));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f));
            } else {
                Toast.makeText(this, "No se encontró resultado para: " + query, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error de geocoding: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Obtener un String formateado a partir de lat/lng con Geocoder
     */
    private String getAddressFromLatLng(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> results = geocoder.getFromLocation(lat, lng, 1);
            if (results != null && !results.isEmpty()) {
                Address address = results.get(0);

                // Un ejemplo simple de formateo
                StringBuilder sb = new StringBuilder();
                if (address.getThoroughfare() != null) sb.append(address.getThoroughfare()).append(", ");
                if (address.getLocality() != null) sb.append(address.getLocality()).append(", ");
                if (address.getAdminArea() != null) sb.append(address.getAdminArea()).append(", ");
                if (address.getCountryName() != null) sb.append(address.getCountryName()).append(" ");
                if (address.getPostalCode() != null) sb.append(address.getPostalCode());

                return sb.toString().trim();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}