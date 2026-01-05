package com.orion.proyectoorion.emergency

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * EmergencyContacts - Gestiona los contactos de emergencia
 * 
 * Los contactos se añaden compartiendo códigos vía WhatsApp/Telegram
 * Cada contacto tiene: nombre, código público, ID derivado
 */
class EmergencyContacts(private val context: Context) {
    
    private val contactsFile = File(context.filesDir, "emergency_contacts.json")
    private val messagesFile = File(context.filesDir, "emergency_messages.json")
    
    /**
     * Obtener todos los contactos
     */
    fun getContacts(): List<EmergencyContact> {
        if (!contactsFile.exists()) return emptyList()
        
        return try {
            val json = JSONArray(contactsFile.readText())
            (0 until json.length()).map { i ->
                val obj = json.getJSONObject(i)
                EmergencyContact(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    publicKey = obj.getString("publicKey"),
                    addedAt = obj.getLong("addedAt"),
                    isFavorite = obj.optBoolean("isFavorite", false)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Añadir contacto desde código compartido
     */
    fun addContact(name: String, publicKeyCode: String): EmergencyContact? {
        val contactId = EmergencyCrypto.getContactId(publicKeyCode) ?: return null
        
        // Verificar que no existe
        val existing = getContacts()
        if (existing.any { it.id == contactId }) {
            return existing.first { it.id == contactId }
        }
        
        val contact = EmergencyContact(
            id = contactId,
            name = name.trim(),
            publicKey = publicKeyCode,
            addedAt = System.currentTimeMillis(),
            isFavorite = false
        )
        
        val contacts = existing.toMutableList()
        contacts.add(contact)
        saveContacts(contacts)
        
        return contact
    }
    
    /**
     * Eliminar contacto
     */
    fun removeContact(contactId: String) {
        val contacts = getContacts().filter { it.id != contactId }
        saveContacts(contacts)
    }
    
    /**
     * Marcar/desmarcar favorito
     */
    fun toggleFavorite(contactId: String) {
        val contacts = getContacts().map { contact ->
            if (contact.id == contactId) {
                contact.copy(isFavorite = !contact.isFavorite)
            } else contact
        }
        saveContacts(contacts)
    }
    
    /**
     * Obtener contacto por ID
     */
    fun getContact(contactId: String): EmergencyContact? {
        return getContacts().firstOrNull { it.id == contactId }
    }
    
    /**
     * Obtener contacto por clave pública (sender key del mensaje)
     */
    fun getContactBySenderKey(senderKey: String): EmergencyContact? {
        val senderId = EmergencyCrypto.getContactId(senderKey) ?: return null
        return getContacts().firstOrNull { it.id == senderId }
    }
    
    private fun saveContacts(contacts: List<EmergencyContact>) {
        val json = JSONArray()
        contacts.forEach { contact ->
            json.put(JSONObject().apply {
                put("id", contact.id)
                put("name", contact.name)
                put("publicKey", contact.publicKey)
                put("addedAt", contact.addedAt)
                put("isFavorite", contact.isFavorite)
            })
        }
        contactsFile.writeText(json.toString())
    }
    
    // ============================================================
    // HISTORIAL DE MENSAJES
    // ============================================================
    
    /**
     * Guardar mensaje en historial
     */
    fun saveMessage(message: EmergencyMessage, isOutgoing: Boolean) {
        val messages = getMessageHistory().toMutableList()
        messages.add(SavedMessage(
            messageId = message.id,
            type = message.type,
            senderId = message.senderId,
            recipientId = message.recipientId,
            content = message.payload.data,
            timestamp = message.timestamp,
            isOutgoing = isOutgoing,
            hopCount = message.hopCount
        ))
        
        // Mantener máximo 500 mensajes
        val trimmed = messages.takeLast(500)
        saveMessageHistory(trimmed)
    }
    
    /**
     * Obtener historial de mensajes
     */
    fun getMessageHistory(): List<SavedMessage> {
        if (!messagesFile.exists()) return emptyList()
        
        return try {
            val json = JSONArray(messagesFile.readText())
            (0 until json.length()).map { i ->
                val obj = json.getJSONObject(i)
                SavedMessage(
                    messageId = obj.getString("messageId"),
                    type = MessageType.valueOf(obj.getString("type")),
                    senderId = obj.getString("senderId"),
                    recipientId = obj.optString("recipientId", null).takeIf { it != "null" },
                    content = obj.getString("content"),
                    timestamp = obj.getLong("timestamp"),
                    isOutgoing = obj.getBoolean("isOutgoing"),
                    hopCount = obj.getInt("hopCount")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Obtener mensajes con un contacto específico
     */
    fun getMessagesWithContact(contactId: String): List<SavedMessage> {
        return getMessageHistory().filter { 
            it.senderId == contactId || it.recipientId == contactId 
        }
    }
    
    /**
     * Limpiar historial
     */
    fun clearHistory() {
        messagesFile.delete()
    }
    
    private fun saveMessageHistory(messages: List<SavedMessage>) {
        val json = JSONArray()
        messages.forEach { msg ->
            json.put(JSONObject().apply {
                put("messageId", msg.messageId)
                put("type", msg.type.name)
                put("senderId", msg.senderId)
                put("recipientId", msg.recipientId ?: JSONObject.NULL)
                put("content", msg.content)
                put("timestamp", msg.timestamp)
                put("isOutgoing", msg.isOutgoing)
                put("hopCount", msg.hopCount)
            })
        }
        messagesFile.writeText(json.toString())
    }
}

/**
 * Contacto de emergencia
 */
data class EmergencyContact(
    val id: String,          // ID derivado de la clave pública (8 chars hex)
    val name: String,        // Nombre dado por el usuario
    val publicKey: String,   // Clave pública para cifrado
    val addedAt: Long,       // Timestamp de cuando se añadió
    val isFavorite: Boolean  // Para acceso rápido
)

/**
 * Mensaje guardado en historial
 */
data class SavedMessage(
    val messageId: String,
    val type: MessageType,
    val senderId: String,
    val recipientId: String?,
    val content: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val hopCount: Int
)
