package com.example.motioncontrollerapp

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class WebSocketClient(uri: URI) : WebSocketClient(uri) {
    override fun onOpen(handshakedata: ServerHandshake?) {
        // Połączenie nawiązane
        println("Połączenie otwarte")
    }

    override fun onMessage(message: String?) {
        // Odbieranie wiadomości
        println("Odebrano wiadomość: $message")
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        // Połączenie zamknięte
        println("Połączenie zamknięte: $reason")
    }

    override fun onError(ex: Exception?) {
        // Obsługa błędów
        println("Błąd: ${ex?.message}")
    }
}
