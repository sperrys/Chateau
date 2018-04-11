

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

        #auth = TuftsAuth(name, pw)
        auth = True
        
        if auth: 
            # Make Sure Unique Username
            if  clients.unique_username(name):
                  
                # Otherwise Try to Register     
                c = clients.find_w_sock(sock)
                if c != None:
                    if c.registered:

                    else:
                        c.register(name)    
                
                    # Otherwise send back Auth Issue 
                else:
                     print("Client is already Registered: ", c.username)
                     sock.write_message(Response("ErrorResponse", 302).jsonify())
            else: 
                 # Send Error For Non Unique UserName
                 sock.write_message(Response("ErrorResponse", 302).jsonify())
        else: 
            # Send Error For Bad Auth
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
        c = clients.find_w_sock(sock)

        # If the client isn't registered, remove them from the list
        if c.registered:
            print("Client Who Sent Message: ", c.username)

            name = msg["chatname"]
            recipients = msg["recipients"]
            print(recipients)

            # Initialize Chat with person who made request
            chat_recipients = [c]

            # Create New Chat, add all recipients to it, this can probably get moved to dif file
            for r in recipients:
                new_r  = clients.find_w_username(r)

                # If client is not found, send error
                if new_r == None:
                    err_res = Response("ErrorResponse", 404)
                    err_res.add_pair("message", "At least one client doesn't exist")
                    c.sock.write_message(err.jsonify())
                    return
                else:
                    chat_recipients.append(new_r)
            
            # After Looping through all recipients, create chat
            new_chat = GroupChat(name, chat_recipients)
            chats.add(new_chat)

            # Send ACK back to group chat creator
            ack_res = Response("GroupMessageInitResponse", 200)
            c.send(ack_res.jsonify())

            # Send notification of creation to all others in chat
            chat_res = Response("GroupMessageInitResponse", 201)
            chat_res.add_pair("chatname", name)
            new_chat.SendMessage(chat_res.jsonify())

        else: 
            c.send(Response("ErrorResponse", 301).jsonify())

    except Exception as e:
        print (e)
        sock.write_message(Response("ErroResponse", 400).jsonify())



# Currently Fails Silently on Whether A Person was Found or Not
def MessageRequestHandler(sock, msg):
    print("Message Request")

    c = GetClientWSock(sock)
    print("Client Who Sent Message: ", c.username)

    # If the client isn't registered, remove them from the list
    if c.registered:
        chatname = msg["recipient"]
        content = msg["content"]

        response = Response("MessageRecv", 200)
        response.add_pair("content", content)
        response.add_pair("sender", c.username)
       
        chat = GetChat(chatname)
        if chat != None:
            print("Sending Response...")
            chat.SendMessage(response, c)
            c.sock.write_message(json.dumps({ 
                                    "type": "GroupMessageResponse",
                                    "status": 200
                                }))
        else: 

    else: 
         sock.write_message(Response("ErrorResponse", 301).jsonify())


def ClientListRequestHandler(sock, msg):
    print("Client List Request")

    usernames = []
    c = GetClientWSock(sock)

    if c.registered:
        for c in clients: 
            if c.username != "":
                usernames.append(c.username)

        response = Response("ClientListResponse", 200)
        response.add_pair("clients", usernames)
        sock.write_message(response.jsonify())

    else: 
        sock.write_message(Response("ErrorResponse", 301).jsonify())



def RandomMessageRequestHandler(sock, msg):
    print("Random Message Request")

    try:
        c = GetClientWSock(sock)
        
        # Make Sure Not to Send Usernaame to Self 
        new_friend = sample(clients, 1)
        while new_friend.username == c.username:
            new_friend = sample(clients, 1)

        # Send Message to Random Client
        response = Response("RandomMessageRecvResponse", 200)
        response.add_pair("sender", c.username)
        response.add_pair("content", msg["content"])

        new_friend.sock.write_message(response.jsonify())

        # Send Ack Back to Sender 
        send_response = Response("RandomMessageSendResponse", 200)
        send_response.add_pair("recipient", new_friend.username)
        c.write_message(send_response.jsonify())

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

