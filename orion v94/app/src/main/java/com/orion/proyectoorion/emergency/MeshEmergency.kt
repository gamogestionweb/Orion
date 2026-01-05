package com.orion.proyectoorion.emergency

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*
import android.net.wifi.p2p.WifiP2pManager.*
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Build
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * MeshEmergency v11.0 - WiFi Direct SERVICE DISCOVERY
 *
 * CLAVE: Usa DNS-SD para que SOLO dispositivos Orion se vean entre sí.
 * NO molesta a TVs, impresoras, ni vecinos.
 *
 * Flujo:
 * 1. Registra servicio "_orion._tcp"
 * 2. Busca SOLO dispositivos con ese servicio
 * 3. Cuando encuentra otro Orion → conecta
 * 4. Popup de confirmación solo aparece entre Orions
 */
@SuppressLint("MissingPermission")
class MeshEmergency private constructor(private val context: Context) {

    companion object {
        private const val TAG = "Mesh"
        const val PORT = 8888
        const val UDP_PORT = 8889

        // Identificador único del servicio Orion
        private const val SERVICE_TYPE = "_orion._tcp"
        private const val SERVICE_NAME = "OrionMesh"

        // Singleton instance
        @Volatile
        private var INSTANCE: MeshEmergency? = null

        fun getInstance(context: Context): MeshEmergency {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MeshEmergency(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var wifiP2p: WifiP2pManager? = null
    private var channel: Channel? = null
    private var receiver: BroadcastReceiver? = null
    private var serviceRequest: WifiP2pDnsSdServiceRequest? = null

    private var isGroupOwner = false
    private var groupOwnerAddress: String? = null
    private val orionDevices = ConcurrentHashMap<String, WifiP2pDevice>() // Solo dispositivos Orion
    private val attemptedAddresses = Collections.synchronizedSet(mutableSetOf<String>())
    private var p2pConnected = false

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _peerCount = MutableStateFlow(0)
    val peerCount: StateFlow<Int> = _peerCount.asStateFlow()

    private val _messages = MutableStateFlow<List<MeshMessage>>(emptyList())
    val messages: StateFlow<List<MeshMessage>> = _messages.asStateFlow()

    private val _status = MutableStateFlow("Inactivo")
    val status: StateFlow<String> = _status.asStateFlow()

    private val peers = ConcurrentHashMap<String, Socket>()
    private val seenIds = Collections.synchronizedSet(mutableSetOf<String>())
    private val msgStore = ConcurrentHashMap<String, MeshMessage>()
    private var server: ServerSocket? = null

    val myId: String by lazy { EmergencyCrypto.getDeviceId(context) }
    val myKey: String by lazy { EmergencyCrypto.getShareableCode(context) }
    private val myName: String by lazy { Build.MODEL }

    // ==================== ACTIVATE ====================

    fun activate() {
        if (_isActive.value) return

        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        _isActive.value = true
        _status.value = "Iniciando..."

        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "🚀 MESH v11 - SERVICE DISCOVERY")
        Log.d(TAG, "   ID: ${myId.take(8)}")
        Log.d(TAG, "   Solo conecta con otros Orion")
        Log.d(TAG, "════════════════════════════════════════")

        // 🔔 Inicializar canales de notificación
        EmergencyNotifications.createChannels(context)

        loadMessages()

        // TCP Server
        scope.launch { startServer() }

        // UDP backup (para redes WiFi normales)
        scope.launch { delay(500); udpBroadcast() }
        scope.launch { delay(500); udpListen() }

        // WiFi Direct con Service Discovery
        initWifiDirectWithServiceDiscovery()

        // Mantenimiento
        scope.launch { maintenance() }
    }

    fun deactivate() {
        if (!_isActive.value) return
        _isActive.value = false
        saveMessages()

        peers.values.forEach { runCatching { it.close() } }
        peers.clear()
        runCatching { server?.close() }

        // Limpiar WiFi Direct
        try {
            serviceRequest?.let { wifiP2p?.removeServiceRequest(channel, it, null) }
            wifiP2p?.clearLocalServices(channel, null)
            wifiP2p?.cancelConnect(channel, null)
            wifiP2p?.removeGroup(channel, null)
            wifiP2p?.stopPeerDiscovery(channel, null)
        } catch (e: Exception) {}

        try { receiver?.let { context.unregisterReceiver(it) } } catch (e: Exception) {}
        receiver = null

        orionDevices.clear()
        attemptedAddresses.clear()
        p2pConnected = false

        scope.cancel()
        _peerCount.value = 0
        _status.value = "Inactivo"

        Log.d(TAG, "⏹️ MESH DESACTIVADO")
    }

    // ==================== WIFI DIRECT SERVICE DISCOVERY ====================

    private fun initWifiDirectWithServiceDiscovery() {
        wifiP2p = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
        channel = wifiP2p?.initialize(context, Looper.getMainLooper(), null)

        if (wifiP2p == null || channel == null) {
            Log.e(TAG, "❌ WiFi Direct no disponible")
            _status.value = "Sin WiFi Direct"
            return
        }

        // Receiver para eventos
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    WIFI_P2P_STATE_CHANGED_ACTION -> {
                        val state = intent.getIntExtra(EXTRA_WIFI_STATE, -1)
                        if (state == WIFI_P2P_STATE_ENABLED) {
                            Log.d(TAG, "✅ WiFi Direct habilitado")
                            scope.launch {
                                delay(500)
                                setupServiceDiscovery()
                            }
                        } else {
                            Log.d(TAG, "❌ WiFi Direct deshabilitado")
                            _status.value = "WiFi Direct off"
                        }
                    }

                    WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                        val netInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(EXTRA_NETWORK_INFO, android.net.NetworkInfo::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(EXTRA_NETWORK_INFO)
                        }

                        if (netInfo?.isConnected == true) {
                            p2pConnected = true
                            wifiP2p?.requestConnectionInfo(channel) { onP2pConnected(it) }
                        } else {
                            p2pConnected = false
                            Log.d(TAG, "📡 P2P desconectado")
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        // Iniciar service discovery
        scope.launch {
            delay(1000)
            setupServiceDiscovery()
        }
    }

    private fun setupServiceDiscovery() {
        Log.d(TAG, "📡 Configurando Service Discovery...")
        _status.value = "Configurando..."

        // 1. Primero limpiar servicios anteriores
        wifiP2p?.clearLocalServices(channel, object : ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "✅ Servicios anteriores limpiados")
                registerOrionService()
            }
            override fun onFailure(reason: Int) {
                Log.d(TAG, "⚠️ Clear services: ${reasonStr(reason)}")
                registerOrionService() // Intentar de todos modos
            }
        })
    }

    private fun registerOrionService() {
        // 2. Registrar nuestro servicio Orion
        val record = mapOf(
            "id" to myId.take(8),
            "name" to myName,
            "port" to PORT.toString(),
            "app" to "orion"
        )

        val serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(
            SERVICE_NAME,
            SERVICE_TYPE,
            record
        )

        wifiP2p?.addLocalService(channel, serviceInfo, object : ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "✅ Servicio Orion registrado")
                Log.d(TAG, "   $SERVICE_TYPE → $myName")
                setupServiceListeners()
            }
            override fun onFailure(reason: Int) {
                Log.e(TAG, "❌ Error registrando servicio: ${reasonStr(reason)}")
                _status.value = "Error registro"
                // Reintentar
                scope.launch {
                    delay(5000)
                    if (_isActive.value) setupServiceDiscovery()
                }
            }
        })
    }

    private fun setupServiceListeners() {
        // 3. Configurar listeners para cuando encontremos otros Orion

        // Listener para registros TXT (información del servicio)
        val txtListener = DnsSdTxtRecordListener { fullDomain, record, device ->
            val app = record["app"] ?: ""
            val peerId = record["id"] ?: ""
            val peerName = record["name"] ?: device.deviceName

            Log.d(TAG, "🔍 TXT Record: $fullDomain")
            Log.d(TAG, "   app=$app, id=$peerId, name=$peerName")

            if (app == "orion" && peerId != myId.take(8)) {
                Log.d(TAG, "════════════════════════════════════════")
                Log.d(TAG, "🎯 ¡ORION ENCONTRADO!")
                Log.d(TAG, "   Nombre: $peerName")
                Log.d(TAG, "   ID: $peerId")
                Log.d(TAG, "   Device: ${device.deviceAddress}")
                Log.d(TAG, "════════════════════════════════════════")

                orionDevices[device.deviceAddress] = device
                _status.value = "Encontrado: $peerName"
            }
        }

        // Listener para servicios encontrados
        val serviceListener = DnsSdServiceResponseListener { instanceName, registrationType, device ->
            Log.d(TAG, "🔍 Servicio: $instanceName ($registrationType)")

            if (instanceName.contains("Orion", ignoreCase = true) ||
                registrationType.contains("orion", ignoreCase = true)) {
                Log.d(TAG, "🎯 Servicio Orion detectado en ${device.deviceName}")
                orionDevices[device.deviceAddress] = device
            }
        }

        wifiP2p?.setDnsSdResponseListeners(channel, serviceListener, txtListener)

        // 4. Crear y añadir service request
        startServiceDiscovery()
    }

    private fun startServiceDiscovery() {
        // Quitar request anterior si existe
        serviceRequest?.let {
            wifiP2p?.removeServiceRequest(channel, it, null)
        }

        // Crear nuevo request para buscar servicios Orion
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance()

        wifiP2p?.addServiceRequest(channel, serviceRequest, object : ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "✅ Service request añadido")
                discoverServices()
            }
            override fun onFailure(reason: Int) {
                Log.e(TAG, "❌ Error añadiendo request: ${reasonStr(reason)}")
                scope.launch {
                    delay(3000)
                    if (_isActive.value) startServiceDiscovery()
                }
            }
        })
    }

    private fun discoverServices() {
        Log.d(TAG, "🔍 Buscando servicios Orion...")
        _status.value = "Buscando Orion..."

        wifiP2p?.discoverServices(channel, object : ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "✅ Búsqueda de servicios iniciada")

                // Verificar periódicamente si encontramos Orions
                scope.launch {
                    delay(5000)
                    checkAndConnectToOrions()
                }
            }
            override fun onFailure(reason: Int) {
                Log.e(TAG, "❌ Error buscando servicios: ${reasonStr(reason)}")
                _status.value = "Error búsqueda"

                // Reintentar
                scope.launch {
                    delay(5000)
                    if (_isActive.value && !p2pConnected) {
                        discoverServices()
                    }
                }
            }
        })

        // Re-discovery loop
        scope.launch {
            delay(30000)
            if (_isActive.value && !p2pConnected) {
                attemptedAddresses.clear()
                discoverServices()
            }
        }
    }

    private fun checkAndConnectToOrions() {
        if (!_isActive.value || p2pConnected) return

        val availableOrions = orionDevices.filter { !attemptedAddresses.contains(it.key) }

        Log.d(TAG, "📊 Orions disponibles: ${availableOrions.size}")

        if (availableOrions.isEmpty()) {
            Log.d(TAG, "   No hay Orions nuevos para conectar")
            _status.value = "Buscando Orion..."
            return
        }

        // Conectar al primer Orion disponible
        val (address, device) = availableOrions.entries.first()
        attemptedAddresses.add(address)

        connectToOrionDevice(device)
    }

    private fun connectToOrionDevice(device: WifiP2pDevice) {
        Log.d(TAG, "🔌 Conectando a Orion: ${device.deviceName}")
        _status.value = "Conectando..."

        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
            groupOwnerIntent = 0 // Preferir ser cliente
        }

        wifiP2p?.connect(channel, config, object : ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "✅ Solicitud de conexión enviada a ${device.deviceName}")
                Log.d(TAG, "   ⏳ Esperando confirmación del otro dispositivo...")
            }
            override fun onFailure(reason: Int) {
                Log.e(TAG, "❌ Error conectando: ${reasonStr(reason)}")
                _status.value = "Error conexión"

                // Reintentar con otro dispositivo después
                scope.launch {
                    delay(10000)
                    attemptedAddresses.remove(device.deviceAddress)
                    checkAndConnectToOrions()
                }
            }
        })
    }

    private fun onP2pConnected(info: WifiP2pInfo) {
        groupOwnerAddress = info.groupOwnerAddress?.hostAddress
        isGroupOwner = info.isGroupOwner

        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "📡 ¡CONEXIÓN P2P ESTABLECIDA!")
        Log.d(TAG, "   Soy host: $isGroupOwner")
        Log.d(TAG, "   IP host: $groupOwnerAddress")
        Log.d(TAG, "════════════════════════════════════════")

        _status.value = if (isGroupOwner) "Host activo" else "Conectado"

        // Conectar por TCP
        if (!isGroupOwner && groupOwnerAddress != null) {
            scope.launch {
                delay(2000)
                connectTcp(groupOwnerAddress!!, "p2p_host")
            }
        }
    }

    private fun reasonStr(r: Int) = when(r) {
        ERROR -> "ERROR"
        P2P_UNSUPPORTED -> "NO_SOPORTADO"
        BUSY -> "OCUPADO"
        NO_SERVICE_REQUESTS -> "SIN_REQUESTS"
        else -> "UNKNOWN($r)"
    }

    // ==================== UDP (BACKUP) ====================

    private suspend fun udpBroadcast() = withContext(Dispatchers.IO) {
        try {
            val socket = DatagramSocket().apply { broadcast = true }
            val msg = """{"t":"ORION","id":"$myId","n":"$myName","p":$PORT}"""

            Log.d(TAG, "📡 UDP Broadcast activo")

            while (_isActive.value) {
                val data = msg.toByteArray()
                listOf("255.255.255.255", "192.168.43.255", "192.168.49.255", "192.168.1.255").forEach {
                    try { socket.send(DatagramPacket(data, data.size, InetAddress.getByName(it), UDP_PORT)) } catch (e: Exception) {}
                }
                delay(3000)
            }
            socket.close()
        } catch (e: Exception) { Log.e(TAG, "UDP BC: ${e.message}") }
    }

    private suspend fun udpListen() = withContext(Dispatchers.IO) {
        try {
            val socket = DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                bind(InetSocketAddress(UDP_PORT))
            }

            Log.d(TAG, "👂 UDP Listen :$UDP_PORT")
            val buf = ByteArray(1024)

            while (_isActive.value) {
                try {
                    val pkt = DatagramPacket(buf, buf.size)
                    socket.soTimeout = 5000
                    socket.receive(pkt)

                    val data = String(pkt.data, 0, pkt.length)
                    val ip = pkt.address.hostAddress ?: continue

                    val json = JSONObject(data)
                    if (json.optString("t") != "ORION") continue

                    val peerId = json.getString("id")
                    if (peerId == myId || peers.containsKey(peerId)) continue

                    Log.d(TAG, "🛰️ UDP: ${json.optString("n")} @ $ip")
                    connectTcp(ip, peerId)
                } catch (e: SocketTimeoutException) {} catch (e: Exception) { delay(1000) }
            }
            socket.close()
        } catch (e: Exception) { Log.e(TAG, "UDP: ${e.message}") }
    }

    // ==================== TCP ====================

    private suspend fun startServer() = withContext(Dispatchers.IO) {
        try {
            server = ServerSocket(PORT).apply { reuseAddress = true }
            Log.d(TAG, "🖥️ Server :$PORT")

            while (_isActive.value) {
                try {
                    val c = server?.accept() ?: break
                    Log.d(TAG, "📥 Conexión: ${c.inetAddress.hostAddress}")
                    scope.launch { handlePeer(c) }
                } catch (e: Exception) { if (_isActive.value) delay(1000) }
            }
        } catch (e: Exception) { Log.e(TAG, "Srv: ${e.message}") }
    }

    private suspend fun connectTcp(ip: String, id: String) = withContext(Dispatchers.IO) {
        if (peers.containsKey(id) || peers.values.any { it.inetAddress?.hostAddress == ip }) return@withContext

        try {
            Log.d(TAG, "🔌 TCP → $ip")
            val s = Socket()
            s.connect(InetSocketAddress(ip, PORT), 10000)
            Log.d(TAG, "✅ TCP conectado: $ip")
            handlePeer(s)
        } catch (e: Exception) { Log.d(TAG, "❌ TCP: ${e.message}") }
    }

    private suspend fun handlePeer(socket: Socket) = withContext(Dispatchers.IO) {
        var id = socket.inetAddress.hostAddress ?: return@withContext

        try {
            val out = PrintWriter(socket.getOutputStream(), true)
            val inp = BufferedReader(InputStreamReader(socket.getInputStream()))

            out.println("""{"id":"$myId","n":"$myName"}""")
            socket.soTimeout = 10000
            inp.readLine()?.let { runCatching { id = JSONObject(it).optString("id", id) } }
            socket.soTimeout = 0

            if (peers.containsKey(id)) { socket.close(); return@withContext }

            peers[id] = socket
            updateCount()
            Log.d(TAG, "✅ PEER CONECTADO: $id")

            sync(out)

            while (_isActive.value && !socket.isClosed) {
                val line = inp.readLine() ?: break
                process(line, id)
            }
        } catch (e: Exception) {} finally {
            peers.remove(id)
            updateCount()
            runCatching { socket.close() }
        }
    }

    // ==================== MESSAGES ====================

    private fun process(data: String, from: String) {
        try {
            val j = JSONObject(data)
            when (j.optString("t")) {
                "M" -> {
                    val m = parse(j)
                    if (seenIds.contains(m.id)) return
                    seenIds.add(m.id); msgStore[m.id] = m; refresh(); saveMessages()

                    // PROPAGAR a todos los demás peers (excepto quien lo envió)
                    if (m.ttl > 0) {
                        val propagate = ser(m.copy(ttl = m.ttl - 1, hops = m.hops + 1))
                        send(propagate, from)
                    }
                    Log.d(TAG, "📨 Nuevo: ${m.text.take(30)}")

                    // 🔔 NOTIFICACIÓN: Avisar al usuario
                    val isForMe = m.to == null || m.to == myId // Broadcast o para mí
                    if (isForMe && m.from != myId) {
                        val decrypted = if (m.enc && m.to == myId) {
                            try { decrypt(m) } catch (e: Exception) { null }
                        } else if (!m.enc) {
                            m.text
                        } else null

                        EmergencyNotifications.showMeshNotification(
                            context = context,
                            message = m,
                            decryptedText = decrypted,
                            isForMe = true
                        )
                    }
                }
                // SYNC: Recibo lista de IDs del peer
                "SYNC_IDS" -> {
                    val theirIds = mutableSetOf<String>()
                    j.optJSONArray("ids")?.let { arr ->
                        for (i in 0 until arr.length()) theirIds.add(arr.getString(i))
                    }

                    // Enviar mensajes que YO tengo y EL NO tiene
                    val toSend = msgStore.values.filter { it.id !in theirIds }
                    if (toSend.isNotEmpty()) {
                        val arr = JSONArray()
                        toSend.forEach { arr.put(serFull(it)) }
                        sendTo(from, """{"t":"SYNC_MSGS","m":$arr}""")
                        Log.d(TAG, "📤 Enviando ${toSend.size} msgs que le faltan")
                    }

                    // Pedir mensajes que EL tiene y YO NO tengo
                    val myIds = msgStore.keys
                    val iNeed = theirIds.filter { it !in myIds }
                    if (iNeed.isNotEmpty()) {
                        val arr = JSONArray(iNeed)
                        sendTo(from, """{"t":"SYNC_REQ","ids":$arr}""")
                        Log.d(TAG, "📥 Pidiendo ${iNeed.size} msgs que me faltan")
                    }
                }
                // SYNC_REQ: Me piden mensajes específicos
                "SYNC_REQ" -> {
                    val requested = mutableListOf<MeshMessage>()
                    j.optJSONArray("ids")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val id = arr.getString(i)
                            msgStore[id]?.let { requested.add(it) }
                        }
                    }
                    if (requested.isNotEmpty()) {
                        val arr = JSONArray()
                        requested.forEach { arr.put(serFull(it)) }
                        sendTo(from, """{"t":"SYNC_MSGS","m":$arr}""")
                        Log.d(TAG, "📤 Respondiendo con ${requested.size} msgs solicitados")
                    }
                }
                // SYNC_MSGS: Recibo mensajes completos
                "SYNC_MSGS" -> {
                    var newCount = 0
                    val newMessages = mutableListOf<MeshMessage>()
                    j.optJSONArray("m")?.let { a ->
                        for (i in 0 until a.length()) {
                            val m = parse(a.getJSONObject(i))
                            if (!seenIds.contains(m.id)) {
                                seenIds.add(m.id)
                                msgStore[m.id] = m
                                newCount++
                                newMessages.add(m)

                                // Propagar a otros peers (tipo blockchain)
                                if (m.ttl > 0) {
                                    send(ser(m.copy(ttl = m.ttl - 1, hops = m.hops + 1)), from)
                                }
                            }
                        }
                        if (newCount > 0) {
                            refresh()
                            saveMessages()
                            Log.d(TAG, "✅ Sincronizados $newCount mensajes nuevos")

                            // 🔔 NOTIFICACIONES: Avisar de mensajes nuevos para mí
                            newMessages.filter { m ->
                                m.from != myId && (m.to == null || m.to == myId)
                            }.forEach { m ->
                                val decrypted = if (m.enc && m.to == myId) {
                                    try { decrypt(m) } catch (e: Exception) { null }
                                } else if (!m.enc) {
                                    m.text
                                } else null

                                EmergencyNotifications.showMeshNotification(
                                    context = context,
                                    message = m,
                                    decryptedText = decrypted,
                                    isForMe = true
                                )
                            }
                        }
                    }
                }
                // Legacy: soporte para versiones anteriores
                "S" -> {
                    j.optJSONArray("m")?.let { a ->
                        for (i in 0 until a.length()) {
                            val m = parse(a.getJSONObject(i))
                            if (!seenIds.contains(m.id)) { seenIds.add(m.id); msgStore[m.id] = m }
                        }
                        refresh(); saveMessages()
                    }
                }
            }
        } catch (e: Exception) { Log.e(TAG, "Process error: ${e.message}") }
    }

    // Iniciar sincronización blockchain: envío mis IDs
    private fun sync(out: PrintWriter) {
        val myIds = JSONArray(msgStore.keys.toList())
        out.println("""{"t":"SYNC_IDS","ids":$myIds}""")
        Log.d(TAG, "🔄 Sync iniciado con ${msgStore.size} IDs")
    }

    private fun sendTo(peerId: String, json: String) {
        peers[peerId]?.let { socket ->
            if (!socket.isClosed) {
                scope.launch(Dispatchers.IO) {
                    try { PrintWriter(socket.getOutputStream(), true).println(json) } catch (e: Exception) {}
                }
            }
        }
    }

    private fun send(json: String, except: String? = null) {
        peers.forEach { (id, s) ->
            if (id != except && !s.isClosed)
                scope.launch(Dispatchers.IO) { try { PrintWriter(s.getOutputStream(), true).println(json) } catch (e: Exception) {} }
        }
    }

    fun sendPublic(text: String) {
        val m = MeshMessage(UUID.randomUUID().toString().take(10), myId, myName, null, null, text.take(500), null, false, System.currentTimeMillis(), 50, 0)
        seenIds.add(m.id); msgStore[m.id] = m; refresh(); send(ser(m)); saveMessages()
    }

    fun sendPrivate(text: String, toId: String, toKey: String) {
        val pk = EmergencyCrypto.parsePublicKey(toKey) ?: return
        val e = EmergencyCrypto.encryptMessage(context, text.take(500), pk)
        val m = MeshMessage(UUID.randomUUID().toString().take(10), myId, myName, myKey, toId, e.data, e.iv, true, System.currentTimeMillis(), 50, 0)
        seenIds.add(m.id); msgStore[m.id] = m; refresh(); send(ser(m)); saveMessages()
    }

    fun decrypt(m: MeshMessage): String {
        if (!m.enc) return m.text
        if (m.to != myId) return "🔒"
        return try { EmergencyCrypto.decryptMessage(context, EncryptedPayload(m.iv ?: "", m.text, m.fromKey ?: "")) ?: "❌" } catch (e: Exception) { "❌" }
    }

    fun connectManual(ip: String) = scope.launch { connectTcp(ip.trim(), "manual") }

    // ==================== UTILS ====================

    private fun updateCount() {
        _peerCount.value = peers.size
        _status.value = if (peers.isEmpty()) "Buscando Orion..." else "Conectado (${peers.size})"
    }

    private fun refresh() { _messages.value = msgStore.values.sortedByDescending { it.ts } }

    private suspend fun maintenance() {
        while (_isActive.value) {
            delay(60000) // Cada minuto

            // Solo borrar mensajes muy antiguos (30 días)
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            val oldCount = msgStore.size
            msgStore.entries.removeIf { it.value.ts < thirtyDaysAgo }
            val removed = oldCount - msgStore.size
            if (removed > 0) {
                Log.d(TAG, "🧹 Limpiados $removed mensajes antiguos (>30 días)")
                saveMessages()
            }

            // Limpiar seenIds si supera 50000 (mantener espacio)
            if (seenIds.size > 50000) {
                seenIds.clear()
                msgStore.keys.forEach { seenIds.add(it) }
                Log.d(TAG, "🧹 SeenIds limpiado y reconstruido")
            }
        }
    }

    private fun ser(m: MeshMessage) = JSONObject().apply {
        put("t", "M"); put("id", m.id); put("f", m.from); put("fn", m.fromName)
        put("fk", m.fromKey ?: ""); put("to", m.to ?: ""); put("tx", m.text)
        put("iv", m.iv ?: ""); put("e", m.enc); put("ts", m.ts); put("ttl", m.ttl); put("h", m.hops)
    }.toString()

    // Serialización completa para sync (sin wrapper "t":"M")
    private fun serFull(m: MeshMessage) = JSONObject().apply {
        put("id", m.id); put("f", m.from); put("fn", m.fromName)
        put("fk", m.fromKey ?: ""); put("to", m.to ?: ""); put("tx", m.text)
        put("iv", m.iv ?: ""); put("e", m.enc); put("ts", m.ts); put("ttl", m.ttl); put("h", m.hops)
    }

    private fun parse(j: JSONObject) = MeshMessage(
        j.getString("id"), j.getString("f"), j.optString("fn", "?"),
        j.optString("fk").takeIf { it.isNotEmpty() }, j.optString("to").takeIf { it.isNotEmpty() },
        j.getString("tx"), j.optString("iv").takeIf { it.isNotEmpty() },
        j.optBoolean("e"), j.getLong("ts"), j.optInt("ttl", 50), j.optInt("h", 0)
    )

    // Guardar TODO el historial (blockchain local)
    private fun saveMessages() {
        try {
            val a = JSONArray()
            msgStore.values.forEach { a.put(serFull(it)) }
            File(context.filesDir, "mesh_msg.json").writeText(a.toString())
            Log.d(TAG, "💾 Guardados ${msgStore.size} mensajes")
        } catch (e: Exception) { Log.e(TAG, "Save error: ${e.message}") }
    }

    // Cargar todo el historial al iniciar
    private fun loadMessages() {
        try {
            val f = File(context.filesDir, "mesh_msg.json")
            if (!f.exists()) return
            val a = JSONArray(f.readText())
            for (i in 0 until a.length()) {
                val m = parse(a.getJSONObject(i))
                msgStore[m.id] = m
                seenIds.add(m.id)
            }
            refresh()
            Log.d(TAG, "📂 Cargados ${msgStore.size} mensajes del historial")
        } catch (e: Exception) { Log.e(TAG, "Load error: ${e.message}") }
    }
}

data class MeshMessage(val id: String, val from: String, val fromName: String, val fromKey: String?, val to: String?, val text: String, val iv: String?, val enc: Boolean, val ts: Long, val ttl: Int, val hops: Int)

enum class MessageType { SOS, IM_OK, LOCATION, TEXT, NEED_HELP, SAFE_ZONE }
data class EmergencyMessage(val id: String, val type: MessageType, val senderId: String, val recipientId: String?, val payload: MessagePayload, val timestamp: Long, val ttl: Int, val hopCount: Int, val signature: String)
data class MessagePayload(val encrypted: Boolean, val data: String, val senderKey: String, val iv: String? = null)