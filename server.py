import tornado.ioloop
import tornado.web
import tornado.websocket
import json

from chatclient import ChatClient

from tornado.options import define, options, parse_command_line


define("port", default=8888, help="run on the given port", type=int)

# We gonna store clients in an array.
clients = []

class IndexHandler(tornado.web.RequestHandler):
    @tornado.web.asynchronous
    def get(self):
        self.render("index.html")


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


    # When a web socket connection has closed
    def on_close(self):
        print("WebSocket closed")


def MessageHandler(sock, msg):

    # Try To Parse Message Type and Handle 
    # Accordingly, otherwise send back error

    try: 
        msgType = msg["type"]
        print ("Got Message Type: ", msgType)

        if msgType == 1:
            print("Register Request")
            sock.write_message(u"Successful Register Request")

        elif msgType == 3:
            print("Message Request")

            recipients = msg["recipients"]
            content = msg["content"]

            print("Numbers of Clients to Send Message to: ", len(recipients))

            for c in recipients:
                print(c)
                print("Sending Response...")

                response =  { 
                              "status" : 200, 
                              "type" : 3, 
                              "clients": recipients, 
                              "content": content 
                            }

                sock.write_message(json.dumps(response))

        elif msgType == 4:
            print("Client List Request")
            print("Sending Response...")

            # Make List of usernames for clients
            usernames = [c.username for c in clients]

            response = { "status": 200, 
                         "clients": usernames
                       }

            sock.write_message(json.dumps(response))

        else: 
            print("Not a valid Message Type")
            print("Sending Error Response...")

            response = { "status": 401 }
            sock.write_message(json.dumps(response))

    except Exception as e: 
        print(e)
        print("Sending Error Response...")
        response = { "status": 400 }
        sock.write_message(json.dumps(response))





app = tornado.web.Application([
    (r'/', IndexHandler),
    (r'/ws', WebSocketHandler),
])

if __name__ == '__main__':
    parse_command_line()
    app.listen(options.port)
    tornado.ioloop.IOLoop.instance().start()

