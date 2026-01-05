package com.orion.proyectoorion.emergency

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * ApkSharing - Compartir Orion sin Play Store
 * 
 * En emergencias reales, una sola persona con la app instalada
 * puede pasarla a otros dispositivos de mano en mano.
 * 
 * Flujo:
 * 1. Usuario pulsa "Compartir Orion"
 * 2. Se extrae el APK de la app instalada
 * 3. Se abre el menú de compartir de Android
 * 4. El receptor puede usar Bluetooth, NFC, cable USB, lo que sea
 */
object ApkSharing {
    
    /**
     * Comparte el APK de Orion usando el sistema de compartir nativo de Android.
     * Funciona offline, sin internet, con cualquier método de transferencia.
     */
    fun shareApk(context: Context, langCode: String = "es"): Boolean {
        return try {
            // 1. Obtener la ruta del APK instalado
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            val sourceApk = File(appInfo.sourceDir)
            
            if (!sourceApk.exists()) {
                showError(context, langCode, "APK no encontrado")
                return false
            }
            
            // 2. Copiar a ubicación compartible (cache/shared_apk/)
            val sharedDir = File(context.cacheDir, "shared_apk").apply { mkdirs() }
            val sharedApk = File(sharedDir, "Orion_Emergency.apk")
            
            sourceApk.inputStream().use { input ->
                sharedApk.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // 3. Crear URI con FileProvider
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                sharedApk
            )
            
            // 4. Crear intent de compartir
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                putExtra(Intent.EXTRA_SUBJECT, getShareSubject(langCode))
                putExtra(Intent.EXTRA_TEXT, getShareText(langCode))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // 5. Mostrar selector de apps
            val chooser = Intent.createChooser(shareIntent, getChooserTitle(langCode))
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            
            true
        } catch (e: Exception) {
            showError(context, langCode, e.message ?: "Error desconocido")
            false
        }
    }
    
    /**
     * Obtiene el tamaño del APK para mostrar al usuario
     */
    fun getApkSize(context: Context): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            val size = File(appInfo.sourceDir).length()
            formatSize(size)
        } catch (e: Exception) {
            "?"
        }
    }
    
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        }
    }
    
    private fun showError(context: Context, langCode: String, details: String) {
        val msg = when (langCode) {
            "es" -> "Error al compartir: $details"
            "ca" -> "Error en compartir: $details"
            "pt" -> "Erro ao compartilhar: $details"
            "fr" -> "Erreur de partage: $details"
            "de" -> "Fehler beim Teilen: $details"
            "zh" -> "分享错误: $details"
            else -> "Share error: $details"
        }
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }
    
    private fun getChooserTitle(langCode: String): String = when (langCode) {
        "es" -> "Compartir Orion vía..."
        "ca" -> "Compartir Orion via..."
        "pt" -> "Compartilhar Orion via..."
        "fr" -> "Partager Orion via..."
        "de" -> "Orion teilen über..."
        "zh" -> "通过...分享Orion"
        else -> "Share Orion via..."
    }
    
    private fun getShareSubject(langCode: String): String = when (langCode) {
        "es" -> "Orion - Comunicación de Emergencia"
        "ca" -> "Orion - Comunicació d'Emergència"
        "pt" -> "Orion - Comunicação de Emergência"
        "fr" -> "Orion - Communication d'Urgence"
        "de" -> "Orion - Notfallkommunikation"
        "zh" -> "Orion - 紧急通讯"
        else -> "Orion - Emergency Communication"
    }
    
    private fun getShareText(langCode: String): String = when (langCode) {
        "es" -> """
            📱 ORION - App de Emergencia
            
            Esta app permite comunicación sin internet usando WiFi Direct.
            
            Para instalar:
            1. Guarda el archivo APK
            2. Abre el archivo
            3. Permite "Instalar apps desconocidas" si lo pide
            4. Instala y abre Orion
            5. Ve a Modo Emergencia
            
            ⚠️ Activa WiFi y Ubicación para conectar con otros dispositivos.
        """.trimIndent()
        
        "ca" -> """
            📱 ORION - App d'Emergència
            
            Aquesta app permet comunicació sense internet usant WiFi Direct.
            
            Per instal·lar:
            1. Desa el fitxer APK
            2. Obre el fitxer
            3. Permet "Instal·lar apps desconegudes" si ho demana
            4. Instal·la i obre Orion
            5. Ves a Mode Emergència
            
            ⚠️ Activa WiFi i Ubicació per connectar amb altres dispositius.
        """.trimIndent()
        
        "pt" -> """
            📱 ORION - App de Emergência
            
            Este app permite comunicação sem internet usando WiFi Direct.
            
            Para instalar:
            1. Salve o arquivo APK
            2. Abra o arquivo
            3. Permita "Instalar apps desconhecidos" se solicitado
            4. Instale e abra Orion
            5. Vá para Modo Emergência
            
            ⚠️ Ative WiFi e Localização para conectar com outros dispositivos.
        """.trimIndent()
        
        "fr" -> """
            📱 ORION - App d'Urgence
            
            Cette app permet la communication sans internet via WiFi Direct.
            
            Pour installer:
            1. Enregistrez le fichier APK
            2. Ouvrez le fichier
            3. Autorisez "Installer des apps inconnues" si demandé
            4. Installez et ouvrez Orion
            5. Allez au Mode Urgence
            
            ⚠️ Activez WiFi et Localisation pour vous connecter à d'autres appareils.
        """.trimIndent()
        
        "de" -> """
            📱 ORION - Notfall-App
            
            Diese App ermöglicht Kommunikation ohne Internet über WiFi Direct.
            
            Zur Installation:
            1. APK-Datei speichern
            2. Datei öffnen
            3. "Unbekannte Apps installieren" erlauben falls gefragt
            4. Installieren und Orion öffnen
            5. Zum Notfallmodus gehen
            
            ⚠️ WiFi und Standort aktivieren um sich mit anderen Geräten zu verbinden.
        """.trimIndent()
        
        "zh" -> """
            📱 ORION - 紧急通讯应用
            
            此应用使用WiFi Direct实现无网络通讯。
            
            安装步骤:
            1. 保存APK文件
            2. 打开文件
            3. 如提示，允许"安装未知应用"
            4. 安装并打开Orion
            5. 进入紧急模式
            
            ⚠️ 开启WiFi和位置服务以连接其他设备。
        """.trimIndent()
        
        else -> """
            📱 ORION - Emergency App
            
            This app enables communication without internet using WiFi Direct.
            
            To install:
            1. Save the APK file
            2. Open the file
            3. Allow "Install unknown apps" if prompted
            4. Install and open Orion
            5. Go to Emergency Mode
            
            ⚠️ Enable WiFi and Location to connect with other devices.
        """.trimIndent()
    }
}
