// Archivo: EditUserActivity.java
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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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
    private static final int RC_CAMERA          = 100; // Para el intent de cámara
    private static final int RC_GALLERY         = 101; // Para el intent de galería
    private static final int RC_CAMERA_PERMISSION = 200;
    private static final int RC_SELECT_ADDRESS  = 300; // NUEVO: Para SelectAddressActivity

    private Uri cameraImageUri;
    private String externalPhotoUrl = ""; // Nueva variable para almacenar la URL externa

    // Vistas
    private EditText edtName, edtEmail, edtAddress, edtPhone;
    private ImageView userImageView;
    private Button btnDirection, btnImage, btnSave;
    private CountryCodePicker countryCodePicker;

    private FirebaseAuth mAuth;
    private FavoriteDatabaseHelper dbHelper;
    private KeyStoreManager keystoreManager; // Nueva instancia

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_user);

        // Inicializar KeystoreManager
        keystoreManager = new KeyStoreManager();

        // Ajuste de insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Instancias
        mAuth = FirebaseAuth.getInstance();
        dbHelper = new FavoriteDatabaseHelper(this);

        // Referencias UI
        edtName       = findViewById(R.id.editTextTextName);
        edtEmail      = findViewById(R.id.editTextTextEmail);
        edtAddress    = findViewById(R.id.editTextTextAddress);
        edtPhone      = findViewById(R.id.editTextNumberPhone);
        userImageView = findViewById(R.id.imageView);
        btnDirection  = findViewById(R.id.buttonSelectDirection);
        btnImage      = findViewById(R.id.buttonSelectImage);
        btnSave       = findViewById(R.id.buttonSave);
        countryCodePicker = findViewById(R.id.countryCodePicker);

        // Restaurar prefijo país
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

        // Si viene foto de MainActivity, cargarla
        String photoUriString = getIntent().getStringExtra("EXTRA_PROFILE_PICTURE_URI");
        if (photoUriString != null && !photoUriString.isEmpty()) {
            Uri photoUri = Uri.parse(photoUriString);
            Glide.with(this)
                    .load(photoUri)
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Evita la caché en disco
                    .skipMemoryCache(true)                    // Evita la caché en memoria
                    .circleCrop()
                    .into(userImageView);
            if (photoUriString.startsWith("http")) {
                externalPhotoUrl = photoUriString; // Asignar la URL externa
            } else {
                cameraImageUri = photoUri; // Asignar la URI local
            }
        }

        // Cargar datos del usuario en la base local
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery(
                    "SELECT name, email, " + FavoriteDatabaseHelper.COL_PHONE + ", " +
                            FavoriteDatabaseHelper.COL_ADDRESS + ", " + FavoriteDatabaseHelper.COL_PHOTO_URL +
                            " FROM " + FavoriteDatabaseHelper.TABLE_USUARIOS +
                            " WHERE " + FavoriteDatabaseHelper.COL_USER_ID + " = ?",
                    new String[]{ userId }
            );

            if (cursor != null && cursor.moveToFirst()) {
                String name     = cursor.getString(0);
                String email    = cursor.getString(1);
                String encryptedPhone    = cursor.getString(2);
                String encryptedAddress  = cursor.getString(3);
                String photoUrl = cursor.getString(4);

                // Descifrar teléfono y dirección
                String phone = keystoreManager.decrypt(encryptedPhone);
                String address = keystoreManager.decrypt(encryptedAddress);

                edtName.setText(name);
                edtEmail.setText(email);
                edtPhone.setText(phone);
                edtAddress.setText(address);
                // Bloquear el email (solo lectura)
                edtEmail.setKeyListener(null);
                edtEmail.setFocusable(false);
                edtEmail.setCursorVisible(false);

                if (photoUrl != null && !photoUrl.isEmpty()) {
                    loadImageIntoView(Uri.parse(photoUrl));
                    if (photoUrl.startsWith("http")) {
                        externalPhotoUrl = photoUrl; // Asignar la URL externa
                    } else {
                        cameraImageUri = Uri.parse(photoUrl); // Asignar la URI local
                    }
                }

                cursor.close();
            }
            db.close();
        }

        // Botón para abrir la actividad de Seleccionar Dirección
        btnDirection.setOnClickListener(v -> {
            Intent intent = new Intent(this, SelectAddressActivity.class);
            startActivityForResult(intent, RC_SELECT_ADDRESS);
        });

        // Botón para seleccionar imagen (cámara, galería, URL)
        btnImage.setOnClickListener(v -> showImageOptionsDialog());

        // Botón Guardar
        btnSave.setOnClickListener(v -> {
            // Validar phone
            String phoneNumber = edtPhone.getText().toString().trim();
            String countryCode = countryCodePicker.getSelectedCountryCode();
            String selectedCountryNameCode = countryCodePicker.getSelectedCountryNameCode();
            String fullPhone = "+" + countryCode + phoneNumber;

            if (!isValidPhoneNumber(fullPhone, selectedCountryNameCode)) {
                Toast.makeText(this, "Número de teléfono inválido para el país seleccionado", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validar nombre
            String newName = edtName.getText().toString().trim();
            if (TextUtils.isEmpty(newName)) {
                Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                return;
            }

            // Obtener otros campos
            String newAddress = edtAddress.getText().toString().trim();

            // Obtener foto URL (puede ser una URI local o una URL externa)
            String photoUrl = "";
            if (cameraImageUri != null) {
                photoUrl = cameraImageUri.toString();
            } else if (!externalPhotoUrl.isEmpty()) {
                photoUrl = externalPhotoUrl;
            }

            // Cifrar teléfono y dirección
            String encryptedPhone = keystoreManager.encrypt(phoneNumber);
            String encryptedAddress = keystoreManager.encrypt(newAddress);

            if (encryptedPhone == null || encryptedAddress == null) {
                Toast.makeText(this, "Error al cifrar los datos. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Actualizar en la base de datos local
            FirebaseUser user = mAuth.getCurrentUser(); // Renombrado a 'user'
            if (user != null) {
                dbHelper.insertOrUpdateUser(
                        user.getUid(),
                        newName,
                        edtEmail.getText().toString().trim(),
                        getCurrentTimestamp(),
                        getCurrentTimestamp(),
                        encryptedPhone,      // Guardar teléfono cifrado
                        encryptedAddress,    // Guardar dirección cifrada
                        photoUrl
                );
            }

            Toast.makeText(this, "Datos guardados correctamente.", Toast.LENGTH_SHORT).show();

            // Volver a MainActivity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    //────────────────────────────────────────────────────────────────────────
    // IMAGEN: CÁMARA, GALERÍA, URL
    //────────────────────────────────────────────────────────────────────────
    private void showImageOptionsDialog() {
        String[] items = {"Cámara", "Galería", "URL externa"};

        new android.app.AlertDialog.Builder(this)
                .setTitle("Seleccionar imagen")
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0: // cámara
                            openCamera();
                            break;
                        case 1: // galería
                            openGallery();
                            break;
                        case 2: // url
                            showUrlDialog();
                            break;
                    }
                })
                .create()
                .show();
    }

    /**
     * Verifica si tenemos permiso de cámara
     * Si no -> lo pide
     * Si sí -> abre la cámara
     */
    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    RC_CAMERA_PERMISSION
            );
        } else {
            launchCameraIntent();
        }
    }

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

    private File createTempImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, RC_GALLERY);
    }

    private void showUrlDialog() {
        final EditText input = new EditText(this);
        input.setHint("https://ejemplo.com/foto.png");

        new android.app.AlertDialog.Builder(this)
                .setTitle("Introduce la URL de la imagen")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) {
                        externalPhotoUrl = url; // Almacenar la URL externa
                        loadImageIntoView(Uri.parse(url));
                    } else {
                        Toast.makeText(this, "URL vacía", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    //────────────────────────────────────────────────────────────────────────
    // onActivityResult para fotos, galería y SELECT_ADDRESS
    //────────────────────────────────────────────────────────────────────────
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Si la Activity de Seleccionar dirección devolvió RESULT_OK
        if (requestCode == RC_SELECT_ADDRESS && resultCode == RESULT_OK) {
            if (data != null) {
                String selectedAddress = data.getStringExtra("SELECTED_ADDRESS");
                if (selectedAddress != null) {
                    edtAddress.setText(selectedAddress);
                }
            }
        }

        // Cámara
        if (requestCode == RC_CAMERA && resultCode == RESULT_OK) {
            if (cameraImageUri != null) {
                loadImageIntoView(cameraImageUri);
                externalPhotoUrl = ""; // Limpiar la URL externa si se tomó una foto con cámara
            }
        }

        // Galería
        if (requestCode == RC_GALLERY && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri galleryUri = data.getData();
                loadImageIntoView(galleryUri);
                externalPhotoUrl = ""; // Limpiar la URL externa si se seleccionó una imagen de la galería
            }
        }
    }

    /**
     * Maneja la respuesta del usuario al pedir permiso de cámara
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RC_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCameraIntent();
            } else {
                Toast.makeText(this, "No se concedió permiso de cámara", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Cargar la imagen en el ImageView con Glide
     */
    private void loadImageIntoView(Uri uri) {
        Glide.with(this)
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.NONE) // Evita la caché en disco
                .skipMemoryCache(true)                    // Evita la caché en memoria
                .circleCrop()
                .into(userImageView);
    }

    /**
     * Valida el número de teléfono según el país seleccionado
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

    /**
     * Obtener el timestamp actual
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
}
