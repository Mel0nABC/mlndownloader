document.addEventListener("DOMContentLoaded", () => {
    connect();
});

const stompClient = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/ws-connect'
});

stompClient.onConnect = (frame) => {
    setConnected(true);
    console.log('Connected: ' + frame);
    stompClient.subscribe('/api/speed', (jsonString) => {
        const data = JSON.parse(jsonString.body);

        const files = document.querySelector("#files");

        files.innerHTML = "";

        data.forEach(element => {
            const p = document.createElement("p");
            console.log(element)
            p.textContent = element.filePath + " - " + element.downloadedBytes;

            files.append(p);
        });



    });
};

stompClient.onWebSocketError = (error) => {
    console.error('Error with websocket', error);
};

stompClient.onStompError = (frame) => {
    console.error('Broker reported error: ' + frame.headers['message']);
    console.error('Additional details: ' + frame.body);
};

function setConnected(connected) {
    console.log("CONNECTED: " + connected)
}

function connect() {
    stompClient.activate();
}