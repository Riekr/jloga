import perspective from "./perspective.js";

const worker = perspective.worker();
const viewer = document.querySelector("perspective-viewer");
viewer.toggleConfig();

let table, u;

async function s(title, data) {
  if (title && title.length)
    window.document.title = title;
  table && table.clear();
  table = await worker.table(data);
  u = table.update
  await viewer.load(table);
}

const socket = new WebSocket(window.location.origin.replace("http", "ws"));
socket.onmessage = async (msg) => {
  // console.log("socket.onmessage", msg);
  await eval(msg.data.substr(8));
  socket.send(msg.data.substr(0, 8) + "OK");
}
setInterval(() => {
  socket.send("        PING");
}, 45000);
