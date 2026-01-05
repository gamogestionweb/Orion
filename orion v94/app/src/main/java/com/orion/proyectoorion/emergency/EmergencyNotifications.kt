package com.orion.proyectoorion.emergency

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Gestiona las notificaciones de emergencia
 * - Canal de alta prioridad para SOS
 * - Canal normal para mensajes
 * - Vibración y sonido personalizado para emergencias
 */
object EmergencyNotifications {

    private const val CHANNEL_SOS = "orion_emergency_sos"
    private const val CHANNEL_MESSAGES = "orion_emergency_messages"

    private const val NOTIFICATION_GROUP = "orion_emergency_group"

    private var notificationId = 1000

    /**
     * Inicializa los canales de notificación (llamar en Application.onCreate o al iniciar mesh)
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            // Canal SOS - Máxima prioridad
            val sosChannel = NotificationChannel(
                CHANNEL_SOS,
                "🆘 Alertas SOS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas de emergencia SOS - máxima prioridad"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500) // Patrón SOS
                enableLights(true)
                lightColor = 0xFFFF0000.toInt() // Rojo
                setBypassDnd(true) // Pasar modo No Molestar
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC

                // Sonido de alarma
                val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setSound(alarmSound, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            }

            // Canal mensajes normales
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "📨 Mensajes de emergencia",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Mensajes recibidos en modo emergencia"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 100, 300)
                enableLights(true)
                lightColor = 0xFF00FF00.toInt() // Verde
            }

            notificationManager.createNotificationChannel(sosChannel)
            notificationManager.createNotificationChannel(messagesChannel)
        }
    }

    /**
     * Muestra notificación cuando llega un mensaje
     */
    fun showMessageNotification(
        context: Context,
        message: EmergencyMessage,
        senderName: String? = null
    ) {
        // Verificar permiso en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                return // Sin permiso, no mostrar
            }
        }

        val isSOS = message.type == MessageType.SOS
        val channelId = if (isSOS) CHANNEL_SOS else CHANNEL_MESSAGES

        // Intent para abrir la app al tocar la notificación
        // IMPORTANTE: FLAG_ACTIVITY_SINGLE_TOP reutiliza la Activity existente vía onNewIntent()
        // sin destruirla, preservando el estado del mesh
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("open_emergency", true)
            putExtra("message_id", message.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            message.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construir título y contenido
        val sender = senderName ?: message.senderId.take(6)
        val (title, icon) = when (message.type) {
            MessageType.SOS -> "🆘 ¡EMERGENCIA!" to "⚠️"
            MessageType.IM_OK -> "✅ Estoy bien" to "👍"
            MessageType.LOCATION -> "📍 Ubicación recibida" to "🗺️"
            MessageType.NEED_HELP -> "🙏 Necesita ayuda" to "❗"
            MessageType.SAFE_ZONE -> "🏠 Zona segura" to "✓"
            MessageType.TEXT -> "💬 Mensaje" to "📨"
        }

        val content = if (isSOS) {
            "¡$sender necesita ayuda urgente!"
        } else {
            "${message.payload.data.take(100)}"
        }

        // Construir notificación
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Usar icono de la app si existe
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(if (isSOS) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (isSOS) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Para SOS, añadir características especiales
        if (isSOS) {
            builder
                .setColorized(true)
                .setColor(0xFFFF0000.toInt())
                .setOngoing(false) // Se puede descartar
                .setFullScreenIntent(pendingIntent, true) // Mostrar en pantalla completa si está bloqueado

            // Vibración extra para SOS
            vibrateDevice(context, isSOS = true)
        }

        // Mostrar
        try {
            NotificationManagerCompat.from(context).notify(notificationId++, builder.build())
        } catch (e: SecurityException) {
            // Sin permiso
        }
    }

    /**
     * Vibra el dispositivo
     */
    private fun vibrateDevice(context: Context, isSOS: Boolean) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = if (isSOS) {
                // Patrón SOS en morse: ... --- ...
                longArrayOf(0, 200, 100, 200, 100, 200, 300, 500, 100, 500, 100, 500, 300, 200, 100, 200, 100, 200)
            } else {
                longArrayOf(0, 300, 100, 300)
            }
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            if (isSOS) {
                vibrator.vibrate(longArrayOf(0, 200, 100, 200, 100, 200, 300, 500, 100, 500, 100, 500), -1)
            } else {
                vibrator.vibrate(500)
            }
        }
    }

    /**
     * Muestra notificación cuando llega un MeshMessage
     * @param isForMe true si el mensaje es para mí (privado) o es broadcast
     */
    fun showMeshNotification(
        context: Context,
        message: MeshMessage,
        decryptedText: String? = null,
        isForMe: Boolean = true
    ) {
        // No notificar mis propios mensajes
        if (message.from == EmergencyCrypto.getDeviceId(context)) return

        // No notificar si no es para mí (mensaje privado para otro)
        if (!isForMe) return

        // Verificar permiso en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        // Detectar si es SOS por el contenido
        val textToCheck = (decryptedText ?: message.text).lowercase()
        val isSOS = textToCheck.contains("sos") ||
                textToCheck.contains("emergencia") ||
                textToCheck.contains("emergency") ||
                textToCheck.contains("ayuda") ||
                textToCheck.contains("help")

        val channelId = if (isSOS) CHANNEL_SOS else CHANNEL_MESSAGES

        // Intent para abrir la app
        // IMPORTANTE: FLAG_ACTIVITY_SINGLE_TOP reutiliza la Activity existente vía onNewIntent()
        // sin destruirla, preservando el estado del mesh
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("open_emergency", true)
            putExtra("message_id", message.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            message.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Título y contenido
        val senderDisplay = message.fromName.ifBlank { message.from.take(6) }
        val title = if (isSOS) "🆘 ¡EMERGENCIA!" else "📨 Mensaje de $senderDisplay"
        val content = decryptedText ?: if (message.enc) "🔒 Mensaje cifrado" else message.text.take(100)

        // Construir notificación
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(if (isSOS) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (isSOS) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (isSOS) {
            builder
                .setColorized(true)
                .setColor(0xFFFF0000.toInt())
                .setFullScreenIntent(pendingIntent, true)
            vibrateDevice(context, isSOS = true)
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId++, builder.build())
        } catch (e: SecurityException) {
            // Sin permiso
        }
    }

    /**
     * Cancela todas las notificaciones de emergencia
     */
    fun cancelAll(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }

    /**
     * Verifica si tenemos permiso para notificaciones (Android 13+)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true // En versiones anteriores no se necesita permiso explícito
        }
    }
}