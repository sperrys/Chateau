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

## The WS portion of the Server takes JSON messsages at the /ws url and then based 
upon the message, acts accordingly. The messages and their responses are documented below.
For message types and message arguements, the keyword format must match exactly.

### Message Types:

**Register Client:** 

type: 1 

Arguments
	- username

Response:
	- If successful, `"status": 200`
	- if failure, `"status": 400` 


**Message Client:**

type: 3

Arguments:
	- `"clients"` usernames for who the message is going to
	- `"content"` content of the actual message body

Response:
	- If successful, `"status": 200`
	- if failure, `"status": 400` 


**Clients List:** 

type: 4 

Arguments
	- None
Response:
	- If successful, `"status": 200` and an array of `"clients"` usernames. 
	- if failure, `"status": 400` 


# To Use The Python Test Client
### In order to use the python test client, make sure that the server is running locally with `python server.py`. Then
in a seperate window, start the client with `python test_client.py`. The test client is configured to read in a json message from file eg. `message.json` and then send that json file to the server as a message. The test client then reads from the server and prints to console. The client will then loop back and listen for another file message. **Note that the client is currently configured to only read once, so if a message sent to the server prompts multiple messages back to the client, only one message will show. (Assumes port localhost:8888)***
