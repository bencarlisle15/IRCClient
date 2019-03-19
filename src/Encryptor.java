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
            String[] mainData = encryptAES(jsonText.getBytes(StandardCharsets.UTF_8));
            String withAdditional;
            if (withKey) {
                withAdditional = new JSONStringer().object().key("user").value(encryptRSA(user.getBytes(StandardCharsets.UTF_8))).key("aesKey").value(encryptRSA(getAESKey().getEncoded())).key("macKey").value(encryptRSA(getMACKey().getEncoded())).key("mac").value(mainData[0]).key("iv").value(mainData[1]).key("data").value(mainData[2]).endObject().toString();
            } else {
                withAdditional = new JSONStringer().object().key("user").value(encryptRSA(user.getBytes(StandardCharsets.UTF_8))).key("mac").value(mainData[0]).key("iv").value(mainData[1]).key("data").value(mainData[2]).endObject().toString();
            }
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
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKey secretKey = getAESKey();
            jsonText = Base64.getDecoder().decode(jsonText);
            byte[] mac = Arrays.copyOfRange(jsonText, 0, 64);
            byte[] ivBytes = Arrays.copyOfRange(jsonText, 64, 80);
            byte[] toDecode = Arrays.copyOfRange(jsonText, 80, jsonText.length);
            if (!isValidMac(mac, toDecode)) {
                //todo error handling
                System.out.println("INVALID MAC");
                throw new InvalidKeyException();
            }
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
            return new String(cipher.doFinal(toDecode));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException e) {
            return null;
        }
    }

    private boolean isValidMac(byte[] expectedMac, byte[] ciphertext) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        SecretKey secretKey = getMACKey();
        Mac mac = Mac.getInstance("HMACSHA512");
        mac.init(secretKey);
        return Arrays.equals(expectedMac, mac.doFinal(ciphertext));
    }

    private String[] encryptAES(byte[] jsonText) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException, InvalidKeyException, IOException, InvalidKeySpecException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKey secretKey = getAESKey();
        byte[] ivBytes = new byte[16];
        new SecureRandom().nextBytes(ivBytes);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        byte[] encBytes = cipher.doFinal(jsonText);
        byte[] mac = addIntegrity(encBytes);
        String[] returnVal = new String[3];
        returnVal[0] = encryptRSA(mac);
        returnVal[1] = encryptRSA(iv.getIV());
        returnVal[2] = Base64.getEncoder().encodeToString(encBytes);
        return returnVal;
    }

    private byte[] addIntegrity(byte[] ciphertext) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        SecretKey secretKey = getMACKey();
        Mac mac = Mac.getInstance("HMACSHA512");
        mac.init(secretKey);
        return mac.doFinal(ciphertext);
    }

    private SecretKey getAESKey() throws IOException, NoSuchAlgorithmException {
        if (!new File("secretAES.ks").exists()) {
            return setAESSecretKey();
        } else {
            String keyFile = "secretAES.ks";
            byte[] keyBytes = Files.readAllBytes(Paths.get(keyFile));
            return new SecretKeySpec(keyBytes, "AES");
        }
    }

    private SecretKey getMACKey() throws IOException, NoSuchAlgorithmException {
        if (!new File("secretHMAC.ks").exists()) {
            return setMACSecretKey();
        } else {
            String keyFile = "secretHMAC.ks";
            byte[] keyBytes = Files.readAllBytes(Paths.get(keyFile));
            return new SecretKeySpec(keyBytes, "HMACSHA512");
        }
    }

    private SecretKey setAESSecretKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        SecretKey secretKey = keyGenerator.generateKey();
        String keyFile = "secretAES.ks";
        try (FileOutputStream out = new FileOutputStream(keyFile)) {
            byte[] keyBytes = secretKey.getEncoded();
            out.write(keyBytes);
        } catch (IOException e) {
            System.out.println("Could not write key to file, Critical Error");
            System.exit(-1);
        }
        return secretKey;
    }

    private SecretKey setMACSecretKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("HMACSHA512");
        SecretKey secretKey = keyGenerator.generateKey();
        String keyFile = "secretHMAC.ks";
        try (FileOutputStream out = new FileOutputStream(keyFile)) {
            byte[] keyBytes = secretKey.getEncoded();
            out.write(keyBytes);
        } catch (IOException e) {
            System.out.println("Could not write key to file, Critical Error");
            System.exit(-1);
        }
        return secretKey;
    }

    private RSAPublicKey getServerPublicKey() throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] keyBytes = Files.readAllBytes(Paths.get("public_key.der"));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
    }
}
