# Chateau

This is a Repository for networks project 1.

## Backend Server Instructions: 

1) Make sure you have `pip` installed
2) type `pip install virtualenv` , this will install a python project package manager. 
3) Once virtualenv is installed, type `virtualenv -p python3 env` this will create a local environment called "env"
4) Activate that local environment with `source env/bin/activate`
5) Now once you're sandboxed in the environment, install dependencies with `pip install -r requirements.txt` 
6) now run `python server.py`, this will start the server on localhost:8888



# Server API Details

#### The WS portion of the Server takes JSON messsages at the /ws url and then based upon the message, acts accordingly. The messages and their responses are documented below. For message types and message arguements, the keyword format must match exactly.

### Message Types:

**Register Client:** 

"type": "RegisterRequest"

Arguments
	- `"username"`
	- `"password"`

Response:
	- If successful, `"type": "RegisterResponse", "status": 200`
	- if generic failure, `"type": "ErrorResponse", status": 400` 
	- if no socket (this shouldn't happen), `"type": "ErrorResponse", status": 300` 
	- if user is already registered, `"type": "ErrorResponse", status": 302` 
	- if username has already been taken, `"type": "ErrorResponse", status": 302` 

**Start a New Group Message**

"type": "GroupMessageInitRequest"

Arguments:
	- `"recipients"` usernames of those to be in the chat
	- `"chatname"` unique chatname

Response:
	- If successful, `"type": "GroupMessageInitResponse", "status": 200` goes to the person who sent the request
	- If successful, `"type": "GroupMessageInitResponse", "status": 201` goes to all the recipients
	- if generic failure, `"type": "ErrorResponse", status": 400` 
	- if failure is that the client making the request is not registered, `"type": "ErrorResponse", status": 301`
	- if failure is that the chatname is not unique, `"type": "ErrorResponse", status": 301`


**Send a Message to a Chat or Client**

"type": "GroupMessageRequest"

Arguments: 
	- "recipient"
	- "content"

Response:
	- If sucessful, `"type": "MessageSendResponse"` and `"status": 200` goes to the person who sent the message
	- If sucessful, all clients in chat will get a `"type": "GroupMessageRecv"`, `"content": "food"`
	- If generic failure, `"type": "ErrorResponse", "status": 400`


**Message From Other Person in Group Chat**

"type" : "GroupMessageRecv"

arguments: 
	"status": 201 (if chat was newly created), 200 if afterthat
	"sender": username of the person who sent the message
	"content": the actual content of the new message in the chat



**Clients List:** 

"type": "ClientListRequest"

Arguments
	- None
Response:
	- If successful, `"type": "ClientListResponse", "status": 200` and an array of `"clients"` usernames who are online. 
	- if failure, `"type": "ErrorResponse", "status": 400` 


**Random Message:** 

"type" : "RandomMessageRequest"

Arguments
	- `"content"`
Response:
	- If successful, `"type": "RandomMessageResponse", "status": 200` and a random `"clients"` username. 
	- if failure, `"type": "ErrorResponse", "status": 400` 

**Initiate Single Conversation**

"type": "SingleMessageRequest"

Arguments
	- `"recipient"`
	- `"content"`

Response:
	- If successful, `"type": "SingleMessageRecvResponse", "status": 200`, and `"sender"` and `"content"` goes to whoever gets the message. A "SingleMessageSendResponse" with `"status": 200` is sent to whoever sent the original single message request.
	- If failure, `"type": ErrorResponse, "status": 400`




# To Use The Python Test Client
#### In order to use the python test client, make sure that the server is running locally with `python server.py`. Then
in a seperate window, start the client with `python test_client.py`. The test client is configured to read in a json message from file eg. `message.json` and then send that json file to the server as a message. The test client then reads from the server and prints to console. The client will then loop back and listen for another file message. **Note that the client is currently configured to only read once, so if a message sent to the server prompts multiple messages back to the client, only one message will show. (Assumes port localhost:8888)

# To Test the Potential Tufts Auth Functionality
Run the script `python ldap_client.py` and try your utln and password. This is done over tls and you can check the source, nothing is saved"


## Current Behaviours
1) You must succesfully have a register client request (1) before making any other requests
