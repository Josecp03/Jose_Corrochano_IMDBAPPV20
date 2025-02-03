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

import edu.pmdm.corrochano_josimdbapp.database.DatabaseHelper;
import edu.pmdm.corrochano_josimdbapp.sync.UsersSync;

public class EditUserActivity extends AppCompatActivity {

    // Atributos
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
    private DatabaseHelper dbHelper;
    private KeyStoreManager keystoreManager;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_user);

        // Inicializar el KeyStoreManager
        keystoreManager = new KeyStoreManager();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar autenticación y base de datos
        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper(this);

        // Asignar las vistas de la UI
        edtName = findViewById(R.id.editTextTextName);
        edtEmail = findViewById(R.id.editTextTextEmail);
        edtAddress = findViewById(R.id.editTextTextAddress);
        edtPhone = findViewById(R.id.editTextNumberPhone);
        userImageView = findViewById(R.id.imageView);
        btnDirection = findViewById(R.id.buttonSelectDirection);
        btnImage = findViewById(R.id.buttonSelectImage);
        btnSave = findViewById(R.id.buttonSave);
        countryCodePicker = findViewById(R.id.countryCodePicker);

        // Restaurar el prefijo del país desde SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int lastCode = prefs.getInt("LAST_COUNTRY_CODE", -1);
        if (lastCode != -1) {
            countryCodePicker.setCountryForPhoneCode(lastCode);
        } else {
            countryCodePicker.setCountryForPhoneCode(Integer.parseInt(countryCodePicker.getDefaultCountryCode()));
        }

        // Guardar el país seleccionado al cambiarlo
        countryCodePicker.setOnCountryChangeListener(() -> {
            int selectedCode = countryCodePicker.getSelectedCountryCodeAsInt();
            prefs.edit().putInt("LAST_COUNTRY_CODE", selectedCode).apply();
        });

        // Si se recibe foto desde MainActivity, cargarla
        String photoUriString = getIntent().getStringExtra("EXTRA_PROFILE_PICTURE_URI");
        if (photoUriString != null && !photoUriString.isEmpty()) {
            Uri photoUri = Uri.parse(photoUriString);
            Glide.with(this)
                    .load(photoUri)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .circleCrop()
                    .into(userImageView);

            // Si la URL es externa se guarda en externalPhotoUrl, de lo contrario se trata como foto de cámara
            if (photoUriString.startsWith("http")) {
                externalPhotoUrl = photoUriString;
            } else {
                cameraImageUri = photoUri;
            }

        }

        // Cargar los datos del usuario
        loadUserData();

        // Configurar botón para abrir la actividad de selección de dirección
        btnDirection.setOnClickListener(v -> abrirSelectAddressActivity());

        // Configurar botón para seleccionar imagen
        btnImage.setOnClickListener(v -> showImageOptionsDialog());

        // Botón Guardar que actualiza la base local y la nube con los datos encriptados
        btnSave.setOnClickListener(v -> {

            String phoneNumber = edtPhone.getText().toString().trim();
            String countryCode = countryCodePicker.getSelectedCountryCode();
            String selectedCountryNameCode = countryCodePicker.getSelectedCountryNameCode();
            String fullPhone = "+" + countryCode + phoneNumber;

            // Validar número de teléfono según el país seleccionado
            if (!edtPhone.getText().toString().isEmpty() && !isValidPhoneNumber(fullPhone, selectedCountryNameCode)) {
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

            // Determinar qué foto utilizar
            if (cameraImageUri != null) {
                photoUrl = cameraImageUri.toString();
            } else if (!externalPhotoUrl.isEmpty()) {
                photoUrl = externalPhotoUrl;
            }

            // Encriptar datos para almacenamiento local y en la nube
            String encryptedPhone = keystoreManager.encrypt(phoneNumber);
            String encryptedAddress = keystoreManager.encrypt(newAddress);
            if (encryptedPhone == null || encryptedAddress == null) {
                Toast.makeText(this, "Error al cifrar los datos. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Guardar datos en la base de datos local con datos encriptados
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
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

                // Actualizar la nube con los mismos datos encriptados
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

    // Abre la actividad para seleccionar dirección
    private void abrirSelectAddressActivity() {
        Intent intent = new Intent(this, SelectAddressActivity.class);
        startActivityForResult(intent, RC_SELECT_ADDRESS);
    }

    // Muestra diálogo para seleccionar opción de imagen
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

    // Inicia el proceso para abrir la cámara
    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, RC_CAMERA_PERMISSION);
        } else {
            launchCameraIntent();
        }
    }

    // Lanza el intent para tomar una foto con la cámara
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

        // Obtener URI para el archivo temporal usando FileProvider
        cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
        cameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, cameraImageUri);
        startActivityForResult(cameraIntent, RC_CAMERA);
    }

    // Crea un archivo temporal para almacenar la imagen capturada
    private File createTempImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    // Abre la galería para seleccionar una imagen
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

    // Muestra diálogo para introducir URL externa de imagen
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
                        // Si se usa URL externa, se borra la imagen tomada con la cámara
                        cameraImageUri = null;
                        loadImageIntoView(Uri.parse(url));
                    } else {
                        Toast.makeText(this, "URL vacía", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Maneja los resultados de las actividades iniciadas
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Resultado de la actividad de selección de dirección
        if (requestCode == RC_SELECT_ADDRESS && resultCode == RESULT_OK) {
            if (data != null) {
                String selectedAddress = data.getStringExtra("SELECTED_ADDRESS");
                if (selectedAddress != null) {
                    edtAddress.setText(selectedAddress);
                }
            }
        }

        // Resultado de la cámara
        if (requestCode == RC_CAMERA && resultCode == RESULT_OK) {
            if (cameraImageUri != null) {
                loadImageIntoView(cameraImageUri);
                externalPhotoUrl = "";
            }
        }

        // Resultado de la galería
        if (requestCode == RC_GALLERY && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri galleryUri = data.getData();
                loadImageIntoView(galleryUri);
                externalPhotoUrl = "";
            }
        }

    }

    // Maneja el resultado de la solicitud de permisos
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

    // Carga la imagen en la vista usando Glide
    private void loadImageIntoView(Uri uri) {
        Glide.with(this)
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .circleCrop()
                .into(userImageView);
    }

    // Valida el número de teléfono utilizando la librería libphonenumber
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

    // Obtiene la fecha y hora actual en formato "yyyy-MM-dd HH:mm:ss"
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Navega a la actividad de Login y finaliza la actual
    private void navegarAlLogin(){
        Intent intent = new Intent(EditUserActivity.this, LogInActivity.class);
        startActivity(intent);
        finish();
    }

    // Carga los datos del usuario desde Firestore y, si es posible, actualiza la base de datos local
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
                        // Recuperar los datos encriptados de la nube
                        String cloudName = documentSnapshot.getString("name");
                        String cloudEmail = documentSnapshot.getString("email");
                        String cloudPhotoUrl = documentSnapshot.getString("photo_url");
                        String cloudPhone = documentSnapshot.getString("phone");
                        String cloudAddress = documentSnapshot.getString("address");

                        // Asignar datos a las vistas, si existen
                        if (cloudName != null && !cloudName.isEmpty()) {
                            edtName.setText(cloudName);
                        }
                        if (cloudEmail != null && !cloudEmail.isEmpty()) {
                            edtEmail.setText(cloudEmail);
                        }
                        // Desencriptar y mostrar el teléfono
                        if (cloudPhone != null && !cloudPhone.isEmpty()) {
                            String decryptedPhone = keystoreManager.decrypt(cloudPhone);
                            edtPhone.setText(decryptedPhone);
                        }
                        // Desencriptar y mostrar la dirección
                        if (cloudAddress != null && !cloudAddress.isEmpty()) {
                            String decryptedAddress = keystoreManager.decrypt(cloudAddress);
                            edtAddress.setText(decryptedAddress);
                        }
                        // Cargar imagen si existe
                        if (cloudPhotoUrl != null && !cloudPhotoUrl.isEmpty()) {
                            Glide.with(EditUserActivity.this)
                                    .load(cloudPhotoUrl)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .skipMemoryCache(true)
                                    .circleCrop()
                                    .into(userImageView);
                        }
                        // Bloquear edición del email
                        edtEmail.setKeyListener(null);
                        edtEmail.setFocusable(false);
                        edtEmail.setCursorVisible(false);

                        // Actualizar la base local con los datos de la nube (almacenándolos encriptados)
                        updateLocalUserData(userId, cloudName, cloudEmail, cloudPhone, cloudAddress, cloudPhotoUrl);

                    } else {

                        // Si no hay datos en la nube, cargar datos desde la base local
                        loadUserDataFromLocal();

                    }
                })

                .addOnFailureListener(e -> {
                    // Si ocurre un error, cargar datos desde la base local
                    loadUserDataFromLocal();
                });

    }

    // Actualiza la base de datos local con los datos obtenidos de la nube
    private void updateLocalUserData(String userId, String name, String email, String phone, String address, String photoUrl) {
        try {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
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
            ex.printStackTrace();
        }
    }

    // Carga los datos del usuario desde la base de datos local y los muestra desencriptados
    private void loadUserDataFromLocal() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            navegarAlLogin();
            return;
        }

        String nombre;
        if (currentUser.getDisplayName() != null) {
            nombre = currentUser.getDisplayName();
        } else {
            nombre = "Usuario";
        }

        String email;
        if (currentUser.getEmail() != null) {
            email = currentUser.getEmail();
        } else {
            email = "Sin email";
        }

        Uri fotoUri = currentUser.getPhotoUrl();

        // Si el usuario inició sesión con Facebook, actualizar datos de perfil
        AccessToken fbAccessToken = AccessToken.getCurrentAccessToken();
        boolean isFacebookLoggedIn = (fbAccessToken != null && !fbAccessToken.isExpired());

        if (isFacebookLoggedIn) {

            Profile profile = Profile.getCurrentProfile();
            if (profile != null) {
                String facebookNombre = (profile.getFirstName() + " " + profile.getLastName()).trim();
                facebookNombre = facebookNombre.isEmpty() ? "Usuario de Facebook" : facebookNombre;
                nombre = facebookNombre;
                email = "Conectado con Facebook";
                Uri facebookFoto = profile.getProfilePictureUri(300, 300);
                if (facebookFoto != null) {
                    fotoUri = facebookFoto;
                }
            }

        }

        // Consultar la base de datos local para obtener datos adicionales
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + DatabaseHelper.COL_NAME + ", " +
                        DatabaseHelper.COL_PHONE + ", " +
                        DatabaseHelper.COL_ADDRESS + ", " +
                        DatabaseHelper.COL_PHOTO_URL +
                        " FROM " + DatabaseHelper.TABLE_USUARIOS +
                        " WHERE " + DatabaseHelper.COL_USER_ID + " = ?",
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

            // Desencriptar datos para mostrarlos en formato legible
            String decryptedPhone = keystoreManager.decrypt(encryptedPhone);
            String decryptedAddress = keystoreManager.decrypt(encryptedAddress);
            edtName.setText(nombre);
            edtEmail.setText(email);
            edtPhone.setText(decryptedPhone);
            edtAddress.setText(decryptedAddress);
            cursor.close();
        }

        db.close();

        // Cargar la imagen en la vista, si existe
        if (fotoUri != null && !fotoUri.toString().isEmpty()) {
            Glide.with(this)
                    .load(fotoUri)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .circleCrop()
                    .into(userImageView);
        }

        // Bloquear la edición del email
        edtEmail.setKeyListener(null);
        edtEmail.setFocusable(false);
        edtEmail.setCursorVisible(false);

    }
}
