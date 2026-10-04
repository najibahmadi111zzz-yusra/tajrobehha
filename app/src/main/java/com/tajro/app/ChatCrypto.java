package com.tajro.app;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class ChatCrypto {

    private static final String STORE = "AndroidKeyStore";
    private static final String ALIAS_PREFIX = "tajro_chat_e2ee_";
    private static final int AES_BITS = 256;
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;
    private static final int RSA_BITS = 3072;

    private ChatCrypto() {}

    public static final class EncryptedText {
        public final String cipherText;
        public final String iv;
        public final String wrappedKeyForReceiver;
        public final String wrappedKeyForSender;

        public EncryptedText(String cipherText, String iv, String wrappedKeyForReceiver, String wrappedKeyForSender) {
            this.cipherText = cipherText;
            this.iv = iv;
            this.wrappedKeyForReceiver = wrappedKeyForReceiver;
            this.wrappedKeyForSender = wrappedKeyForSender;
        }
    }

    public static final class EncryptedFile {
        public final File file;
        public final String iv;
        public final String wrappedKeyForReceiver;
        public final String wrappedKeyForSender;

        public EncryptedFile(File file, String iv, String wrappedKeyForReceiver, String wrappedKeyForSender) {
            this.file = file;
            this.iv = iv;
            this.wrappedKeyForReceiver = wrappedKeyForReceiver;
            this.wrappedKeyForSender = wrappedKeyForSender;
        }
    }

    private static String alias(String uid) {
        return ALIAS_PREFIX + uid;
    }

    public static void ensureKeyPair(Context context, String uid) throws Exception {
        if (uid == null || uid.trim().isEmpty()) {
            throw new IllegalArgumentException("uid is empty");
        }

        KeyStore ks = KeyStore.getInstance(STORE);
        ks.load(null);

        if (ks.containsAlias(alias(uid))) {
            return;
        }

        KeyPairGenerator generator =
                KeyPairGenerator.getInstance(
                        "RSA",
                        STORE
                );

        android.security.keystore.KeyGenParameterSpec spec =
                new android.security.keystore.KeyGenParameterSpec.Builder(
                        alias(uid),
                        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT |
                                android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                        .setKeySize(RSA_BITS)
                        .setDigests(
                                android.security.keystore.KeyProperties.DIGEST_SHA256,
                                android.security.keystore.KeyProperties.DIGEST_SHA512
                        )
                        .setEncryptionPaddings(
                                android.security.keystore.KeyProperties.ENCRYPTION_PADDING_RSA_OAEP
                        )
                        .build();

        generator.initialize(spec);
        generator.generateKeyPair();
    }

    public static String getPublicKeyBase64(String uid) throws Exception {
        KeyStore ks = KeyStore.getInstance(STORE);
        ks.load(null);

        java.security.cert.Certificate cert =
                ks.getCertificate(alias(uid));

        if (cert == null) {
            throw new IllegalStateException("E2EE key not found");
        }

        return Base64.encodeToString(
                cert.getPublicKey().getEncoded(),
                Base64.NO_WRAP
        );
    }

    public static void ensureAndPublishKey(
            Context context,
            String uid,
            FirebaseFirestore db
    ) {
        new Thread(() -> {
            try {
                ensureKeyPair(context, uid);
                String publicKey = getPublicKeyBase64(uid);

                Map<String, Object> data = new HashMap<>();
                data.put("chatPublicKey", publicKey);
                data.put("chatE2EEVersion", 1L);

                db.collection("users")
                        .document(uid)
                        .set(data, SetOptions.merge());
            } catch (Exception ignored) {
            }
        }).start();
    }

    public static PublicKey publicKeyFromBase64(String value) throws Exception {
        byte[] encoded = Base64.decode(value, Base64.NO_WRAP);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(new X509EncodedKeySpec(encoded));
    }

    private static PrivateKey getPrivateKey(String uid) throws Exception {
        KeyStore ks = KeyStore.getInstance(STORE);
        ks.load(null);
        KeyStore.Entry entry = ks.getEntry(alias(uid), null);
        if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
            throw new IllegalStateException("E2EE private key not found");
        }
        return ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
    }

    private static SecretKey randomAesKey() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(AES_BITS);
        return generator.generateKey();
    }

    private static byte[] randomIv() {
        byte[] iv = new byte[GCM_IV_BYTES];
        new java.security.SecureRandom().nextBytes(iv);
        return iv;
    }

    private static byte[] rsaWrap(SecretKey key, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
        OAEPParameterSpec spec = new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA1,
                PSource.PSpecified.DEFAULT
        );
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, spec);
        return cipher.doFinal(key.getEncoded());
    }

    private static SecretKey rsaUnwrap(String wrappedKey, String uid) throws Exception {
        if (wrappedKey == null || wrappedKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Wrapped key is empty");
        }
        
        // پاکسازی تمام کاراکترهای فضای خالی و خط جدید احتمالی و تبدیل امن با ساختار NO_WRAP
        String cleanWrappedKey = wrappedKey.trim().replaceAll("\\s", "");
        byte[] wrapped = Base64.decode(cleanWrappedKey, Base64.NO_WRAP);
        
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
        OAEPParameterSpec spec = new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA1,
                PSource.PSpecified.DEFAULT
        );
        
        cipher.init(Cipher.DECRYPT_MODE, getPrivateKey(uid), spec);
        byte[] raw = cipher.doFinal(wrapped);
        return new SecretKeySpec(raw, "AES");
    }

    public static EncryptedText encryptText(
            String plain,
            PublicKey receiverPublicKey,
            PublicKey senderPublicKey
    ) throws Exception {
        SecretKey aes = randomAesKey();
        byte[] iv = randomIv();

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.ENCRYPT_MODE,
                aes,
                new GCMParameterSpec(GCM_TAG_BITS, iv)
        );

        byte[] encrypted = cipher.doFinal(
                plain.getBytes(StandardCharsets.UTF_8)
        );

        return new EncryptedText(
                Base64.encodeToString(encrypted, Base64.NO_WRAP),
                Base64.encodeToString(iv, Base64.NO_WRAP),
                Base64.encodeToString(rsaWrap(aes, receiverPublicKey), Base64.NO_WRAP),
                Base64.encodeToString(rsaWrap(aes, senderPublicKey), Base64.NO_WRAP)
        );
    }

    public static String decryptTextForUser(
            String cipherText,
            String ivBase64,
            String wrappedKey,
            String uid
    ) throws Exception {
        SecretKey aes = rsaUnwrap(wrappedKey, uid);
        byte[] iv = Base64.decode(ivBase64, Base64.NO_WRAP);
        byte[] encrypted = Base64.decode(cipherText, Base64.NO_WRAP);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.DECRYPT_MODE,
                aes,
                new GCMParameterSpec(GCM_TAG_BITS, iv)
        );

        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    public static EncryptedFile encryptFile(
            Context context,
            Uri source,
            PublicKey receiverPublicKey,
            PublicKey senderPublicKey
    ) throws Exception {
        SecretKey aes = randomAesKey();
        byte[] iv = randomIv();

        File out = new File(
                context.getCacheDir(),
                "e2ee_" + System.currentTimeMillis() + ".bin"
        );

        InputStream input = context.getContentResolver().openInputStream(source);
        if (input == null) throw new IllegalStateException("Cannot read file");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
            
Cipher.ENCRYPT_MODE,
aes,
new GCMParameterSpec(GCM_TAG_BITS, iv)
);
OutputStream fileOut = new FileOutputStream(out);
CipherOutputStream encryptedOut = new CipherOutputStream(fileOut, cipher);
byte[] buffer = new byte[8192];
int count;
try {
while ((count = input.read(buffer)) != -1) {
encryptedOut.write(buffer, 0, count);
}
} finally {
try { encryptedOut.flush(); } catch (Exception ignored) {}
try { input.close(); } catch (Exception ignored) {}
try { encryptedOut.close(); } catch (Exception ignored) {}
}
return new EncryptedFile(
out,
Base64.encodeToString(iv, Base64.NO_WRAP),
Base64.encodeToString(rsaWrap(aes, receiverPublicKey), Base64.NO_WRAP),
Base64.encodeToString(rsaWrap(aes, senderPublicKey), Base64.NO_WRAP)
);
}
public static File decryptFile(
Context context,
File encryptedFile,
String ivBase64,
String wrappedKey,
String uid
) throws Exception {
SecretKey aes = rsaUnwrap(wrappedKey, uid);
byte[] iv = Base64.decode(ivBase64, Base64.NO_WRAP);
File out = new File(
context.getCacheDir(),
"decrypted_" + System.currentTimeMillis()
);
Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
cipher.init(
Cipher.DECRYPT_MODE,
aes,
new GCMParameterSpec(GCM_TAG_BITS, iv)
);
InputStream input = new FileInputStream(encryptedFile);
CipherInputStream decryptedIn = new CipherInputStream(input, cipher);
OutputStream output = new FileOutputStream(out);
byte[] buffer = new byte[8192];
int count;
try {
while ((count = decryptedIn.read(buffer)) != -1) {
output.write(buffer, 0, count);
}
} finally {
try { output.flush(); } catch (Exception ignored) {}
try { decryptedIn.close(); } catch (Exception ignored) {}
try { output.close(); } catch (Exception ignored) {}
}
return out
}
    
}
