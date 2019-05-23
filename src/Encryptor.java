import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONStringer;
import org.json.JSONWriter;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
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
            String user = encryptRSA(getUser().getBytes(StandardCharsets.UTF_8), "user".getBytes(StandardCharsets.UTF_8));
            String aesKeyString = encryptRSA(aesKey.getEncoded(), "aesKey".getBytes(StandardCharsets.UTF_8));
            String rsaIV = encryptRSA(iv, "iv".getBytes(StandardCharsets.UTF_8));
            JSONWriter json = new JSONStringer().object().key("user").value(user).key("aesKey").value(aesKeyString).key("iv").value(rsaIV).key("data").value(mainData);
            if (withKey) {
                SecretKey macKey = createMACKey();
                String macKeyString = encryptRSA(macKey.getEncoded(), "macKey".getBytes(StandardCharsets.UTF_8));
                String mac = encryptRSA(generateMac(macKey, mainData.getBytes(StandardCharsets.UTF_8)), "mac".getBytes(StandardCharsets.UTF_8));
                json = json.key("macKey").value(macKeyString).key("mac").value(mac);
            } else {
                String signature = signData(getSelfPrivateKey(), mainData.getBytes(StandardCharsets.UTF_8));
                json = json.key("signature").value(signature);
            }
            String response = json.endObject().toString();
            return response;
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException | IOException | IllegalBlockSizeException | SignatureException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] randomBytes(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    public String generateNonce() {
        return new String(randomBytes(128));
    }

    private String encryptRSA(byte[] withAdditional, byte[] label) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, IOException, InvalidKeySpecException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-512AndMGF1Padding");
        OAEPParameterSpec oaepParams = new OAEPParameterSpec("SHA-512", "MGF1", new MGF1ParameterSpec("SHA-512"), PSource.PSpecified.DEFAULT);
        cipher.init(Cipher.ENCRYPT_MODE, getServerPublicKey(), oaepParams);
        return Base64.getEncoder().encodeToString(cipher.doFinal(withAdditional));
    }

    public String decryptAES(String aesKey, String jsonText, String ivString) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] aesBytes = decryptRSA(aesKey);
            SecretKey secretKey = getAESKey(aesBytes);
            byte[] ciphertext = Base64.getDecoder().decode(jsonText);
            byte[] ivBytes = decryptRSA(ivString);
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
            return new String(cipher.doFinal(ciphertext));
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeySpecException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] decryptRSA(String ciphertext) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-512AndMGF1Padding");
        OAEPParameterSpec oaepParams = new OAEPParameterSpec("SHA-512", "MGF1", new MGF1ParameterSpec("SHA-512"), PSource.PSpecified.DEFAULT);
        cipher.init(Cipher.DECRYPT_MODE, getSelfPrivateKey(), oaepParams);
        byte[] decoded = Base64.getDecoder().decode(ciphertext.getBytes(StandardCharsets.UTF_8));
        return cipher.doFinal(decoded);
    }

    private byte[] getIV() {
        return randomBytes(16);
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

    private String signData(RSAPrivateKey privateKey, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, InvalidAlgorithmParameterException {
        Signature privateSignature = Signature.getInstance("SHA512withRSA/PSS", new BouncyCastleProvider());
        privateSignature.setParameter(new PSSParameterSpec("SHA-512", "MGF1", MGF1ParameterSpec.SHA512, 64, 1));
        privateSignature.initSign(privateKey);
        privateSignature.update(data);
        return Base64.getEncoder().encodeToString(privateSignature.sign());
    }

    private SecretKey getAESKey(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, "AES");
    }

    private SecretKey createAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        return keyGenerator.generateKey();
    }

    private SecretKey createMACKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("HMACSHA512");
        return keyGenerator.generateKey();
    }

    private void generateKeyPair() throws IOException, NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(4096);
        KeyPair kp = kpg.generateKeyPair();
        Key publicKey = kp.getPublic();
        Key privateKey = kp.getPrivate();
        FileOutputStream out = new FileOutputStream("private.key");
        out.write(privateKey.getEncoded());
        out = new FileOutputStream("public.der");
        out.write(publicKey.getEncoded());
        out.close();
    }

    private RSAPrivateKey getSelfPrivateKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        if (!new File("private.key").exists()) {
            generateKeyPair();
        }
        byte[] keyBytes = Files.readAllBytes(Paths.get("private.key"));
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory fact = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) fact.generatePrivate(keySpec);
    }

    public RSAPublicKey getSelfPublicKey() {
        try {
            if (!new File("public.der").exists()) {
                generateKeyPair();
            }
            byte[] keyBytes = Files.readAllBytes(Paths.get("public.der"));
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(spec);
        } catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException e) {
            return null;
        }
    }

    private RSAPublicKey getServerPublicKey() throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] keyBytes = Files.readAllBytes(Paths.get("public_key.der"));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
    }
}
