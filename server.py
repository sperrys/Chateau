import tornado.ioloop
import tornado.web
import tornado.websocket
import json
import os

from chatclient import ChatClient
from chat import Chat

from tornado.options import define, options, parse_command_line

define("port", default=5000, help="run on the given port", type=int)


options.port = int(os.environ.get('PORT', 5000))

clients = []
chats = []

class IndexHandler(tornado.web.RequestHandler):
    @tornado.web.asynchronous
    def get(self):
        self.render("index.html")

class CertRequestHandler(tornado.web.RequestHandler):
    @tornado.web.asynchronous
    def get(self):
        self.write('04BH71eULyDvP9YR8dSmFXBvJgADOpnA11Jz1yX-hVY.VC6txwIMOMmKlkjWLi-iG47yPMBGmPSp3-r_5m8IY34')

class WebSocketHandler(tornado.websocket.WebSocketHandler):

    # When A Web Socket Connection Has Been Opened 
    def open(self):
        print("WebSocket opened")
        print("New Client Initializing...")

        c = ChatClient(self)
        clients.append(c)

    def on_message(self, message):
        print ("Client Sent: ", message)
        # Get Message From Socket 
        # Pass Off to Message Handler
        try: 
            msg = json.loads(message)
            MessageHandler(self, msg)

        except Exception as e: 
            print("Poorly formatted Request Message")
            print("Sending Error Response...")

            response = { "status": 400 }
            self.write_message(json.dumps(response))


    # When a web socket connection has closed,
    # Remove the client from list of clients
    def on_close(self):
        print("WebSocket closed")
        print("Removing Client")
        RemoveClientWSock(self)


def RemoveClientWSock(sock):
    for c in clients:
        if c.sock == sock:
            clients.remove(c)

def GetClientWSock(sock):
    for c in clients:
        if c.sock == sock:
            return c

def MessageHandler(sock, msg):

    # Try To Parse Message Type and Handle 
    # Accordingly, otherwise send back error

    try: 
        msgType = msg["type"]
        print ("Server Got Message Type: ", msgType)

        if msgType == "RegisterRequest":
            RegisterRequestHandler(sock, msg)
        elif msgType == "GroupMessageInitRequest":
            GroupMessageInitHandler(sock, msg)
        elif msgType == "GroupMessageRequest":
            GroupMessageRequestHandler(sock, msg)
        elif msgType == "ClientListRequest":
            ClientListRequestHandler(sock, msg)
        elif msgType == "RandomMessageRequest":
            RandomMessageRequestHandler(sock, msg)
        elif msgType == "SingleMessageRequest":
            SingleMessageRequestHandler(sock, msg)
        else: 
            print("Not a valid Message Type")
            print("Sending Error Response...")

            response = { 
                         "type" : "ErrorResponse",
                         "status": 400
                       }
            sock.write_message(json.dumps(response))

    except Exception as e: 
        print(e)
        print("Sending Error Response...")
        response = { 
                     "type" : "ErrorResponse", 
                     "status": 400 
                   }

        sock.write_message(json.dumps(response))



def RegisterRequestHandler(sock, msg):
    print("Register Request")
    name = msg["username"]

    # Make Sure Unique Username
    for l in clients:
        if name == l.username:
            response = { 
                        "type"  : "RegisterResponse",
                        "status": 302 
                        }
            sock.write_message(json.dumps(response))
            return 

    # Otherwise Try to Register     
    for c in clients:
        if c.sock == sock:
            print("Found Succesful Connection")
            print("Client Registering...")
            
            # If not registered before,
            # Send back a c lient List
            if not c.registered:
                c.username = name
                c.registered = True
                ClientListRequestHandler(sock, msg)
                return 
            # Otherwise send back Auth Issue 
            else:
                print("Client is already Registered: ", c.username)
                print("Sending Error Response...")
                response = { 
                            "type"  : "ErrorResponse",
                            "status": 302 
                            }
                sock.write_message(json.dumps(response))
                return 

    
    # If the client cannot be found, send an error
    response = { 
                "type"  : "ErrorResponse", 
                "status": 301 
                }
    sock.write_message(json.dumps(response))


def GetChat(chatname):
    for c in chats:
        if (c.chatname == chatname):
            return c
    return None


def GetClientWName(name):
    for c in clients: 
        if c.username == name:
            return c
    return None
    

def SingleMessageRequestHandler(sock, msg):
    print("Single Message Request")

    c = GetClientWSock(sock)
    print("Client who sent Message: ", c.username)

    if c.registered:
        recipient = msg["recipient"]
        content = msg["content"]

        r = GetClientWName(recipient)

        if r != None:
            response = {
                "type"  : "SingleMessageResponse",
                "status": 200,
                "sender": c.username,
                "content": content
            }
            r.sock.write_message(json.dumps(response))
            c.sock.write_message(json.dumps({"type": "SingleMessageResponse", 
                                             "status": 200})) 
    else:
        RemoveClientWSock(c)


def GroupMessageInitHandler(sock, msg):
    print("GroupMessageInitRequest")

    c = GetClientWSock(sock)

     # If the client isn't registered, remove them from the list
    if c.registered:
        print("Client Who Sent Message: ", c.username)

        name = msg["chatname"]
        recipients = msg["recipients"]
        content = msg["content"]
        chat_recipients = [c]

        for r in recipients:
            new_r  = GetClientWName(r)
            chat_recipients.append(new_r)

        new_chat = Chat(name, recipients)
        chats.append(new_chat)
        response = {
                    "type": "GroupMessageRecv", 
                    "status": 201, 
                    "sender": c.username, 
                    "content": content
                    }
        new_chat.SendMessage(response, c)
        c.sock.write_message(json.dumps({
                                "type": "GroupMessageInitResponse",
                                "status": 201
                            }))

    else: 
        RemoveClientWSock(sock)



# Currently Fails Silently on Whether A Person was Found or Not
def GroupMessageRequestHandler(sock, msg):
    print("Group Message Request")

    c = GetClientWSock(sock)
    print("Client Who Sent Message: ", c.username)

    # If the client isn't registered, remove them from the list
    if c.registered:
        chatname = msg["chatname"]
        content = msg["content"]

        response =  { 
                      "type"   : "GroupMessageRecv",
                      "chatname": chatname,
                      "status" : 200, 
                      "type" : 3,
                      "sender" : c.username,
                      "content": content 
                    }

        chat = GetChat(chatname)
        if chat != None:
            print("Sending Response...")
            chat.SendMessage(response, c)
            c.sock.write_message(json.dumps({ 
                                    "type": "GroupMessageResponse",
                                    "status": 200
                                }))
            chatname = msg["chatname"]
        content = msg["content"]
    else: 
        RemoveClientWSock(sock)


def ClientListRequestHandler(sock, msg):
    print("Client List Request")

    c = GetClientWSock(sock)

    if c.registered:
        # Make List of usernames for clients
        usernames = []
        for c in clients: 
            if c.username != "":
                usernames.append(c.username)

        response = { 
                     "type"  : "ClientListResponse",
                     "status": 200, 
                     "clients": usernames
                   }
        print("Sending Response...")
        sock.write_message(json.dumps(response))

    else: 
        print("Sending Error Response...")
        response = { 
                    "type"  : "ErrorResponse",
                    "status": 302 
                    }
        sock.write_message(json.dumps(response))
        RemoveClientWSock(sock)



def RandomMessageRequestHandler(sock, msg):
    print("Random Message Request")

    try:
        c = GetClientWSock(sock)
        
        new_friend = sample(clients, 1)
        while new_friend.username == c.username:
            new_friend = sample(clients, 1)

        response = {
                    "type" : "RandomMessageIntiation",
                    "status" : 200,
                    "sender" : c.username,
                    "content": msg["content"]
                   }

        print("Sending Response...")
        new_friend.sock.write_message(json.dumps(response))
        c.write_message(json.dumps(
                                    {
                                        "type" : "RandomMessageResponse",
                                        "status": 200,
                                        "client": new_friend.username
                                    }))

    except Exception as e:
        print("Sending Error Response")
        response = { 
                    "Error Response"
                    "status": 400 

                    }
        sock.write_message(json.dumps(response)) 




app = tornado.web.Application([
    (r'/', IndexHandler),
    (r'/ws', WebSocketHandler),
    (r'/.well-known/acme-challenge/04BH71eULyDvP9YR8dSmFXBvJgADOpnA11Jz1yX-hVY/', CertRequestHandler)
])

if __name__ == '__main__':
    parse_command_line()
    app.listen(options.port)
    tornado.ioloop.IOLoop.instance().start()

