package edu.pmdm.corrochano_josimdbapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.hbb20.CountryCodePicker;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import edu.pmdm.corrochano_josimdbapp.database.FavoriteDatabaseHelper;

public class EditUserActivity extends AppCompatActivity {

    // Request codes
    private static final int RC_CAMERA = 100;           // Para el intent de cámara
    private static final int RC_GALLERY = 101;          // Para el intent de galería
    private static final int RC_CAMERA_PERMISSION = 200; // Para solicitar permiso de cámara

    private Uri cameraImageUri;

    // Vistas
    private EditText edtName;
    private EditText edtEmail;
    private EditText edtAddress;
    private EditText edtPhone;
    private ImageView userImageView;
    private Button btnDirection;
    private Button btnImage;
    private Button btnSave;
    private CountryCodePicker countryCodePicker;

    private FirebaseAuth mAuth;
    private FavoriteDatabaseHelper dbHelper;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_user);

        // Ajuste para insets (barras de estado/navigation)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Instancias
        mAuth = FirebaseAuth.getInstance();
        dbHelper = new FavoriteDatabaseHelper(this);

        // Referencias UI
        edtName      = findViewById(R.id.editTextTextName);
        edtEmail     = findViewById(R.id.editTextTextEmail);
        edtAddress   = findViewById(R.id.editTextTextAddress);
        edtPhone     = findViewById(R.id.editTextNumberPhone);
        userImageView= findViewById(R.id.imageView);
        btnDirection = findViewById(R.id.buttonSelectDirection);
        btnImage     = findViewById(R.id.buttonSelectImage);
        btnSave      = findViewById(R.id.buttonSave);
        countryCodePicker = findViewById(R.id.countryCodePicker);

        // Restaura prefijo país de SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int lastCode = prefs.getInt("LAST_COUNTRY_CODE", -1);
        if (lastCode != -1) {
            countryCodePicker.setCountryForPhoneCode(lastCode);
        } else {
            countryCodePicker.setCountryForPhoneCode(
                    Integer.parseInt(countryCodePicker.getDefaultCountryCode())
            );
        }
        countryCodePicker.setOnCountryChangeListener(() -> {
            int selectedCode = countryCodePicker.getSelectedCountryCodeAsInt();
            prefs.edit().putInt("LAST_COUNTRY_CODE", selectedCode).apply();
        });

        // Si se pasó la foto de MainActivity, la cargamos en el ImageView
        String photoUriString = getIntent().getStringExtra("EXTRA_PROFILE_PICTURE_URI");
        if (photoUriString != null && !photoUriString.isEmpty()) {
            Uri photoUri = Uri.parse(photoUriString);
            Glide.with(this).load(photoUri).circleCrop().into(userImageView);
        }

        // Cargar datos del usuario en la base local
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery(
                    "SELECT " + FavoriteDatabaseHelper.COL_NAME + ", " + FavoriteDatabaseHelper.COL_EMAIL +
                            " FROM " + FavoriteDatabaseHelper.TABLE_USUARIOS +
                            " WHERE " + FavoriteDatabaseHelper.COL_USER_ID + " = ?",
                    new String[]{ userId }
            );

            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                String email = cursor.getString(1);

                edtName.setText(name);
                edtEmail.setText(email);
                // Bloquear el email (solo lectura)
                edtEmail.setKeyListener(null);
                edtEmail.setFocusable(false);
                edtEmail.setCursorVisible(false);

                cursor.close();
            }
            db.close();
        }

        // Botón para seleccionar imagen (cámara, galería, URL)
        btnImage.setOnClickListener(v -> showImageOptionsDialog());

        // Botón Guardar
        btnSave.setOnClickListener(v -> {
            // Validar phone
            String phoneNumber = edtPhone.getText().toString().trim();
            String countryCode = countryCodePicker.getSelectedCountryCode();
            String fullPhone = "+" + countryCode + phoneNumber;

            if (!isValidPhoneNumber(fullPhone, countryCodePicker.getSelectedCountryNameCode())) {
                Toast.makeText(this, "Número de teléfono inválido para el país seleccionado", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validar nombre
            String newName = edtName.getText().toString().trim();
            if (TextUtils.isEmpty(newName)) {
                Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                return;
            }

            // Actualizar en la base de datos local
            if (currentUser != null) {
                dbHelper.updateUserName(currentUser.getUid(), newName);
            }

            Toast.makeText(this, "Datos guardados correctamente.", Toast.LENGTH_SHORT).show();

            // Volver a MainActivity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    /**
     * Muestra un diálogo con opciones para escoger la imagen: Cámara, Galería o URL.
     */
    private void showImageOptionsDialog() {
        String[] items = {"Cámara", "Galería", "URL externa"};

        new android.app.AlertDialog.Builder(this)
                .setTitle("Seleccionar imagen")
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openCamera();
                            break;
                        case 1:
                            openGallery();
                            break;
                        case 2:
                            showUrlDialog();
                            break;
                    }
                })
                .create()
                .show();
    }

    /**
     * Verifica si tenemos permiso de cámara; si no, lo pide.
     * Si está concedido, lanza la cámara.
     */
    private void openCamera() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Pedir permiso
            requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    RC_CAMERA_PERMISSION
            );
        } else {
            // Ya tenemos permiso => lanzar cámara
            launchCameraIntent();
        }
    }

    /**
     * Lanza el intent de cámara para tomar foto y guardarla en cameraImageUri.
     */
    private void launchCameraIntent() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

        File photoFile;
        try {
            photoFile = createTempImageFile();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creando archivo para la cámara", Toast.LENGTH_SHORT).show();
            return;
        }

        cameraImageUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                photoFile
        );

        cameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, cameraImageUri);
        startActivityForResult(cameraIntent, RC_CAMERA);
    }

    /**
     * Crear un archivo temporal para la foto en getExternalFilesDir(Environment.DIRECTORY_PICTURES)
     */
    private File createTempImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    /**
     * Abre la galería para seleccionar una imagen
     */
    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, RC_GALLERY);
    }

    /**
     * Muestra un diálogo para introducir la URL externa de la imagen.
     */
    private void showUrlDialog() {
        final EditText input = new EditText(this);
        input.setHint("https://ejemplo.com/foto.png");

        new android.app.AlertDialog.Builder(this)
                .setTitle("Introduce la URL de la imagen")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) {
                        loadImageIntoView(Uri.parse(url));
                    } else {
                        Toast.makeText(this, "URL vacía", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Recoger el resultado de cámara/galería
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == RC_CAMERA) {
                // Imagen de la cámara en cameraImageUri
                if (cameraImageUri != null) {
                    loadImageIntoView(cameraImageUri);
                }
            } else if (requestCode == RC_GALLERY) {
                // Imagen de la galería
                if (data != null && data.getData() != null) {
                    Uri galleryUri = data.getData();
                    loadImageIntoView(galleryUri);
                }
            }
        }
    }

    /**
     * Maneja la respuesta del usuario al pedir permiso de cámara.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RC_CAMERA_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido => lanzar cámara
                launchCameraIntent();
            } else {
                // Permiso denegado => notificar
                Toast.makeText(this, "No se concedió permiso de cámara", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Carga la imagen en el ImageView con Glide (circleCrop para redonda)
     */
    private void loadImageIntoView(Uri uri) {
        Glide.with(this)
                .load(uri)
                .circleCrop()
                .into(userImageView);
    }

    /**
     * Valida el número de teléfono según el país seleccionado.
     */
    private boolean isValidPhoneNumber(String fullPhone, String selectedCountryCode) {
        if (TextUtils.isEmpty(fullPhone) || TextUtils.isEmpty(selectedCountryCode)) {
            return false;
        }

        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil.parse(fullPhone, selectedCountryCode);
            return phoneNumberUtil.isValidNumberForRegion(phoneNumber, selectedCountryCode);
        } catch (NumberParseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
