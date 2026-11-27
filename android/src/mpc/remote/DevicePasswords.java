package mpc.remote;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class DevicePasswords {
    private static final String KEY_ALIAS = "mpcr-screen-saved-device-passwords";
    private static final int IV_BYTES = 12;
    static final String PREFERENCE_PREFIX = "savedDevicePassword:";

    private DevicePasswords() {}

    private static SecretKey key(boolean create) throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        SecretKey key = (SecretKey) store.getKey(KEY_ALIAS, null);
        if (key != null || !create) return key;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build());
        return generator.generateKey();
    }

    static String encrypt(String address, String password) throws GeneralSecurityException, IOException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key(true));
        cipher.updateAAD(address.getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();
        if (iv.length != IV_BYTES) throw new GeneralSecurityException("Unexpected IV length");
        byte[] clear = password.getBytes(StandardCharsets.UTF_8);
        try {
            byte[] encrypted = cipher.doFinal(clear);
            byte[] stored = Arrays.copyOf(iv, IV_BYTES + encrypted.length);
            System.arraycopy(encrypted, 0, stored, IV_BYTES, encrypted.length);
            return Base64.encodeToString(stored, Base64.NO_WRAP);
        } finally {
            Arrays.fill(clear, (byte) 0);
        }
    }

    static String decrypt(String address, String encoded) throws GeneralSecurityException, IOException {
        SecretKey key = key(false);
        if (key == null) return null;
        byte[] stored = Base64.decode(encoded, Base64.NO_WRAP);
        if (stored.length < IV_BYTES + 16) return null;
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, stored, 0, IV_BYTES));
        cipher.updateAAD(address.getBytes(StandardCharsets.UTF_8));
        byte[] clear = cipher.doFinal(stored, IV_BYTES, stored.length - IV_BYTES);
        try {
            return new String(clear, StandardCharsets.UTF_8);
        } finally {
            Arrays.fill(clear, (byte) 0);
        }
    }
}
