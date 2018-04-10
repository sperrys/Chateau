

# server.py 
# Written by Spencer Perry 
# 4/10/18
# Comp 112 Project 


import json
import os

import tornado.ioloop
from  tornado.ioloop import PeriodicCallback
import tornado.web
import tornado.websocket
import tornado.httpserver

from chatclient import ChatClient, Clients 
from chat import Chat
from response import Response

from ldap_client import TuftsAuth
from tornado.options import define, options, parse_command_line



from timeoutservice import TimeoutWebSocketService

define("port", default=5000, help="run on the given port", type=int)


options.port = int(os.environ.get('PORT', 5000))

clients = Clients()

chats = []

class IndexHandler(tornado.web.RequestHandler):
    @tornado.web.asynchronous
    def get(self):
        self.render("index.html")

class CertRequestHandler(tornado.web.RequestHandler):
    @tornado.web.asynchronous
    def get(self):
        self.write('JYcn_tukfGzSlJDAeUkqCbWZJEGG02TBV5_QR-08H34.VC6txwIMOMmKlkjWLi-iG47yPMBGmPSp3-r_5m8IY34')
        self.finish()

class WebSocketHandler(tornado.websocket.WebSocketHandler):
    #def prepare(self):
    #    self.timeout_service = TimeoutWebSocketService(self, timeout=(1000*60)) 
    #    self.timeout_service.refresh_timeout()

    # When A Web Socket Connection Has Been Opened 
    def open(self):
        print("WebSocket opened")
        print("New Client Initializing...")
        #PeriodicCallback(self.keep_alive, 30000).start()

        c = ChatClient(self)
        clients.add(c)

    def on_message(self, message):
        print ("Server Got Message: ", message)
        #self.timeout_service.refresh_timeout()

        # Get Message From Socket 
        # Pass Off to Message Handler
        try: 
            msg = json.loads(message)
            MessageHandler(self, msg)
        except Exception as e:
            print(e) 
            self.write_message(Response("ErrorResponse", 400).jsonify())

    def keep_alive(self):
        self.ping(json.dumps({"type": "KeepAlive"}))


    # When a web socket connection has closed,
    # Remove the client from list of clients
    def on_close(self):
        print("WebSocket closed")
        print("Removing Client")
        #self.timeout_service.clean_timeout()
        RemoveClientWSock(self)


def RemoveClientWSock(sock):
    for c in clients:
        if c.sock == sock:
            clients.remove(c)

def GetClientWSock(sock):
    for c in clients:
        if c.sock == sock:
            return c


# Try To Parse Message Type and Handle 
# Accordingly, otherwise send back error
def MessageHandler(sock, msg):

    try: 
        msgType = msg["type"]
        print ("Server Got Message Type: ", msgType)

        if msgType == "RegisterRequest":
            RegisterRequestHandler(sock, msg)
        elif msgType == "GroupMessageInitRequest":
            GroupMessageInitHandler(sock, msg)
        elif msgType == "MessageRequest":
            GroupMessageRequestHandler(sock, msg)
        elif msgType == "ClientListRequest":
            ClientListRequestHandler(sock, msg)
        elif msgType == "RandomMessageRequest":
            RandomMessageRequestHandler(sock, msg)
        # TODO, turn this into just message request    
        #elif msgType == "SingleMessageRequest":
        #    SingleMessageRequestHandler(sock, msg)
        else: 
            sock.write_message(Response(400).jsonify())

    except Exception as e: 
        print(e)
        sock.write_message(Response(400).jsonify())


def RegisterRequestHandler(sock, msg):
    print("Register Request")

    try: 
        name = msg["username"]
        pw = msg["password"]

        print(name)
        print(pw)
        
        #auth = TuftsAuth(name, pw)
        auth = True
        
        if auth: 
            # Make Sure Unique Username
            for l in clients:
                if name == l.username:
                    sock.write_message(Response(302).jsonify())
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
                        response = Response("RegisterResponse", 200)
                        sock.write_message(response.jsonify())
                        return 
                    # Otherwise send back Auth Issue 
                    else:
                        print("Client is already Registered: ", c.username)
                        sock.write_message(Response("ErrorResponse", 302).jsonify())
                        return 
        else: 
            sock.write_message(Response("ErrorResponse", 301).jsonify())

    except Exception as e:
        print(e)
        sock.write_message(ErrorResponse(400).jsonify())


# Return Chat Based On Chat Name
def GetChat(chatname):
    for c in chats:
        if (c.chatname == chatname):
            return c
    return None

# Return Client Based on Client Name
def GetClientWName(name):
    for c in clients: 
        if c.username == name:
            return c
    return None
    

def SingleMessageRequestHandler(sock, msg):
    print("Single Message Request")

    try:
        c = GetClientWSock(sock)
        print("Client who sent Message: ", c.username)

        if c.registered:
            recipient = msg["recipient"]
            content = msg["content"]

            r = GetClientWName(recipient)

            if r != None:
                response = {
                    "type"  : "SingleMessageRecvResponse",
                    "status": 200,
                    "sender": c.username,
                    "content": content
                }

                # Write Message to Recipient and ACK the Sender
                print("Forwarding Single Message, Sending ACK")
                r.sock.write_message(json.dumps(response))
                c.sock.write_message(json.dumps({"type": "SingleMessageSendResponse", 
                                                 "status": 200})) 
            else:
                c.sock.write_message(ErrorResponse(302).jsonify())
        else:
            c.sock.write_message(ErrorResponse(301).jsonify())
    except Exception as e: 
        c.sock.write_message(ErrorResponse(400).jsonify())


def GroupMessageInitHandler(sock, msg):
    print("GroupMessageInitRequest")

    try: 
        c = GetClientWSock(sock)

        # If the client isn't registered, remove them from the list
        if c.registered:
            print("Client Who Sent Message: ", c.username)

            name = msg["chatname"]
            recipients = msg["recipients"]
            print(recipients)

            # Initialize Chat wit person who made request
            chat_recipients = [c]

            # Create New Chat, add all recipients to it.
            for r in recipients:
                print("Looking for ", r)
                new_r  = GetClientWName(r)
                if new_r == None:
                    err = ErrorResponse(404)
                    c.sock.write_message(err.add_pair("message", "At least one client doesn't exist"))
                    return
                else:
                    chat_recipients.append(new_r)
            
            # After Looping through all recipients, create chat
            new_chat = Chat(name, chat_recipients)
            chats.append(new_chat)

            # Send ACK back to group chat creator
            c.sock.write_message(json.dumps({
                            "type": "GroupMessageInitResponse",
                            "status": 200
                        }))
            # Send notification of creation to all others in chat
            new_chat.SendMessage(json.dumps({
                            "type": "GroupMessageInitResponse",
                            "status": 201,
                            "chatname" : name
                        }), c)
        else: 
            sock.write_message(ErrorResponse(301).jsonify())
    except Exception as e:
        print (e)
        sock.write_message(ErrorResponse(400).jsonify())




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
                      "chatname" : chatname,
                      "status" : 200, 
                      "sender" : c.username,
                      "content" : content 
                    }

        chat = GetChat(chatname)
        if chat != None:
            print("Sending Response...")
            chat.SendMessage(response, c)
            c.sock.write_message(json.dumps({ 
                                    "type": "GroupMessageResponse",
                                    "status": 200
                                }))
    else: 
         sock.write_message(ErrorResponse(301).jsonify())


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
        sock.write_message(ErrorResponse(301).jsonify())



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
        sock.write_message(ErrorResponse(400).jsonify())


app = tornado.web.Application([
    (r'/', IndexHandler),
    (r'/ws', WebSocketHandler),
    (r'/.well-known/acme-challenge/JYcn_tukfGzSlJDAeUkqCbWZJEGG02TBV5_QR-08H34', CertRequestHandler)
])

if __name__ == '__main__':
    parse_command_line()
    http_server = tornado.httpserver.HTTPServer(app)
    http_server.listen(options.port)
    tornado.ioloop.IOLoop.instance().start()
    print("Starting Server")

