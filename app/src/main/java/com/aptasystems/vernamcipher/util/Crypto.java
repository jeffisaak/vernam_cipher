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

public class Crypto {

    public static final String PROVIDER = "BC";
    public static final int SALT_LENGTH = 9;
    public static final int IV_LENGTH = 16;
    public static final int PBE_ITERATION_COUNT = 100;

    private static final String HASH_ALGORITHM = "SHA-512";
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

    public static byte[] encryptToByteArray(SecretKey secret, byte[] cleartext) throws CryptoException {
        try {

            byte[] iv = generateIv();
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            Cipher encryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM, PROVIDER);
            encryptionCipher.init(Cipher.ENCRYPT_MODE, secret, ivspec);
            byte[] encryptedText = encryptionCipher.doFinal(cleartext);

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            outStream.write(iv);
            outStream.write(encryptedText);
            outStream.flush();
            outStream.close();
            return outStream.toByteArray();

        } catch (Exception e) {
            throw new CryptoException("Unable to encrypt", e);
        }
    }

    public static String encrypt(SecretKey secret, byte[] cleartext) throws CryptoException {
        try {

            byte[] iv = generateIv();
            String ivHex = Hex.toHexString(iv);
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            Cipher encryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM, PROVIDER);
            encryptionCipher.init(Cipher.ENCRYPT_MODE, secret, ivspec);
            byte[] encryptedText = encryptionCipher.doFinal(cleartext);
            String encryptedHex = Hex.toHexString(encryptedText);

            return ivHex + encryptedHex;

        } catch (Exception e) {
            throw new CryptoException("Unable to encrypt", e);
        }
    }

    public static String encrypt(SecretKey secret, String cleartext) throws CryptoException {
        try {
            return encrypt(secret, cleartext.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new CryptoException("Unable to encrypt", e);
        }
    }

    public static byte[] decryptToByteArray(SecretKey secret, byte[] encrypted) throws CryptoException {
        try {
            Cipher decryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM, PROVIDER);

            ByteArrayOutputStream ivStream = new ByteArrayOutputStream();
            ivStream.write(encrypted, 0, IV_LENGTH);

            ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
            dataStream.write(encrypted, IV_LENGTH, encrypted.length - IV_LENGTH);

            byte[] iv = ivStream.toByteArray();
            byte[] data = dataStream.toByteArray();

            IvParameterSpec ivspec = new IvParameterSpec(iv);
            decryptionCipher.init(Cipher.DECRYPT_MODE, secret, ivspec);
            byte[] decrypted = decryptionCipher.doFinal(data);
            return decrypted;
        } catch (Exception e) {
            throw new CryptoException("Unable to decrypt", e);
        }

    }

    public static String decrypt(SecretKey secret, String encrypted) throws CryptoException {
        try {
            Cipher decryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM, PROVIDER);
            String ivHex = encrypted.substring(0, IV_LENGTH * 2);
            String encryptedHex = encrypted.substring(IV_LENGTH * 2);
            IvParameterSpec ivspec = new IvParameterSpec(Hex.decode(ivHex));
            decryptionCipher.init(Cipher.DECRYPT_MODE, secret, ivspec);
            byte[] decryptedText = decryptionCipher.doFinal(Hex.decode(encryptedHex));
            String decrypted = new String(decryptedText, "UTF-8");
            return decrypted;
        } catch (Exception e) {
            throw new CryptoException("Unable to decrypt", e);
        }
    }

    public static String decrypt(SecretKey secret, byte[] encrypted) throws CryptoException {
        try {
            return decrypt(secret, new String(encrypted, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new CryptoException("Unable to decrypt", e);
        }
    }


    public static SecretKey getSecretKey(String password, String salt) throws CryptoException {
        try {
            PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), Hex.decode(salt), PBE_ITERATION_COUNT, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE_ALGORITHM, PROVIDER);
            SecretKey tmp = factory.generateSecret(pbeKeySpec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), SECRET_KEY_ALGORITHM);
            return secret;
        } catch (Exception e) {
            throw new CryptoException("Unable to get secret key", e);
        }
    }

    // TODO - Clean up, constants, etc.

    public static String getHash(String password, String salt) throws CryptoException {
        try {
            String input = password + salt;
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM, PROVIDER);
            byte[] out = md.digest(input.getBytes("UTF-8"));
            return Hex.toHexString(out);
        } catch (Exception e) {
            throw new CryptoException("Unable to get hash", e);
        }
    }

    public static String generateSalt() throws CryptoException {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);
            String saltHex = Hex.toHexString(salt);
            return saltHex;
        } catch (Exception e) {
            throw new CryptoException("Unable to generate salt", e);
        }
    }

    private static byte[] generateIv() throws NoSuchAlgorithmException, NoSuchProviderException {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        return iv;
    }

}