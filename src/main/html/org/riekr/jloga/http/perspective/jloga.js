import perspective from "./perspective.js";

const worker = perspective.worker();
const viewer = document.querySelector("perspective-viewer");
viewer.toggleConfig();

let table;
const socket = new WebSocket(window.location.origin.replace("http", "ws"));
socket.onmessage = async (msg) => {
//	console.log("socket.onmessage", msg);
	switch(msg.data.charAt(8)) {
		case 't':
	    window.document.title = msg.data.substring(9);
			break;
		case 's':
		  table && table.clear();
		  table = await worker.table(JSON.parse(msg.data.substring(9)));
		  await viewer.load(table);
			break;
		case 'u':
			table.update(JSON.parse(msg.data.substring(9)));
			break;
		default:
			alert('invalid command ' + msg.data.charAt(0));
		  socket.send(msg.data.substr(0, 9));
			return;
	}
  socket.send(msg.data.substr(0, 8) + "K");
}
setInterval(() => {
  socket.send("        PING");
}, 45000);
