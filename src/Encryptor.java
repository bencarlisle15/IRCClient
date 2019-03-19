import org.json.JSONStringer;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

class Encryptor {

    private String user;

    void setUser(String user) {
        this.user = user;
    }

    String getUser() {
        return user;
    }


    String encryptEverything(String jsonText, boolean withKey) {
        try {
            String mainData = encryptAES(jsonText.getBytes(StandardCharsets.UTF_8));
            String withAdditional;
            if (withKey) {
                withAdditional = new JSONStringer().object().key("key").value(encryptRSA(getAESKey().getEncoded())).key("data").value(mainData).endObject().toString();
            } else {
                withAdditional = new JSONStringer().object().key("user").value(encryptRSA(user.getBytes(StandardCharsets.UTF_8))).key("data").value(mainData).endObject().toString();
            }
            System.out.println(withAdditional);
            return withAdditional;
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException | IOException | IllegalBlockSizeException e) {
            return null;
        }
    }


    private String encryptRSA(byte[] withAdditional) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, IOException, InvalidKeySpecException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, getServerPublicKey());
        return Base64.getEncoder().encodeToString(cipher.doFinal(withAdditional));
    }

    String decryptAES(byte[] jsonText) {
        try {
            Cipher ci = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKey skey = getAESKey();
            jsonText = Base64.getDecoder().decode(jsonText);
            byte[] iv = Arrays.copyOfRange(jsonText, 0, 16);
            IvParameterSpec ivspec = new IvParameterSpec(iv);
            ci.init(Cipher.DECRYPT_MODE, skey, ivspec);
            byte[] toDecode = Arrays.copyOfRange(jsonText, 16, jsonText.length);
            return new String(ci.doFinal(toDecode));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException e) {
            return null;
        }
    }

    private String encryptAES(byte[] jsonText) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException, InvalidKeyException, IOException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKey skey = getAESKey();
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivspec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, skey, ivspec);
        byte[] encBytes = cipher.doFinal(jsonText);
        byte[] concat = new byte[iv.length + encBytes.length];
        System.arraycopy(iv, 0, concat, 0, iv.length);
        System.arraycopy(encBytes, 0, concat, iv.length, encBytes.length);
        return Base64.getEncoder().encodeToString(concat);
    }


    private SecretKey getAESKey() throws IOException, NoSuchAlgorithmException {
        if (!new File("secret.ks").exists()) {
            return setSecretKey();
        } else {
            String keyFile = "secret.ks";
            byte[] keyb = Files.readAllBytes(Paths.get(keyFile));
            return new SecretKeySpec(keyb, "AES");
        }
    }

    private SecretKey setSecretKey() throws NoSuchAlgorithmException {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        SecretKey skey = kgen.generateKey();
        String keyFile = "secret.ks";
        try (FileOutputStream out = new FileOutputStream(keyFile)) {
            byte[] keyb = skey.getEncoded();
            out.write(keyb);
        } catch (IOException e) {
            System.out.println("Could not write key to file, Critical Error");
            System.exit(-1);
        }
        return skey;
    }

    private RSAPublicKey getServerPublicKey() throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] keyBytes = Files.readAllBytes(Paths.get("public_key.der"));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
    }
}
