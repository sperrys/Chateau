import tornado.ioloop
import tornado.web
import tornado.websocket
import json
import os

from chatclient import ChatClient

from tornado.options import define, options, parse_command_line

define("port", default=8888, help="run on the given port", type=int)


options.port = int(os.environ.get('PORT', 5000))
clients = []

class IndexHandler(tornado.web.RequestHandler):
    @tornado.web.asynchronous
    def get(self):
        self.render("index.html")


class CertRequestHandler(tornado.web.RequestHandler):
    @tornado.web.asynchronous
    def get(self):
        self.write('eBavPJ_kR67DbW7MEJ49Z-L7xqUyJZgvi5shL5iCI78.VC6txwIMOMmKlkjWLi-iG47yPMBGmPSp3-r_5m8IY34')


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
            print("Poorly formatted JSON message")
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
        print ("Got Message Type: ", msgType)

        if msgType == 1:
            RegisterRequestHandler(sock, msg)
        elif msgType == 3:
            MessageRequestHandler(sock, msg)
        elif msgType == 4:
            ClientListRequestHandler(sock, msg)
        elif msgType == 5:
            RandomMessageRequestHandler(sock, msg)
        else: 
            print("Not a valid Message Type")
            print("Sending Error Response...")

            response = { "status": 400 }
            sock.write_message(json.dumps(response))

    except Exception as e: 
        print(e)
        print("Sending Error Response...")
        response = { "status": 400 }
        sock.write_message(json.dumps(response))


def RegisterRequestHandler(sock, msg):
    print("Register Request")
    name = msg["username"]

    # Make Sure Unique Username
    for l in clients:
        if name == l.username:
            response = { "status": 302 }
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
                response = { "status": 302 }
                sock.write_message(json.dumps(response))
                return 

    # If the client cannot be found, send an error
    response = { "status": 301 }
    sock.write_message(json.dumps(response))


    
# Currently Fails Silently on Whether A Person was Found or Not
def MessageRequestHandler(sock, msg):
    print("Message Request")

    c = GetClientWSock(sock)
    print("Client Who Sent Message: ", c.username)

    # If the client isn't registered, remove them from the list
    if c.registered:
        recipients = msg["recipients"]
        content = msg["content"]

        print("Numbers of Clients to Send Message to: ", len(recipients))

        for r_cli in clients:
            for r in recipients:
                if r == r_cli.username:
                    print(r)
                    
                    response =  { 
                                    "status" : 200, 
                                    "type" : 3,
                                    "sender" : c.username,
                                    "clients": recipients,
                                    "content": content 
                                }

                    print("Sending Response...")
                    r_cli.sock.write_message(json.dumps(response))
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

        response = { "status": 200, 
                     "clients": usernames
                   }
        print("Sending Response...")
        sock.write_message(json.dumps(response))

    else: 
        print("Sending Error Response...")
        response = { "status": 302 }
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
                    "status" : 200,
                    "content": msg["content"]
                   }

        print("Sending Response...")
        new_friend.sock.write_message(json.dumps(response))

    except Exception as e:
        print("Sending Error Response")
        response = { "status": 400 }
        sock.write_message(json.dumps(response)) 






app = tornado.web.Application([
    (r'/', IndexHandler),
    (r'/ws', WebSocketHandler),
    (r'/.well-known/acme-challenge/eBavPJ_kR67DbW7MEJ49Z-L7xqUyJZgvi5shL5iCI78/', CertRequestHandler)
])


if __name__ == '__main__':
    parse_command_line()
    app.listen(options.port)
    tornado.ioloop.IOLoop.instance().start()

