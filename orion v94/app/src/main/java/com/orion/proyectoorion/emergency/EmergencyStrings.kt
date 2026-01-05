package com.orion.proyectoorion.emergency

/**
 * Strings multi-idioma para el modo emergencia
 */
data class EmergencyStrings(
    val langCode: String,

    // Pantalla principal
    val title: String,
    val infoTitle: String,
    val infoDesc: String,
    val infoEncrypted: String,
    val infoPublic: String,

    // Estado
    val statusInactive: String,
    val statusSearching: String,
    val statusConnected: String,
    val deviceSingular: String,
    val devicePlural: String,

    // Permisos
    val permWarning: String,
    val wifiWarning: String,
    val locationWarning: String,

    // Botones principales
    val btnActivate: String,
    val btnDeactivate: String,

    // Contactos
    val contactsLabel: String,
    val contactsTitle: String,
    val contactsEmpty: String,
    val contactsEmptyHint: String,
    val addContactTitle: String,
    val addContactName: String,
    val addContactNameHint: String,
    val addContactCode: String,
    val addContactBtn: String,
    val contactDeleted: String,
    val contactAdded: String,
    val invalidCode: String,

    // Scanner QR
    val scanPrompt: String,
    val scanQrBtn: String,
    val qrScanned: String,

    // Mi código QR
    val myQrTitle: String,
    val myQrDesc: String,
    val myQrShowCode: String,
    val myQrHideCode: String,
    val myQrCopy: String,
    val myQrCopied: String,
    val shareQrBtn: String,

    // Mensajes
    val messagesTitle: String,
    val messagesEmpty: String,
    val messagesEmptyInactive: String,
    val messagePlaceholder: String,
    val messageSearching: String,
    val messageReady: String,
    val encryptedLabel: String,
    val selectRecipient: String,
    val recipientFor: String,
    val hopsLabel: String,
    val fromMe: String,
    val privateMsg: String,

    // Botones generales
    val btnClose: String,
    val btnCancel: String
)

// ============================================================
// ESPAÑOL
// ============================================================

val EMERGENCY_ES = EmergencyStrings(
    langCode = "ES",

    title = "EMERGENCIA",
    infoTitle = "📡 Comunicación sin Internet",
    infoDesc = "Los mensajes saltan de móvil en móvil hasta llegar a su destino, sin necesidad de Internet ni cobertura.",
    infoEncrypted = "Cifrado E2E: Envía mensajes privados que solo tu contacto puede leer.",
    infoPublic = "Público: O envía a todos los dispositivos cercanos sin cifrar.",

    statusInactive = "Inactivo",
    statusSearching = "Buscando dispositivos cercanos...",
    statusConnected = "Listo para enviar mensajes",
    deviceSingular = "dispositivo",
    devicePlural = "dispositivos",

    permWarning = "⚠️ Permisos",
    wifiWarning = "⚠️ WiFi",
    locationWarning = "⚠️ Ubicación",

    btnActivate = "ACTIVAR",
    btnDeactivate = "DESACTIVAR",

    contactsLabel = "👥 Contactos",
    contactsTitle = "Contactos",
    contactsEmpty = "Sin contactos",
    contactsEmptyHint = "Escanea el QR de un contacto",
    addContactTitle = "Añadir contacto",
    addContactName = "Nombre",
    addContactNameHint = "Ej: Mamá, Papá...",
    addContactCode = "Su código",
    addContactBtn = "Añadir",
    contactDeleted = "Eliminado",
    contactAdded = "añadido",
    invalidCode = "❌ Código inválido",

    scanPrompt = "Escanea el código QR",
    scanQrBtn = "Escanear QR",
    qrScanned = "QR escaneado ✓",

    myQrTitle = "Tu código QR",
    myQrDesc = "Muestra este QR para que te añadan como contacto",
    myQrShowCode = "Ver código texto",
    myQrHideCode = "Ocultar código",
    myQrCopy = "Copiar",
    myQrCopied = "✅ Copiado",
    shareQrBtn = "Comparte tu QR",

    messagesTitle = "📨 Mensajes",
    messagesEmpty = "Sin mensajes aún",
    messagesEmptyInactive = "Activa para comunicarte",
    messagePlaceholder = "Mensaje...",
    messageSearching = "Buscando dispositivos cercanos...",
    messageReady = "Listo para enviar mensajes",
    encryptedLabel = "Cifrado",
    selectRecipient = "Selecciona destinatario ↑",
    recipientFor = "Para:",
    hopsLabel = "salto(s)",
    fromMe = "Yo",
    privateMsg = "🔒 Privado",

    btnClose = "Cerrar",
    btnCancel = "Cancelar"
)

// ============================================================
// ENGLISH
// ============================================================

val EMERGENCY_EN = EmergencyStrings(
    langCode = "EN",

    title = "EMERGENCY",
    infoTitle = "📡 Communication without Internet",
    infoDesc = "Messages hop from phone to phone until they reach their destination, without needing Internet or coverage.",
    infoEncrypted = "E2E Encrypted: Send private messages that only your contact can read.",
    infoPublic = "Public: Or send to all nearby devices without encryption.",

    statusInactive = "Inactive",
    statusSearching = "Searching for nearby devices...",
    statusConnected = "Ready to send messages",
    deviceSingular = "device",
    devicePlural = "devices",

    permWarning = "⚠️ Permissions",
    wifiWarning = "⚠️ WiFi",
    locationWarning = "⚠️ Location",

    btnActivate = "ACTIVATE",
    btnDeactivate = "DEACTIVATE",

    contactsLabel = "👥 Contacts",
    contactsTitle = "Contacts",
    contactsEmpty = "No contacts",
    contactsEmptyHint = "Scan a contact's QR code",
    addContactTitle = "Add contact",
    addContactName = "Name",
    addContactNameHint = "Ex: Mom, Dad...",
    addContactCode = "Their code",
    addContactBtn = "Add",
    contactDeleted = "Deleted",
    contactAdded = "added",
    invalidCode = "❌ Invalid code",

    scanPrompt = "Scan QR code",
    scanQrBtn = "Scan QR",
    qrScanned = "QR scanned ✓",

    myQrTitle = "Your QR code",
    myQrDesc = "Show this QR so others can add you as a contact",
    myQrShowCode = "Show text code",
    myQrHideCode = "Hide code",
    myQrCopy = "Copy",
    myQrCopied = "✅ Copied",
    shareQrBtn = "Share your QR",

    messagesTitle = "📨 Messages",
    messagesEmpty = "No messages yet",
    messagesEmptyInactive = "Activate to communicate",
    messagePlaceholder = "Message...",
    messageSearching = "Searching for nearby devices...",
    messageReady = "Ready to send messages",
    encryptedLabel = "Encrypted",
    selectRecipient = "Select recipient ↑",
    recipientFor = "To:",
    hopsLabel = "hop(s)",
    fromMe = "Me",
    privateMsg = "🔒 Private",

    btnClose = "Close",
    btnCancel = "Cancel"
)

// ============================================================
// FRANÇAIS
// ============================================================

val EMERGENCY_FR = EmergencyStrings(
    langCode = "FR",

    title = "URGENCE",
    infoTitle = "📡 Communication sans Internet",
    infoDesc = "Les messages sautent de téléphone en téléphone jusqu'à leur destination, sans Internet ni couverture.",
    infoEncrypted = "Chiffré E2E: Envoyez des messages privés que seul votre contact peut lire.",
    infoPublic = "Public: Ou envoyez à tous les appareils proches sans chiffrement.",

    statusInactive = "Inactif",
    statusSearching = "Recherche d'appareils proches...",
    statusConnected = "Prêt à envoyer des messages",
    deviceSingular = "appareil",
    devicePlural = "appareils",

    permWarning = "⚠️ Permissions",
    wifiWarning = "⚠️ WiFi",
    locationWarning = "⚠️ Localisation",

    btnActivate = "ACTIVER",
    btnDeactivate = "DÉSACTIVER",

    contactsLabel = "👥 Contacts",
    contactsTitle = "Contacts",
    contactsEmpty = "Aucun contact",
    contactsEmptyHint = "Scannez le QR d'un contact",
    addContactTitle = "Ajouter contact",
    addContactName = "Nom",
    addContactNameHint = "Ex: Maman, Papa...",
    addContactCode = "Son code",
    addContactBtn = "Ajouter",
    contactDeleted = "Supprimé",
    contactAdded = "ajouté",
    invalidCode = "❌ Code invalide",

    scanPrompt = "Scannez le code QR",
    scanQrBtn = "Scanner QR",
    qrScanned = "QR scanné ✓",

    myQrTitle = "Votre code QR",
    myQrDesc = "Montrez ce QR pour qu'on vous ajoute comme contact",
    myQrShowCode = "Voir code texte",
    myQrHideCode = "Masquer code",
    myQrCopy = "Copier",
    myQrCopied = "✅ Copié",
    shareQrBtn = "Partagez votre QR",

    messagesTitle = "📨 Messages",
    messagesEmpty = "Pas encore de messages",
    messagesEmptyInactive = "Activez pour communiquer",
    messagePlaceholder = "Message...",
    messageSearching = "Recherche d'appareils proches...",
    messageReady = "Prêt à envoyer des messages",
    encryptedLabel = "Chiffré",
    selectRecipient = "Sélectionnez destinataire ↑",
    recipientFor = "Pour:",
    hopsLabel = "saut(s)",
    fromMe = "Moi",
    privateMsg = "🔒 Privé",

    btnClose = "Fermer",
    btnCancel = "Annuler"
)

// ============================================================
// DEUTSCH
// ============================================================

val EMERGENCY_DE = EmergencyStrings(
    langCode = "DE",

    title = "NOTFALL",
    infoTitle = "📡 Kommunikation ohne Internet",
    infoDesc = "Nachrichten springen von Handy zu Handy bis sie ihr Ziel erreichen, ohne Internet oder Empfang.",
    infoEncrypted = "E2E-Verschlüsselt: Sende private Nachrichten, die nur dein Kontakt lesen kann.",
    infoPublic = "Öffentlich: Oder sende an alle Geräte in der Nähe ohne Verschlüsselung.",

    statusInactive = "Inaktiv",
    statusSearching = "Suche nach Geräten in der Nähe...",
    statusConnected = "Bereit zum Senden",
    deviceSingular = "Gerät",
    devicePlural = "Geräte",

    permWarning = "⚠️ Berechtigungen",
    wifiWarning = "⚠️ WLAN",
    locationWarning = "⚠️ Standort",

    btnActivate = "AKTIVIEREN",
    btnDeactivate = "DEAKTIVIEREN",

    contactsLabel = "👥 Kontakte",
    contactsTitle = "Kontakte",
    contactsEmpty = "Keine Kontakte",
    contactsEmptyHint = "Scanne den QR-Code eines Kontakts",
    addContactTitle = "Kontakt hinzufügen",
    addContactName = "Name",
    addContactNameHint = "Z.B.: Mama, Papa...",
    addContactCode = "Sein Code",
    addContactBtn = "Hinzufügen",
    contactDeleted = "Gelöscht",
    contactAdded = "hinzugefügt",
    invalidCode = "❌ Ungültiger Code",

    scanPrompt = "QR-Code scannen",
    scanQrBtn = "QR scannen",
    qrScanned = "QR gescannt ✓",

    myQrTitle = "Dein QR-Code",
    myQrDesc = "Zeige diesen QR, damit andere dich als Kontakt hinzufügen können",
    myQrShowCode = "Code anzeigen",
    myQrHideCode = "Code verbergen",
    myQrCopy = "Kopieren",
    myQrCopied = "✅ Kopiert",
    shareQrBtn = "Teile deinen QR",

    messagesTitle = "📨 Nachrichten",
    messagesEmpty = "Noch keine Nachrichten",
    messagesEmptyInactive = "Aktiviere zum Kommunizieren",
    messagePlaceholder = "Nachricht...",
    messageSearching = "Suche nach Geräten in der Nähe...",
    messageReady = "Bereit zum Senden",
    encryptedLabel = "Verschlüsselt",
    selectRecipient = "Empfänger auswählen ↑",
    recipientFor = "An:",
    hopsLabel = "Sprung/Sprünge",
    fromMe = "Ich",
    privateMsg = "🔒 Privat",

    btnClose = "Schließen",
    btnCancel = "Abbrechen"
)

// ============================================================
// 中文
// ============================================================

val EMERGENCY_ZH = EmergencyStrings(
    langCode = "ZH",

    title = "紧急模式",
    infoTitle = "📡 无网络通信",
    infoDesc = "消息从手机跳到手机直到到达目的地，无需互联网或信号覆盖。",
    infoEncrypted = "端到端加密：发送只有您的联系人才能阅读的私人消息。",
    infoPublic = "公开：或向所有附近设备发送未加密消息。",

    statusInactive = "未激活",
    statusSearching = "正在搜索附近设备...",
    statusConnected = "准备发送消息",
    deviceSingular = "设备",
    devicePlural = "设备",

    permWarning = "⚠️ 权限",
    wifiWarning = "⚠️ WiFi",
    locationWarning = "⚠️ 位置",

    btnActivate = "激活",
    btnDeactivate = "停用",

    contactsLabel = "👥 联系人",
    contactsTitle = "联系人",
    contactsEmpty = "无联系人",
    contactsEmptyHint = "扫描联系人的二维码",
    addContactTitle = "添加联系人",
    addContactName = "姓名",
    addContactNameHint = "例如：妈妈、爸爸...",
    addContactCode = "对方的代码",
    addContactBtn = "添加",
    contactDeleted = "已删除",
    contactAdded = "已添加",
    invalidCode = "❌ 无效代码",

    scanPrompt = "扫描二维码",
    scanQrBtn = "扫描二维码",
    qrScanned = "二维码已扫描 ✓",

    myQrTitle = "你的二维码",
    myQrDesc = "展示此二维码让他人添加你为联系人",
    myQrShowCode = "显示文本代码",
    myQrHideCode = "隐藏代码",
    myQrCopy = "复制",
    myQrCopied = "✅ 已复制",
    shareQrBtn = "分享你的二维码",

    messagesTitle = "📨 消息",
    messagesEmpty = "暂无消息",
    messagesEmptyInactive = "激活以开始通信",
    messagePlaceholder = "消息...",
    messageSearching = "正在搜索附近设备...",
    messageReady = "准备发送消息",
    encryptedLabel = "加密",
    selectRecipient = "选择收件人 ↑",
    recipientFor = "发给：",
    hopsLabel = "跳",
    fromMe = "我",
    privateMsg = "🔒 私密消息",

    btnClose = "关闭",
    btnCancel = "取消"
)

// ============================================================
// РУССКИЙ
// ============================================================

val EMERGENCY_RU = EmergencyStrings(
    langCode = "RU",

    title = "ЭКСТРЕННЫЙ",
    infoTitle = "📡 Связь без интернета",
    infoDesc = "Сообщения прыгают с телефона на телефон, пока не достигнут цели, без интернета и покрытия.",
    infoEncrypted = "E2E шифрование: Отправляйте личные сообщения, которые может прочитать только ваш контакт.",
    infoPublic = "Публично: Или отправьте всем ближайшим устройствам без шифрования.",

    statusInactive = "Неактивно",
    statusSearching = "Поиск ближайших устройств...",
    statusConnected = "Готово к отправке сообщений",
    deviceSingular = "устройство",
    devicePlural = "устройств",

    permWarning = "⚠️ Разрешения",
    wifiWarning = "⚠️ WiFi",
    locationWarning = "⚠️ Локация",

    btnActivate = "АКТИВИРОВАТЬ",
    btnDeactivate = "ДЕАКТИВИРОВАТЬ",

    contactsLabel = "👥 Контакты",
    contactsTitle = "Контакты",
    contactsEmpty = "Нет контактов",
    contactsEmptyHint = "Отсканируйте QR-код контакта",
    addContactTitle = "Добавить контакт",
    addContactName = "Имя",
    addContactNameHint = "Пр.: Мама, Папа...",
    addContactCode = "Его код",
    addContactBtn = "Добавить",
    contactDeleted = "Удалено",
    contactAdded = "добавлен",
    invalidCode = "❌ Неверный код",

    scanPrompt = "Сканировать QR-код",
    scanQrBtn = "Сканировать QR",
    qrScanned = "QR отсканирован ✓",

    myQrTitle = "Ваш QR-код",
    myQrDesc = "Покажите этот QR, чтобы вас добавили как контакт",
    myQrShowCode = "Показать код",
    myQrHideCode = "Скрыть код",
    myQrCopy = "Копировать",
    myQrCopied = "✅ Скопировано",
    shareQrBtn = "Поделиться QR",

    messagesTitle = "📨 Сообщения",
    messagesEmpty = "Пока нет сообщений",
    messagesEmptyInactive = "Активируйте для общения",
    messagePlaceholder = "Сообщение...",
    messageSearching = "Поиск ближайших устройств...",
    messageReady = "Готово к отправке сообщений",
    encryptedLabel = "Зашифровано",
    selectRecipient = "Выберите получателя ↑",
    recipientFor = "Кому:",
    hopsLabel = "прыжок(ов)",
    fromMe = "Я",
    privateMsg = "🔒 Личное",

    btnClose = "Закрыть",
    btnCancel = "Отмена"
)

// ============================================================
// PORTUGUÊS
// ============================================================

val EMERGENCY_PT = EmergencyStrings(
    langCode = "PT",

    title = "EMERGÊNCIA",
    infoTitle = "📡 Comunicação sem Internet",
    infoDesc = "As mensagens saltam de celular em celular até chegar ao destino, sem precisar de Internet ou cobertura.",
    infoEncrypted = "Criptografia E2E: Envie mensagens privadas que só seu contato pode ler.",
    infoPublic = "Público: Ou envie para todos os dispositivos próximos sem criptografia.",

    statusInactive = "Inativo",
    statusSearching = "Procurando dispositivos próximos...",
    statusConnected = "Pronto para enviar mensagens",
    deviceSingular = "dispositivo",
    devicePlural = "dispositivos",

    permWarning = "⚠️ Permissões",
    wifiWarning = "⚠️ WiFi",
    locationWarning = "⚠️ Localização",

    btnActivate = "ATIVAR",
    btnDeactivate = "DESATIVAR",

    contactsLabel = "👥 Contatos",
    contactsTitle = "Contatos",
    contactsEmpty = "Sem contatos",
    contactsEmptyHint = "Escaneie o QR de um contato",
    addContactTitle = "Adicionar contato",
    addContactName = "Nome",
    addContactNameHint = "Ex: Mãe, Pai...",
    addContactCode = "Código dele",
    addContactBtn = "Adicionar",
    contactDeleted = "Excluído",
    contactAdded = "adicionado",
    invalidCode = "❌ Código inválido",

    scanPrompt = "Escaneie o código QR",
    scanQrBtn = "Escanear QR",
    qrScanned = "QR escaneado ✓",

    myQrTitle = "Seu código QR",
    myQrDesc = "Mostre este QR para que te adicionem como contato",
    myQrShowCode = "Ver código texto",
    myQrHideCode = "Ocultar código",
    myQrCopy = "Copiar",
    myQrCopied = "✅ Copiado",
    shareQrBtn = "Compartilhe seu QR",

    messagesTitle = "📨 Mensagens",
    messagesEmpty = "Sem mensagens ainda",
    messagesEmptyInactive = "Ative para se comunicar",
    messagePlaceholder = "Mensagem...",
    messageSearching = "Procurando dispositivos próximos...",
    messageReady = "Pronto para enviar mensagens",
    encryptedLabel = "Criptografado",
    selectRecipient = "Selecione destinatário ↑",
    recipientFor = "Para:",
    hopsLabel = "salto(s)",
    fromMe = "Eu",
    privateMsg = "🔒 Privado",

    btnClose = "Fechar",
    btnCancel = "Cancelar"
)

/**
 * Obtener strings según código de idioma
 */
fun getEmergencyStrings(langCode: String): EmergencyStrings {
    return when (langCode.uppercase()) {
        "ES" -> EMERGENCY_ES
        "EN" -> EMERGENCY_EN
        "FR" -> EMERGENCY_FR
        "DE" -> EMERGENCY_DE
        "ZH", "CN" -> EMERGENCY_ZH
        "RU" -> EMERGENCY_RU
        "PT" -> EMERGENCY_PT
        else -> EMERGENCY_EN
    }
}