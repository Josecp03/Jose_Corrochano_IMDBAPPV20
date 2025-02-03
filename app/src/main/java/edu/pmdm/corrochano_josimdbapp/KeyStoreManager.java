package edu.pmdm.corrochano_josimdbapp;

import android.util.Base64;
import android.util.Log;

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class KeyStoreManager {

    // Atributos
    private static final String TAG = "KeyStoreManager";
    private static final String SECRET_KEY = "ThisIsMyStaticKeyForDemoPurposesOnly";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12;
    private static final int TAG_LENGTH = 128;

    private SecretKeySpec secretKey;

    public KeyStoreManager() {
        try {
            // Derivar una clave AES a partir del hash SHA-256 del string secreto
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(SECRET_KEY.getBytes("UTF-8"));
            secretKey = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String encrypt(String plainText) {
        try {

            // Obtener la instancia del cipher con la transformación indicada
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            // Crear un vector de inicialización (IV) aleatorio de tamaño IV_SIZE
            byte[] iv = new byte[IV_SIZE];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);

            // Configurar el parámetro GCM con la longitud de etiqueta y el IV
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            // Encriptar el texto plano y obtener el texto cifrado
            byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));

            // Combinar el IV y el texto cifrado en un solo arreglo
            int combinedLength = iv.length + cipherText.length;
            byte[] combined = new byte[combinedLength];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            // Codificar el resultado en Base64 para su representación como String
            return Base64.encodeToString(combined, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String decrypt(String cipherText) {

        try {

            // Decodificar el texto cifrado de Base64 a bytes
            byte[] combined = Base64.decode(cipherText, Base64.DEFAULT);

            // Extraer el IV de los primeros IV_SIZE bytes
            byte[] iv = new byte[IV_SIZE];
            System.arraycopy(combined, 0, iv, 0, IV_SIZE);

            // Calcular la longitud del texto cifrado real
            int cipherTextLength = combined.length - IV_SIZE;
            byte[] actualCipherText = new byte[cipherTextLength];
            System.arraycopy(combined, IV_SIZE, actualCipherText, 0, cipherTextLength);

            // Obtener la instancia del cipher con la transformación indicada
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            // Desencriptar y obtener el texto plano en bytes
            byte[] decryptedBytes = cipher.doFinal(actualCipherText);
            return new String(decryptedBytes, "UTF-8");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
