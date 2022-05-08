const worker = window.perspective.worker();
const viewer = document.querySelector("perspective-viewer");
viewer.toggleConfig();

let table;

async function set(title, data) {
    if (title && title.length)
        window.document.title = title;
    table && table.clear();
    table = await worker.table(data);
    await viewer.load(table);
}

async function update(data) {
    await table.update(data);
}

const socket = new WebSocket(window.location.origin.replace("http", "ws"));
socket.onmessage = async (msg) => {
    console.log("socket.onmessage", msg);
    const id = msg.data.substr(0, 8);
    await eval(msg.data.substr(8));
    socket.send(id + "OK");
}
setInterval(() => {
    socket.send("        PING");
}, 45000);
