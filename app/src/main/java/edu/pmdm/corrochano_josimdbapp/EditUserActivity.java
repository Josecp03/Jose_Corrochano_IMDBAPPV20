package edu.pmdm.corrochano_josimdbapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
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
import com.facebook.AccessToken;
import com.facebook.Profile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
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
import edu.pmdm.corrochano_josimdbapp.sync.UsersSync;

public class EditUserActivity extends AppCompatActivity {

    private static final int RC_CAMERA = 100;
    private static final int RC_GALLERY = 101;
    private static final int RC_CAMERA_PERMISSION = 200;
    private static final int RC_SELECT_ADDRESS = 300;
    private static final int RC_STORAGE_PERMISSION = 500;

    private Uri cameraImageUri;
    private String externalPhotoUrl = "";
    private EditText edtName, edtEmail, edtAddress, edtPhone;
    private ImageView userImageView;
    private Button btnDirection, btnImage, btnSave;
    private CountryCodePicker countryCodePicker;
    private FirebaseAuth mAuth;
    private FavoriteDatabaseHelper dbHelper;
    private KeyStoreManager keystoreManager;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_user);

        // Inicializamos el KeyStoreManager (ahora con clave estática compartida)
        keystoreManager = new KeyStoreManager();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new FavoriteDatabaseHelper(this);
        edtName = findViewById(R.id.editTextTextName);
        edtEmail = findViewById(R.id.editTextTextEmail);
        edtAddress = findViewById(R.id.editTextTextAddress);
        edtPhone = findViewById(R.id.editTextNumberPhone);
        userImageView = findViewById(R.id.imageView);
        btnDirection = findViewById(R.id.buttonSelectDirection);
        btnImage = findViewById(R.id.buttonSelectImage);
        btnSave = findViewById(R.id.buttonSave);
        countryCodePicker = findViewById(R.id.countryCodePicker);

        // Restauramos el prefijo del país
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int lastCode = prefs.getInt("LAST_COUNTRY_CODE", -1);
        if (lastCode != -1) {
            countryCodePicker.setCountryForPhoneCode(lastCode);
        } else {
            countryCodePicker.setCountryForPhoneCode(Integer.parseInt(countryCodePicker.getDefaultCountryCode()));
        }
        countryCodePicker.setOnCountryChangeListener(() -> {
            int selectedCode = countryCodePicker.getSelectedCountryCodeAsInt();
            prefs.edit().putInt("LAST_COUNTRY_CODE", selectedCode).apply();
        });

        // Si viene foto desde MainActivity, la cargamos
        String photoUriString = getIntent().getStringExtra("EXTRA_PROFILE_PICTURE_URI");
        if (photoUriString != null && !photoUriString.isEmpty()) {
            Uri photoUri = Uri.parse(photoUriString);
            Glide.with(this)
                    .load(photoUri)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .circleCrop()
                    .into(userImageView);
            if (photoUriString.startsWith("http")) {
                externalPhotoUrl = photoUriString;
            } else {
                cameraImageUri = photoUri;
            }
        }

        // Cargamos los datos del usuario (ahora se espera que tanto en la nube como en local estén encriptados)
        loadUserData();

        // Botón para abrir la actividad de selección de dirección
        btnDirection.setOnClickListener(v -> abrirSelectAddressActivity());
        // Botón para seleccionar imagen (cámara, galería, URL externa)
        btnImage.setOnClickListener(v -> showImageOptionsDialog());
        // Botón Guardar: actualiza la base local y la nube con los datos encriptados
        btnSave.setOnClickListener(v -> {
            String phoneNumber = edtPhone.getText().toString().trim();
            String countryCode = countryCodePicker.getSelectedCountryCode();
            String selectedCountryNameCode = countryCodePicker.getSelectedCountryNameCode();
            String fullPhone = "+" + countryCode + phoneNumber;
            if (!isValidPhoneNumber(fullPhone, selectedCountryNameCode)) {
                Toast.makeText(this, "Número de teléfono inválido para el país seleccionado", Toast.LENGTH_SHORT).show();
                return;
            }
            String newName = edtName.getText().toString().trim();
            if (TextUtils.isEmpty(newName)) {
                Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                return;
            }
            String newAddress = edtAddress.getText().toString().trim();
            String photoUrl = "";
            if (cameraImageUri != null) {
                photoUrl = cameraImageUri.toString();
            } else if (!externalPhotoUrl.isEmpty()) {
                photoUrl = externalPhotoUrl;
            }
            // Encriptamos para almacenamiento tanto local como en la nube
            String encryptedPhone = keystoreManager.encrypt(phoneNumber);
            String encryptedAddress = keystoreManager.encrypt(newAddress);
            if (encryptedPhone == null || encryptedAddress == null) {
                Toast.makeText(this, "Error al cifrar los datos. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                // Guardamos en la DB local con datos encriptados
                dbHelper.insertOrUpdateUser(
                        user.getUid(),
                        newName,
                        edtEmail.getText().toString().trim(),
                        getCurrentTimestamp(),
                        getCurrentTimestamp(),
                        encryptedPhone,
                        encryptedAddress,
                        photoUrl
                );
                // Actualizamos la nube con los mismos datos encriptados
                UsersSync.updateUser(this, user.getUid(),
                        newName,
                        edtEmail.getText().toString().trim(),
                        encryptedPhone,
                        encryptedAddress,
                        photoUrl);
            }
            Toast.makeText(this, "Datos guardados correctamente.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    // Abre la actividad de selección de dirección
    private void abrirSelectAddressActivity() {
        Intent intent = new Intent(this, SelectAddressActivity.class);
        startActivityForResult(intent, RC_SELECT_ADDRESS);
    }

    // Muestra las opciones para seleccionar imagen
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

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, RC_CAMERA_PERMISSION);
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
        cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, RC_STORAGE_PERMISSION);
            } else {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, RC_GALLERY);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, RC_STORAGE_PERMISSION);
            } else {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, RC_GALLERY);
            }
        }
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
                        externalPhotoUrl = url;
                        // Importante: si se escoge una URL, se borra la imagen tomada con la cámara
                        cameraImageUri = null;
                        loadImageIntoView(Uri.parse(url));
                    } else {
                        Toast.makeText(this, "URL vacía", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SELECT_ADDRESS && resultCode == RESULT_OK) {
            if (data != null) {
                String selectedAddress = data.getStringExtra("SELECTED_ADDRESS");
                if (selectedAddress != null) {
                    edtAddress.setText(selectedAddress);
                }
            }
        }
        if (requestCode == RC_CAMERA && resultCode == RESULT_OK) {
            if (cameraImageUri != null) {
                loadImageIntoView(cameraImageUri);
                externalPhotoUrl = "";
            }
        }
        if (requestCode == RC_GALLERY && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri galleryUri = data.getData();
                loadImageIntoView(galleryUri);
                externalPhotoUrl = "";
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCameraIntent();
            } else {
                Toast.makeText(this, "No se concedió permiso de cámara", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == RC_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Permiso de acceso a archivos no concedido", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadImageIntoView(Uri uri) {
        Glide.with(this)
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .circleCrop()
                .into(userImageView);
    }

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

    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void navegarAlLogin(){
        Intent intent = new Intent(EditUserActivity.this, LogInActivity.class);
        startActivity(intent);
        finish();
    }

    // Carga los datos del usuario desde Firestore (se esperan datos encriptados) y los muestra desencriptados.
    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            navegarAlLogin();
            return;
        }
        String userId = currentUser.getUid();
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Datos obtenidos desde Firestore (almacenados encriptados)
                        String cloudName = documentSnapshot.getString("name");
                        String cloudEmail = documentSnapshot.getString("email");
                        String cloudPhotoUrl = documentSnapshot.getString("photo_url");
                        String cloudPhone = documentSnapshot.getString("phone");
                        String cloudAddress = documentSnapshot.getString("address");

                        if (cloudName != null && !cloudName.isEmpty()) {
                            edtName.setText(cloudName);
                        }
                        if (cloudEmail != null && !cloudEmail.isEmpty()) {
                            edtEmail.setText(cloudEmail);
                        }
                        // Desencriptamos para mostrar los datos legibles
                        if (cloudPhone != null && !cloudPhone.isEmpty()) {
                            String decryptedPhone = keystoreManager.decrypt(cloudPhone);
                            edtPhone.setText(decryptedPhone);
                        }
                        if (cloudAddress != null && !cloudAddress.isEmpty()) {
                            String decryptedAddress = keystoreManager.decrypt(cloudAddress);
                            edtAddress.setText(decryptedAddress);
                        }
                        if (cloudPhotoUrl != null && !cloudPhotoUrl.isEmpty()) {
                            Glide.with(EditUserActivity.this)
                                    .load(cloudPhotoUrl)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .skipMemoryCache(true)
                                    .circleCrop()
                                    .into(userImageView);
                        }
                        // Bloquear el email (solo lectura)
                        edtEmail.setKeyListener(null);
                        edtEmail.setFocusable(false);
                        edtEmail.setCursorVisible(false);

                        // Actualizamos la base local con los datos de la nube (volvemos a almacenarlos encriptados)
                        updateLocalUserData(userId, cloudName, cloudEmail, cloudPhone, cloudAddress, cloudPhotoUrl);
                    } else {
                        // Si Firestore no tiene los datos, se recurre a la DB local
                        loadUserDataFromLocal();
                    }
                })
                .addOnFailureListener(e -> {
                    // En caso de error, se carga la información local
                    loadUserDataFromLocal();
                });
    }

    /**
     * Actualiza la base de datos local con los datos obtenidos de la nube.
     * Los datos (phone y address) se esperan encriptados, por lo que se almacenan directamente.
     */
    private void updateLocalUserData(String userId, String name, String email, String phone, String address, String photoUrl) {
        try {
            FavoriteDatabaseHelper dbHelper = new FavoriteDatabaseHelper(this);
            dbHelper.insertOrUpdateUser(
                    userId,
                    name,
                    email,
                    DateFormat.format("yyyy-MM-dd HH:mm:ss", new Date()).toString(),
                    null,
                    phone,
                    address,
                    photoUrl
            );
        } catch (Exception ex) {
            Log.e("EditUserActivity", "Error actualizando la DB local con datos de la nube: " + ex.getMessage());
        }
    }

    /**
     * Carga la información del usuario desde la base de datos local (fallback) y la muestra desencriptada.
     */
    private void loadUserDataFromLocal() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            navegarAlLogin();
            return;
        }
        String nombre = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Usuario";
        String email = (currentUser.getEmail() != null) ? currentUser.getEmail() : "Sin email";
        Uri fotoUri = currentUser.getPhotoUrl();
        // Si se usa Facebook, se obtiene la información de perfil
        AccessToken fbAccessToken = AccessToken.getCurrentAccessToken();
        boolean isFacebookLoggedIn = (fbAccessToken != null && !fbAccessToken.isExpired());
        if (isFacebookLoggedIn) {
            Profile profile = Profile.getCurrentProfile();
            if (profile != null) {
                String facebookNombre = profile.getFirstName() + " " + profile.getLastName();
                facebookNombre = facebookNombre.trim().isEmpty() ? "Usuario de Facebook" : facebookNombre;
                nombre = facebookNombre;
                email = "Conectado con Facebook";
                Uri facebookFoto = profile.getProfilePictureUri(300, 300);
                if (facebookFoto != null) {
                    fotoUri = facebookFoto;
                }
            }
        }
        FavoriteDatabaseHelper dbHelper = new FavoriteDatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + FavoriteDatabaseHelper.COL_NAME + ", " +
                        FavoriteDatabaseHelper.COL_PHONE + ", " +
                        FavoriteDatabaseHelper.COL_ADDRESS + ", " +
                        FavoriteDatabaseHelper.COL_PHOTO_URL +
                        " FROM " + FavoriteDatabaseHelper.TABLE_USUARIOS +
                        " WHERE " + FavoriteDatabaseHelper.COL_USER_ID + " = ?",
                new String[]{currentUser.getUid()}
        );
        if (cursor != null && cursor.moveToFirst()) {
            String localName = cursor.getString(0);
            String encryptedPhone = cursor.getString(1);
            String encryptedAddress = cursor.getString(2);
            String localPhotoUrl = cursor.getString(3);
            if (localName != null && !localName.trim().isEmpty()) {
                nombre = localName;
            }
            if (localPhotoUrl != null && !localPhotoUrl.trim().isEmpty()) {
                fotoUri = Uri.parse(localPhotoUrl);
            }
            // Desencriptamos para mostrar los datos en formato legible
            String decryptedPhone = keystoreManager.decrypt(encryptedPhone);
            String decryptedAddress = keystoreManager.decrypt(encryptedAddress);
            edtName.setText(nombre);
            edtEmail.setText(email);
            edtPhone.setText(decryptedPhone);
            edtAddress.setText(decryptedAddress);
            cursor.close();
        }
        db.close();
        if (fotoUri != null && !fotoUri.toString().isEmpty()) {
            Glide.with(this)
                    .load(fotoUri)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .circleCrop()
                    .into(userImageView);
        }
        // Bloquear el email
        edtEmail.setKeyListener(null);
        edtEmail.setFocusable(false);
        edtEmail.setCursorVisible(false);
    }
}
