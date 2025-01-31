// Archivo: KeystoreManager.java
package edu.pmdm.corrochano_josimdbapp;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.ByteBuffer;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class KeyStoreManager {
    private static final String TAG = "KeystoreManager";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "MyAppKeyAlias";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12; // Tamaño estándar para GCM
    private static final int TAG_LENGTH = 128; // Tamaño del tag para GCM

    private SecretKey secretKey;

    public KeyStoreManager() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);

            // Verificar si la clave ya existe
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                generateKey();
            }

            // Obtener la clave
            KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
            if (secretKeyEntry != null) {
                secretKey = secretKeyEntry.getSecretKey();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing KeystoreManager", e);
        }
    }

    /**
     * Genera una clave AES y la almacena en el Android Keystore.
     */
    private void generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
            )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build();
            keyGenerator.init(keyGenParameterSpec);
            keyGenerator.generateKey();
            Log.d(TAG, "AES key generated and stored in Keystore.");
        } catch (Exception e) {
            Log.e(TAG, "Error generating AES key", e);
        }
    }

    /**
     * Cifra el texto plano y devuelve una cadena Base64 que contiene el IV y el texto cifrado.
     *
     * @param plainText Texto plano a cifrar.
     * @return Texto cifrado en Base64 con IV.
     */
    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = cipher.getIV();
            byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));

            // Concatenar IV y cipherText
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);
            byte[] cipherMessage = byteBuffer.array();

            // Codificar en Base64
            return Base64.encodeToString(cipherMessage, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting data", e);
            return null;
        }
    }

    /**
     * Descifra la cadena Base64 que contiene el IV y el texto cifrado, devolviendo el texto plano.
     *
     * @param cipherText Texto cifrado en Base64 con IV.
     * @return Texto plano descifrado.
     */
    public String decrypt(String cipherText) {
        try {
            byte[] cipherMessage = Base64.decode(cipherText, Base64.DEFAULT);

            // Extraer IV y cipherText
            ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);
            byte[] iv = new byte[IV_SIZE];
            byteBuffer.get(iv);
            byte[] actualCipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(actualCipherText);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            byte[] decryptedBytes = cipher.doFinal(actualCipherText);

            return new String(decryptedBytes, "UTF-8");
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting data", e);
            return null;
        }
    }
}
