import org.json.JSONStringer;
import org.json.JSONWriter;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
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
            byte[] iv = getIV();
            SecretKey aesKey = createAESKey();
            byte[] data = jsonText.getBytes(StandardCharsets.UTF_8);
            String mainData = encryptAES(aesKey, data, iv);
            String user = encryptRSA(getUser().getBytes(StandardCharsets.UTF_8));
            String aesKeyString = encryptRSA(aesKey.getEncoded());
            JSONWriter json = new JSONStringer().object().key("user").value(user).key("aesKey").value(aesKeyString).key("iv").value(iv).key("data").value(mainData);
            if (withKey) {
                SecretKey macKey = createMACKey();
                String macKeyString = encryptRSA(macKey.getEncoded());
                String mac = new String(generateMac(macKey, mainData.getBytes(StandardCharsets.UTF_8)));
                json = json.key("macKey").value(macKeyString).key("mac").value(mac);
            }
            return json.endObject().toString();
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
            SecretKey secretKey = createAESKey();
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
        SecretKey secretKey = createMACKey();
        Mac mac = Mac.getInstance("HMACSHA512");
        mac.init(secretKey);
        return Arrays.equals(expectedMac, mac.doFinal(ciphertext));
    }

    private byte[] getIV() {
        byte[] ivBytes = new byte[16];
        new SecureRandom().nextBytes(ivBytes);
        return ivBytes;
    }

    private String encryptAES(SecretKey secretKey, byte[] jsonText, byte[] ivBytes) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        byte[] encBytes = cipher.doFinal(jsonText);
        return Base64.getEncoder().encodeToString(encBytes);
    }

    private byte[] generateMac(SecretKey secretKey, byte[] ciphertext) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HMACSHA512");
        mac.init(secretKey);
        return mac.doFinal(ciphertext);
    }

    private SecretKey createAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        return keyGenerator.generateKey();
    }

    private SecretKey createMACKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("HMACSHA512");
        return keyGenerator.generateKey();
    }

    private RSAPublicKey getServerPublicKey() throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] keyBytes = Files.readAllBytes(Paths.get("public_key.der"));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
    }
}
