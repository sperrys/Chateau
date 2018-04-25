

#
#  community_node.py
#  Spencer Perry
# 
# 
#  This file dictates the functionality of 
#  a community node both when the service 
#  is functioning under a service authority 
#  and when the community nodes begin to
#  distribute the serivce.  


# For simplicity sake, we use a tornado webserver to act as 
# the servicer portion for our community nodes. This servicer portion 
# could either be provided / implemented by the service authority, 
# or could be constructed by service members. Either way, the servicer 
# in conjuction with the servicer data form the key attributes in maintaning
# functionality during a severe topological shift.

# While a service's functionality could certainly be scaled and distributed amongst machines, 
# for simplicity sake, we assume that we place an emphasis on scaling the servicer's data. 

# We can therefore grow of the network easily, but not necessarily the modes of communication 
# within that network.


# To run, python servicer.py

import json
import os
import traceback

from tornado import ioloop, web, websocket, httpserver

class WebSocketHandler(websocket.WebSocketHandler):

    # When A Web Socket Connection Has Been Opened 
    def open(self):
        print("WebSocket opened")
        print("New Client Initializing...")

        c = ChatClient(self)
        clients.add(c)

    def on_message(self, message):
        print ("Server Got Message: ", message)
        
        # Get Message From Socket 
        # Pass Off to Message Handler
        try: 
            msg = json.loads(message)
            MessageHandler(self, msg)

        except Exception as e:
            print(e) 
            self.write_message(Response("ErrorResponse", 400).jsonify())


    # When a web socket connection has closed,
    # Remove the client from list of clients
    
    def on_close(self):
        print("WebSocket closed")
        print("Removing Client")
        #self.timeout_service.clean_timeout()
        clients.remove_w_sock(self)


app = web.Application([
    (r'/ws', WebSocketHandler)
])


