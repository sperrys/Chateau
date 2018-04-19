

# server.py 
# Written by Spencer Perry 
# 4/10/18
# Comp 112 Project 


import json
import os
import traceback

from random import sample

import tornado.ioloop
from  tornado.ioloop import PeriodicCallback
import tornado.web
import tornado.websocket
import tornado.httpserver

from chatclient import ChatClient, Clients 
from chat import GroupChat, GroupChats
from response import Response
from ldap_client import TuftsAuth

from tornado.options import define, options, parse_command_line
from timeoutservice import TimeoutWebSocketService

define("port", default=5000, help="run on the given port", type=int)


options.port = int(os.environ.get('PORT', 5000))

clients = Clients()
chats = GroupChats()

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
        clients.remove_w_sock(self)


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
            MessageRequestHandler(sock, msg)
        elif msgType == "ClientListRequest":
            ClientListRequestHandler(sock, msg)
        elif msgType == "RandomMessageRequest":
            RandomMessageRequestHandler(sock, msg)
        else: 
            sock.write_message(Response("ErrorResponse", 400).jsonify())

    except Exception as e: 
        print(e)
        print(traceback.format_exc())
        sock.write_message(Response("ErrorResponse", 400).jsonify())


def RegisterRequestHandler(sock, msg):
    print("Register Request")

    try: 
        name = msg["username"]
        pw = msg["password"]
        auth_type = msg["auth"]  
        c = clients.find_w_sock(sock) 

        if auth_type == True:     
            auth = TuftsAuth(name, pw)
        else:
            auth = True
            
        if auth:     
            c.register(name, clients, msg["msg_id"])
         
        # Handle Error For Bad Authentication
        else: 
            auth_err = Response("ErrorResponse", 301)
            auth_err.add_pair("msg_id", msg["msg_id"])
            auth_err.add_pair("detail", "Authentication failed")
            c.send(auth_err.jsonify())

    # Handle Generic Exception
    except Exception as e:
        print(e)
        print(traceback.format_exc())
        generic_err = Response("ErrorResponse", 400)
        generic_err.add_pair("msg_id", msg["msg_id"])
        sock.write_message(generic_err.jsonify())

    

def GroupMessageInitHandler(sock, msg):
    print("GroupMessageInitRequest")

    try: 
        c = clients.find_w_sock(sock)
  
        if c.registered:
    
            name = msg["chatname"]
            recipients = msg["recipients"]

            # Initialize Chat with person who made request
            recipients.append(c.username)

            # Create new group chat, add recipients
            new_chat = GroupChat(c, name, recipients)

            # Validate the chat's recipients, send error if not valid
            if new_chat.validate_recipients(clients, msg["msg_id"]):

                print(chats)
                # Add chat to list of chats, check chatname uniqueness
                if chats.add(new_chat):
                    # Send ACK back to group chat creator
                    ack_res = Response("GroupMessageInitResponse", 200)
                    ack_res.add_pair("msg_id", msg["msg_id"])
                    c.send(ack_res.jsonify())

                    # Send notification of creation to all others in chat
                    chat_res = Response("GroupMessageInitResponse", 201)
                    chat_res.add_pair("chatname", name)
                    new_chat.send(chat_res.jsonify(), c)
                
                else: 
                    err = Response("ErrorResponse", 303)
                    err.add_pair("msg_id", msg["msg_id"])
                    err.add_pair("detail", "chatname already taken")
                    c.send(err.jsonify())



        # Handle if Client is Not Registered
        else:
            err = Response("ErrorResponse", 301)
            err.add_pair("msg_id", msg["msg_id"])
            c.send(err.jsonify())

    # Handle Other Exceptions
    except Exception as e:
        print(e)
        print(traceback.format_exc())

        err = Response("ErrorResponse", 400)
        err.add_pair("msg_id", msg["msg_id"])
        sock.write_message(err.jsonify())


# Look for a valid message recipient
# first using "recipient" as a chatname
# then as a client's username

def find_recipient(recipient, client, msg_id):
    
    r = chats.find(recipient)
 
    if r != None: 
        return (r, "Group")
    else: 
        r = clients.find_w_username(recipient)
        if r != None:
            return (r, "Single")

    err = Response("ErrorResponse", 303)
    err.add_pair("msg_id", msg_id)
    err.add_pair("detail", "Recipient Doesn't Exist")

    c.send(err.jsonify())
    return None

# Currently Fails Silently on Whether A Person was Found or Not
def MessageRequestHandler(sock, msg):
    print("Message Request")
    try: 
        c = clients.find_w_sock(sock)

        # If the client isn't registered, remove them from the list
        if c.registered:
            
            content = msg["content"]
            recipient = find_recipient(msg["recipient"], c, msg["msg_id"])

            response = Response("MessageRecv", 200)
            response.add_pair("content", content)
            response.add_pair("sender", c.username)
            response.add_pair("msg_id", msg["msg_id"])

            if recipient != None:
                if recipient[1] == "Group":
                    response.add_pair("groupchat", True)
                    response.add_pair("chatname", msg["recipient"])
                    response.add_pair("msg_id", msg["msg_id"])
                    recipient[0].send(response.jsonify(), c)

                # chatname becomes person who sent
                # for single messages
                else:
                    response.add_pair("chatname", c.username)
                    response.add_pair("msg_id", msg["msg_id"])
                    response.add_pair("groupchat", False)
                    recipient[0].send(response.jsonify())

                # Send Ack back to sender
                ack = Response("MessageSendResponse", 200) 
                ack.add_pair("msg_id", msg["msg_id"])
                c.send(ack.jsonify())
         
        # Catch Exception if the client is not registered.  
        else: 
            err = Response("ErrorResponse", 301)
            err.add_pair("msg_id", msg["msg_id"])
            sock.write_message(err.jsonify())

    # Catch Generic Exception
    except Exception as e:
        print(e)
        print(traceback.format_exc())
        res = Response("ErrorResponse", 400)
        res.add_pair("msg_id", msg["msg_id"])
        sock.write_message(res.jsonify())



def ClientListRequestHandler(sock, msg):
    print("Client List Request")

    try: 
        c = clients.find_w_sock(sock)

        if c.registered:
            usernames = clients.usernames()
            usernames.remove(c.username)

            response = Response("ClientListResponse", 200)
            response.add_pair("msg_id", msg["msg_id"])
            response.add_pair("clients", usernames)
            c.send(response.jsonify())

        # Handle Error for Client not registered
        else: 
            err = Response("ErrorResponse", 301)
            err.add_pair("msg_id", msg["msg_id"])
            c.send(err.jsonify())
    
    # Handle Generic Error, or no client with Sock
    except Exception as e:
        print (e)
        print(traceback.format_exc())
        sock.write_message(Response("ErrorResponse", 400).jsonify())



def RandomMessageRequestHandler(sock, msg):
    print("Random Message Request")

    try:
        c = clients.find_w_sock(sock)
        print("client: ", c)
    
        if c.registered:
            usernames = clients.usernames()
            usernames.remove(c.username)
            print(usernames)

            if usernames != []:

                # Make Sure Not to Send Usernaame to Self 
                s = sample(usernames, 1)
                print("rand", s)
                rand = s[0]
                
                new_friend = clients.find_w_username(rand)
                print(new_friend.username)

                # Send Message to Random Client
                response = Response("RandomMessageRecv", 200)
                response.add_pair("sender", c.username)
                response.add_pair("content", msg["content"])

                new_friend.send(response.jsonify())

                # Send Ack Back to Sender 
                send_response = Response("RandomMessageResponse", 200)
                send_response.add_pair("msg_id", msg["msg_id"])
                send_response.add_pair("recipient", new_friend.username)
                c.send(send_response.jsonify())
            else:
                  # Send Error to Sender 
                err = Response("ErrorResponse", 404)
                err.add_pair("msg_id", msg["msg_id"])
                err.add_pair("detail", "not enough people online")
                c.send(err.jsonify())

        # Handle client is not registered
        else:
            c.send(Response("ErrorResponse", 301).jsonify())

    # Handle Generic Error
    except Exception as e:
        print (e)
        print(traceback.format_exc())
        sock.write_message(Response("ErrorResponse", 400).jsonify())


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

