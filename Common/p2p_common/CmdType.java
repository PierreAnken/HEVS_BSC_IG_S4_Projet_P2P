package p2p_common;

public enum CmdType {
	sendMyListenerInfo, // client: register with server
	sendAllFileList, 	// server: send all available files to client
	sendMyFileList,  	// client: send file list to server
	getAllFileList,  	// client: ask server for file list
	getFile, 			// client: ask another client for file
	sendFile, 			// client: send file to another client
	refresh,			// user: refresh file list from server
	dl,					// user: ask to download a file
	help,				// user&admin: ask command list
	getClientWithFile,  // client: ask the server for active clients with this file
	sendClientWithFile, // server: send to client active clients with this file
	fileTransfertInfo,	// client: send info to other client about transfer
	filePart,			// client: send a file part to another client
	stopCom	,			// client & server: kill the socket listener if an exception occurs
	online,				// server: admin ask the list of online client
	disconnect,			// server: admin ask for client disconnection
	info				// server: admin ask for client informations
}
