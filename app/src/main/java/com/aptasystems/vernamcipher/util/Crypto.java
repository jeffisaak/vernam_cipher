package com.aptasystems.vernamcipher.util;

import org.spongycastle.crypto.CryptoException;
import org.spongycastle.util.encoders.Hex;
import org.spongycastle.util.encoders.HexEncoder;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Provides cryptographic utility.
 */
public class Crypto {

    private static final String PROVIDER = "BC";
    private static final int IV_LENGTH = 16;
    private static final int PBE_ITERATION_COUNT = 100;
    private static final int PBE_KEY_LENGTH = 256;

    private static final String PBE_ALGORITHM = "PBEWithSHA256And256BitAES-CBC-BC";
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY_ALGORITHM = "AES";

    private Crypto() {
        // Prevents instantiation.
    }

    /**
     * Used to encrypt or decrypt.
     *
     * @param key
     * @param input Either the clear text (in which case the output is the cipher text) or the
     *              cipher text (in which case the output is the clear text.
     * @return
     */
    public static byte[] vernamCipher(byte[] key, byte[] input) {
        byte[] output = new byte[input.length];
        for (int ii = 0; ii < input.length; ii++) {
            output[ii] = (byte) (input[ii] ^ key[ii]);
        }
        return output;
    }

    /**
     * Encrypt a byte array using the specified password and salt; return the result in a byte
     * array.
     *
     * @param password
     * @param salt
     * @param cleartext
     * @return
     * @throws CryptoException
     */
    public static byte[] encryptToByteArray(String password, String salt, byte[] cleartext) throws CryptoException {
        SecretKey secretKey = Crypto.buildSecretKey(password, salt);
        return encryptToByteArray(secretKey, cleartext);
    }

    /**
     * Decrypt a byte array using the specified password and salt; return the result in a byte
     * array.
     *
     * @param password
     * @param salt
     * @param ciphertext
     * @return
     * @throws CryptoException
     */
    public static byte[] decryptToByteArray(String password, String salt, byte[] ciphertext) throws CryptoException {
        SecretKey secretKey = Crypto.buildSecretKey(password, salt);
        return decryptToByteArray(secretKey, ciphertext);
    }

    /**
     * Encrypt a byte array using the specified secret key; return the result in a byte array.
     *
     * @param secret
     * @param cleartext
     * @return
     * @throws CryptoException
     */
    private static byte[] encryptToByteArray(SecretKey secret, byte[] cleartext) throws CryptoException {
        try {
            byte[] iv = generateIv();
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            Cipher encryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM, PROVIDER);
            encryptionCipher.init(Cipher.ENCRYPT_MODE, secret, ivParameterSpec);
            byte[] ciphertext = encryptionCipher.doFinal(cleartext);

            ByteArrayOutputStream ciphertextOutputStream = new ByteArrayOutputStream();
            ciphertextOutputStream.write(iv);
            ciphertextOutputStream.write(ciphertext);
            ciphertextOutputStream.flush();
            ciphertextOutputStream.close();
            return ciphertextOutputStream.toByteArray();

        } catch (Exception e) {
            throw new CryptoException("Encryption error", e);
        }
    }

    /**
     * Decrypt a byte array using the specified secret key; return the result in a byte array.
     *
     * @param secret
     * @param ciphertext
     * @return
     * @throws CryptoException
     */
    private static byte[] decryptToByteArray(SecretKey secret, byte[] ciphertext) throws CryptoException {
        try {
            Cipher decryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM, PROVIDER);

            ByteArrayOutputStream ivStream = new ByteArrayOutputStream();
            ivStream.write(ciphertext, 0, IV_LENGTH);

            ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
            dataStream.write(ciphertext, IV_LENGTH, ciphertext.length - IV_LENGTH);

            byte[] iv = ivStream.toByteArray();
            byte[] data = dataStream.toByteArray();

            IvParameterSpec ivspec = new IvParameterSpec(iv);
            decryptionCipher.init(Cipher.DECRYPT_MODE, secret, ivspec);
            byte[] cleartext = decryptionCipher.doFinal(data);
            return cleartext;
        } catch (Exception e) {
            throw new CryptoException("Decryption error", e);
        }
    }

    /**
     * Build a secret key from a password and salt.
     * @param password
     * @param salt
     * @return
     * @throws CryptoException
     */
    private static SecretKey buildSecretKey(String password, String salt) throws CryptoException {
        try {
            PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), Hex.decode(salt), PBE_ITERATION_COUNT, PBE_KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE_ALGORITHM, PROVIDER);
            SecretKey tmp = factory.generateSecret(pbeKeySpec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), SECRET_KEY_ALGORITHM);
            return secret;
        } catch (Exception e) {
            throw new CryptoException("Secret key error", e);
        }
    }

    private static byte[] generateIv() throws NoSuchAlgorithmException, NoSuchProviderException {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        return iv;
    }

}