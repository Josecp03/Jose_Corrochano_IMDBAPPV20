package edu.pmdm.corrochano_josimdbapp;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SelectAddressActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Atributos
    private GoogleMap mMap;
    private LatLng selectedLatLng;
    private String selectedAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_select_address);

        // Inicializar Places SDK si no está ya inicializado
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Configurar el fragmento de Autocomplete para búsqueda de direcciones
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteFragment.setPlaceFields(Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
        ));
        autocompleteFragment.setHint("Buscar dirección...");

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                // Al seleccionar una dirección, se obtiene el texto de la dirección y las coordenadas
                selectedAddress = place.getAddress();
                LatLng latLng = place.getLatLng();
                if (latLng != null) {
                    selectedLatLng = latLng;
                    mMap.clear();
                    mMap.addMarker(new MarkerOptions().position(latLng).title(selectedAddress));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f));
                }
            }

            @Override
            public void onError(@NonNull Status status) {
                // Mostrar un mensaje de error en caso de fallo en la selección
                Toast.makeText(SelectAddressActivity.this, "Error: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Inicializar el fragmento del mapa y solicitar la carga asíncrona del mapa
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Configurar el botón para confirmar la dirección seleccionada
        findViewById(R.id.buttonConfirmAddress).setOnClickListener(v -> {
            if (selectedLatLng != null && selectedAddress != null) {
                // Si se ha seleccionado una dirección, devolverla al activity que llamó
                Intent intent = new Intent();
                intent.putExtra("SELECTED_ADDRESS", selectedAddress);
                setResult(RESULT_OK, intent);
                finish();
            } else {
                // Si no se ha seleccionado ninguna ubicación, mostrar un mensaje
                Toast.makeText(SelectAddressActivity.this, "No has seleccionado ninguna ubicación", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Mover la cámara a España como posición inicial
        LatLng spain = new LatLng(40.416775, -3.703790);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(spain, 5f));

        // Configurar un listener para detectar clics en el mapa
        mMap.setOnMapClickListener(latLng -> {
            // Al hacer clic en el mapa, actualizar las coordenadas seleccionadas
            selectedLatLng = latLng;
            // Obtener la dirección a partir de las coordenadas
            selectedAddress = getAddressFromLatLng(latLng.latitude, latLng.longitude);
            if (selectedAddress != null) {
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(latLng).title(selectedAddress));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f));
            } else {
                Toast.makeText(SelectAddressActivity.this, "No se pudo obtener la dirección", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Método para obtener una dirección a partir de coordenadas
    private String getAddressFromLatLng(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> results = geocoder.getFromLocation(lat, lng, 1);
            if (results != null && !results.isEmpty()) {
                Address address = results.get(0);
                // Formatear la dirección concatenando los distintos componentes
                StringBuilder sb = new StringBuilder();
                if (address.getThoroughfare() != null) {
                    sb.append(address.getThoroughfare()).append(", ");
                }
                if (address.getLocality() != null) {
                    sb.append(address.getLocality()).append(", ");
                }
                if (address.getAdminArea() != null) {
                    sb.append(address.getAdminArea()).append(", ");
                }
                if (address.getCountryName() != null) {
                    sb.append(address.getCountryName());
                }
                // Retornar la dirección formateada y recortada
                return sb.toString().trim();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Si no se puede obtener la dirección, retornar null
        return null;
    }
}
