package utils;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

public class Fingerprint {

    public static final String FIELD_USE_FINGERPRINT = "use_fingerprint";
    private static final String KEY_ALIAS = "my pin code";

    public enum SensorState { // Список состояний сканера отпечатков пальцев
        NOT_SUPPORTED,// отпечатки не поддерживаются
        NOT_BLOCKED, // если устройство не защищено пином, рисунком или паролем
        NO_FINGERPRINTS, // если на устройстве нет отпечатков
        READY
    }

    private static Cipher sCipher;  // шифровальщик

    private static KeyStore sKeyStore; // Защищенное хранилище для ключей.
    private static KeyPairGenerator sKeyPairGenerator; // Генератор публичных и закрытых ключей

    private static boolean checkFingerprintCompatibility(@NonNull Context context) { // проверка доступности сканера отпечатков
        return FingerprintManagerCompat.from(context).isHardwareDetected(); //FingerprintManagerCompat — это удобная обертка для обычного FingerprintManager’а, которая упрощает проверку устройства на совместимость, инкапсулируя в себе проверку версии API. В данном случае, isHardwareDetected() вернет false, если API ниже 23.
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public static SensorState checkSensorState(@NonNull Context context) { // нужно понять, готов ли сенсор к использованию
        if (checkFingerprintCompatibility(context)) {
            KeyguardManager keyguardManager =
                    (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            assert keyguardManager != null;
            if (!keyguardManager.isKeyguardSecure()) {
                return SensorState.NOT_BLOCKED; // если устройство не защищено пином, рисунком или паролем
            }
            FingerprintManagerCompat fingerprintManager = FingerprintManagerCompat.from(context);
            if (!fingerprintManager.hasEnrolledFingerprints()) {
                return SensorState.NO_FINGERPRINTS; // если на устройстве нет отпечатков
            }
            return SensorState.READY;
        } else {
            return SensorState.NOT_SUPPORTED;
        }
    }

    private static boolean getKeyStore() { // инициализирую хранилище для сохранения ключа пин-кода
        try {
            sKeyStore = KeyStore.getInstance("AndroidKeyStore"); // получаю доступ к хранилищу криптографических ключей??
            sKeyStore.load(null); // видимо, проверка возможности доступа к хранилищу
            return true;
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean getKeyPairGenerator() { // Ключи мы будем доставать из кейстора, но сначала нужно их туда положить. Для создания ключа воспользуемся генератором.
        try {
            // При инициализации мы указываем, в какой кейстор пойдут сгенерированные ключи и для какого алгоритма предназначен этот ключ.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // для данной проверки требуется версия андроида M или больше
                sKeyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore"); // RSA  — криптографический алгоритм с открытым ключом, основывающийся на вычислительной сложности задачи факторизации больших целых чисел.
            }
            return true;
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean generateNewKey() { // Сама же генерация происходит следующим образом:
        if (getKeyPairGenerator()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    sKeyPairGenerator.initialize(new KeyGenParameterSpec.Builder(KEY_ALIAS, // KEY_ALIAS — это псевдоним ключа, по которому мы будем выдергивать его из кейстора, обычный psfs.
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT) // цели действия- шифровка\дешифровка, вероятно
                            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512) // Устанавливает набор алгоритмов дайджеста (например, SHA-256, SHA-384), с которыми может использоваться ключ. Попытки использовать ключ с любым другим алгоритмом дайджеста будут отклонены.
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP) // Устанавливает набор схем заполнения (например, PKCS7Padding, OAEPPadding, PKCS1Padding, NoPadding), с которыми ключ может использоваться при шифровании / дешифровании. Попытки использовать ключ с любой другой схемой заполнения будут отклонены.
                            .setUserAuthenticationRequired(true) //  — этот флаг указывает, что каждый раз, когда нам нужно будет воспользоваться ключом, нужно будет подтвердить себя, в нашем случае — с помощью отпечатка.
                            .build());
                }
                sKeyPairGenerator.generateKeyPair();
                return true;
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static boolean isKeyReady() { // Проверять наличие ключа будем следующим образом:
        try {
            return sKeyStore.containsAlias(KEY_ALIAS) || generateNewKey(); // если ключ уже существует или он создаётся
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean getCipher() { // Шифровкой и дешифровкой в Java занимается объект Cipher. Инициализируем его:
        try {
            sCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding"); // Адовая мешанина в аргументе — это строка трансформации, которая включает в себя алгоритм, режим смешивания и дополнение.
            return true;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static boolean initCipher(int mode) { // После того, как мы получили Cipher, нужно подготовить его к работе. При генерации ключа мы указали, что будем использовать его только для шифровки и расшифровки. Соответственно, Cipher тоже будет для этих целей:
        try {
            sKeyStore.load(null);
            switch (mode) {
                case Cipher.ENCRYPT_MODE:
                    initEncodeCipher(mode);
                    break;
                case Cipher.DECRYPT_MODE:
                    initDecodeCipher(mode);
                    break;
                default:
                    return false; //this cipher is only for encode\decode
            }
            return true;
        } catch (KeyPermanentlyInvalidatedException exception) {
            deleteInvalidKey();
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException |
                NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void initDecodeCipher(int mode) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, InvalidKeyException { // Расшифровка ключа
        PrivateKey key = (PrivateKey) sKeyStore.getKey(KEY_ALIAS, null);
        sCipher.init(mode, key);
    }

    private static void initEncodeCipher(int mode) throws KeyStoreException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException { // шифровка ключа
        // Нетрудно заметить, что зашифровывающий Cipher несколько сложнее инициализировать. Это косяк самого Гугла, суть которого в том, что публичный ключ требует подтверждения пользователя. Мы обходим это требование с помощью слепка ключа (костыль, ага).
        // http://stackoverflow.com/a/36021145/5884194
        PublicKey key = sKeyStore.getCertificate(KEY_ALIAS).getPublicKey();
        PublicKey unrestricted = KeyFactory.getInstance(key.getAlgorithm()).generatePublic(new X509EncodedKeySpec(key.getEncoded()));
        OAEPParameterSpec spec = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
        sCipher.init(mode, unrestricted, spec);
    }

    private static void deleteInvalidKey() { // Удаление ключа
        // Момент с KeyPermanentlyInvalidatedException — если по какой-то причине ключ нельзя использовать, выстреливает это исключение. Возможные причины — добавление нового отпечатка к существующему, смена или полное удаление блокировки. Тогда ключ более не имеет смысла хранить, и мы его удаляем.
        if (getKeyStore()) {
            try {
                sKeyStore.deleteEntry(KEY_ALIAS);
            } catch (KeyStoreException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean prepare() { // Метод, который собирает всю цепочку подготовки:
        return getKeyStore() && getCipher() && isKeyReady();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static String encode(String inputString) { // Опишем метод, который зашифровывает строку аргумент:
        try {
            if (prepare() && initCipher(Cipher.ENCRYPT_MODE)) { // подготовка к шифровке
                byte[] bytes = sCipher.doFinal(inputString.getBytes()); // получаю массив байтов зашифрованной строки
                return Base64.encodeToString(bytes, Base64.NO_WRAP); // перевожу массив зашифрованных байтов в строку результата
            }
        } catch (IllegalBlockSizeException | BadPaddingException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public static String decode(String encodedString, Cipher cipherDecrypter) { // Для расшифровки же используем следующий метод
        // на вход он получает не только зашифрованную строку, но и объект Cipher. Откуда он там взялся, станет ясно позднее.
        try {
            byte[] bytes = Base64.decode(encodedString, Base64.NO_WRAP);
            return new String(cipherDecrypter.doFinal(bytes));
        } catch (IllegalBlockSizeException | BadPaddingException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    // Для того чтобы наконец использовать сенсор, нужно воспользоваться методом FingerprintManagerCompat:
    void authenticate (FingerprintManagerCompat.CryptoObject crypto,
                       CancellationSignal cancel, //  сигнал используется, чтобы отменить режим считывания отпечатков (при сворачивании приложения, например)
                       int flags,
                       FingerprintManagerCompat.AuthenticationCallback callback,
                       Handler handler)
    {

    }

    @RequiresApi(api = Build.VERSION_CODES.M)

    public static FingerprintManagerCompat.CryptoObject getCryptoObject() {

        //Как видно из кода, криптообъект создается из расшифровывающего Cipher. Если этот Cipher прямо сейчас отправить в метод decode(), то вылетит исключение, оповещающее о том, что мы пытаемся использовать ключ без подтверждения.

        //Строго говоря, мы создаем криптообъект и отправляем его на вход в authenticate() как раз для получения этого самого подтверждения.

        //Если getCryptoObject() вернул null, то это значит, что при инициализации Cipher произошел KeyPermanentlyInvalidatedException. Тут уже ничего не поделаешь, кроме как дать пользователю знать, что вход по отпечатку недоступен и ему придется заново ввести пин-код.

        if (prepare() && initCipher(Cipher.DECRYPT_MODE)) {
            return new FingerprintManagerCompat.CryptoObject(sCipher);
        }
        return null;
    }

    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
        //грязные пальчики, недостаточно сильный зажим
        //можно показать helpString в виде тоста
    }

    public void onAuthenticationFailed() {
        //отпечаток считался, но не распознался
    }

    public void onAuthenticationError(int errorCode, CharSequence errString) {
        //несколько неудачных попыток считывания (5)
        //после этого сенсор станет недоступным на некоторое время (30 сек)
    }

    public void onAuthenticationSucceeded(@NonNull FingerprintManagerCompat.AuthenticationResult result) {
        //все прошло успешно
        result.getCryptoObject().getCipher();
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    static void test(Context context){
        String getKeyStoreResult = encode("hello");
        Log.d("surprise", getKeyStoreResult);
    }
}
