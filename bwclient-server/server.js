const WebSocket = require('ws');

const wss = new WebSocket.Server({ port: 3500 });

const connectedClients = new Map();
const recentGameEvents = new Set();

console.log('BWClient (Port: 3500)');

function broadcast(data) {
    const messageString = JSON.stringify(data);
    wss.clients.forEach(client => {
        if (client.readyState === WebSocket.OPEN) {
            client.send(messageString);
        }
    });
}

function broadcastUserList() {
    const userList = Array.from(connectedClients.values());
    const userListMessage = {
        type: 'user_list_update',
        users: userList
    };
    broadcast(userListMessage);
}

wss.on('connection', ws => {
    console.log('Client connected.');

    ws.on('message', message => {
        try {
            const data = JSON.parse(message);

            if (data.type === 'join' && data.player) {
                connectedClients.set(ws, data.player);
                console.log(`Client data: ${data.player}`);
                broadcast(data);
                broadcastUserList();
                return;
            }

            if (data.type === 'message' || data.type === 'party_invite' || data.type === 'health_update' || data.type === 'resource_update' || data.type === 'tactic_vote') {
                broadcast(data);
                return;
            }

            let eventId;
            if (data.entityId) {
                eventId = `${data.type}_${data.entityId}`;
            } else {
                const timestampBucket = Math.floor(Date.now() / 1500);
                eventId = `${data.type}_${data.player}_${timestampBucket}`;
            }

            if (recentGameEvents.has(eventId)) {
                return;
            }

            recentGameEvents.add(eventId);
            setTimeout(() => {
                recentGameEvents.delete(eventId);
            }, 2000);

            broadcast(data);

        } catch (error) {
            console.error('Failed to process message:', message, error);
        }
    });

    ws.on('close', () => {
        if (connectedClients.has(ws)) {
            const disconnectedPlayer = connectedClients.get(ws);
            connectedClients.delete(ws);
            console.log(`Client ${disconnectedPlayer} disconnected.`);
            broadcastUserList();
        } else {
            console.log('Unknown client disconnected.');
        }
    });
});