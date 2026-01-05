package com.orion.proyectoorion.emergency

import android.content.Context
import android.util.Base64
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * EmergencyCrypto - Criptografía E2E optimizada para emergencias
 * 
 * Usa ECDH (Elliptic Curve Diffie-Hellman) + AES-GCM
 * Las claves se comparten previamente via WhatsApp/Telegram como códigos cortos
 * 
 * Flujo:
 * 1. Generar par de claves en primer uso
 * 2. Exportar código público corto (para compartir con familia)
 * 3. Importar códigos de contactos de emergencia
 * 4. Cifrar mensajes con ECDH + AES-GCM
 */
object EmergencyCrypto {
    
    private const val PREFS_NAME = "orion_emergency_keys"
    private const val KEY_PRIVATE = "emergency_private_key"
    private const val KEY_PUBLIC = "emergency_public_key"
    private const val KEY_DEVICE_ID = "emergency_device_id"
    
    private const val EC_ALGORITHM = "EC"
    private const val CURVE_NAME = "secp256r1"
    private const val KEY_SIZE = 256
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12
    
    // Cache de claves en memoria
    private var cachedKeyPair: KeyPair? = null
    private var cachedDeviceId: String? = null
    
    /**
     * Inicializa o recupera el par de claves del dispositivo
     */
    fun initializeKeys(context: Context): KeyPair {
        cachedKeyPair?.let { return it }
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val privateKeyB64 = prefs.getString(KEY_PRIVATE, null)
        val publicKeyB64 = prefs.getString(KEY_PUBLIC, null)
        
        return if (privateKeyB64 != null && publicKeyB64 != null) {
            // Recuperar claves existentes
            val keyFactory = KeyFactory.getInstance(EC_ALGORITHM)
            val privateKey = keyFactory.generatePrivate(
                java.security.spec.PKCS8EncodedKeySpec(Base64.decode(privateKeyB64, Base64.NO_WRAP))
            )
            val publicKey = keyFactory.generatePublic(
                X509EncodedKeySpec(Base64.decode(publicKeyB64, Base64.NO_WRAP))
            )
            KeyPair(publicKey, privateKey).also { cachedKeyPair = it }
        } else {
            // Generar nuevas claves
            val keyPairGen = KeyPairGenerator.getInstance(EC_ALGORITHM)
            keyPairGen.initialize(ECGenParameterSpec(CURVE_NAME), SecureRandom())
            val keyPair = keyPairGen.generateKeyPair()
            
            // Guardar
            prefs.edit()
                .putString(KEY_PRIVATE, Base64.encodeToString(keyPair.private.encoded, Base64.NO_WRAP))
                .putString(KEY_PUBLIC, Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP))
                .apply()
            
            cachedKeyPair = keyPair
            keyPair
        }
    }
    
    /**
     * Obtiene el ID único del dispositivo (8 caracteres hexadecimales)
     */
    fun getDeviceId(context: Context): String {
        cachedDeviceId?.let { return it }
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        
        if (deviceId == null) {
            val keyPair = initializeKeys(context)
            val hash = MessageDigest.getInstance("SHA-256").digest(keyPair.public.encoded)
            deviceId = hash.take(4).joinToString("") { "%02X".format(it) }
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        
        cachedDeviceId = deviceId
        return deviceId
    }
    
    /**
     * Obtiene el código público para compartir (Base64 compacto)
     * Este código se comparte via WhatsApp con la familia
     */
    fun getShareableCode(context: Context): String {
        val keyPair = initializeKeys(context)
        val publicKeyBytes = keyPair.public.encoded
        // Usar Base64 URL-safe sin padding para códigos más cortos
        return Base64.encodeToString(publicKeyBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
    
    /**
     * Parsea una clave pública desde el código compartido
     */
    fun parsePublicKey(code: String): PublicKey? {
        return try {
            val keyBytes = Base64.decode(code, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val keyFactory = KeyFactory.getInstance(EC_ALGORITHM)
            keyFactory.generatePublic(X509EncodedKeySpec(keyBytes))
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Genera el ID de un contacto a partir de su código público
     */
    fun getContactId(publicKeyCode: String): String? {
        return try {
            val publicKey = parsePublicKey(publicKeyCode) ?: return null
            val hash = MessageDigest.getInstance("SHA-256").digest(publicKey.encoded)
            hash.take(4).joinToString("") { "%02X".format(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Cifra un mensaje para un contacto específico
     */
    fun encryptMessage(
        context: Context,
        plaintext: String,
        recipientPublicKey: PublicKey
    ): EncryptedPayload {
        val keyPair = initializeKeys(context)
        
        // Derivar secreto compartido via ECDH
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(keyPair.private)
        keyAgreement.doPhase(recipientPublicKey, true)
        val sharedSecret = keyAgreement.generateSecret()
        
        // Derivar clave AES del secreto compartido
        val aesKey = MessageDigest.getInstance("SHA-256").digest(sharedSecret)
        val secretKey = SecretKeySpec(aesKey, "AES")
        
        // Generar IV aleatorio
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        
        // Cifrar con AES-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        
        return EncryptedPayload(
            iv = Base64.encodeToString(iv, Base64.NO_WRAP),
            data = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            senderKey = getShareableCode(context)
        )
    }
    
    /**
     * Descifra un mensaje de un contacto
     */
    fun decryptMessage(
        context: Context,
        payload: EncryptedPayload
    ): String? {
        return try {
            val keyPair = initializeKeys(context)
            val senderPublicKey = parsePublicKey(payload.senderKey) ?: return null
            
            // Derivar secreto compartido
            val keyAgreement = KeyAgreement.getInstance("ECDH")
            keyAgreement.init(keyPair.private)
            keyAgreement.doPhase(senderPublicKey, true)
            val sharedSecret = keyAgreement.generateSecret()
            
            // Derivar clave AES
            val aesKey = MessageDigest.getInstance("SHA-256").digest(sharedSecret)
            val secretKey = SecretKeySpec(aesKey, "AES")
            
            // Descifrar
            val iv = Base64.decode(payload.iv, Base64.NO_WRAP)
            val ciphertext = Base64.decode(payload.data, Base64.NO_WRAP)
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val plaintext = cipher.doFinal(ciphertext)
            
            String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Firma un mensaje para verificar autenticidad
     */
    fun signMessage(context: Context, data: ByteArray): String {
        val keyPair = initializeKeys(context)
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(keyPair.private)
        signature.update(data)
        return Base64.encodeToString(signature.sign(), Base64.NO_WRAP)
    }
    
    /**
     * Verifica una firma
     */
    fun verifySignature(publicKey: PublicKey, data: ByteArray, signatureB64: String): Boolean {
        return try {
            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initVerify(publicKey)
            signature.update(data)
            signature.verify(Base64.decode(signatureB64, Base64.NO_WRAP))
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Payload cifrado para transmisión
 */
data class EncryptedPayload(
    val iv: String,
    val data: String,
    val senderKey: String
)
