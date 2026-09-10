package com.digitallock.data;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hasheo del PIN con PBKDF2-HmacSHA256. Nunca se guarda el PIN en plano; el hash
 * defiende contra lectura offline del {@code level.dat}. La defensa en runtime es
 * el daño por intento fallido.
 */
public final class PinHasher {
    private PinHasher() {}

    private static final SecureRandom RNG = new SecureRandom();
    private static final int SALT_BYTES = 16;
    private static final int ITERATIONS = 10_000;
    private static final int KEY_BITS = 256; // 32 bytes
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    /** Salt aleatorio de 16 bytes. */
    public static byte[] newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RNG.nextBytes(salt);
        return salt;
    }

    /** Hash de 32 bytes del PIN con el salt dado. */
    public static byte[] hash(String pin, byte[] salt) {
        try {
            KeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo hashear el PIN", e);
        }
    }

    /** Compara en tiempo constante el PIN contra el hash guardado. */
    public static boolean verify(String pin, byte[] salt, byte[] expectedHash) {
        return MessageDigest.isEqual(hash(pin, salt), expectedHash);
    }
}
