const WebSocket = require('ws');

const server = new WebSocket.Server({ port: 8080 });

server.on('connection', (socket) => {
    console.log('Nowe połączenie');

    socket.on('message', (message) => {
        console.log(`Odebrano wiadomość: ${message}`);
        // Odpowiedź do klienta
        socket.send(`Otrzymałem: ${message}`);
    });

    socket.on('close', () => {
        console.log('Połączenie zamknięte');
    });

    socket.on('error', (error) => {
        console.error('Błąd: ', error);
    });
});

console.log('Serwer WebSocket uruchomiony na ws://localhost:8080');
