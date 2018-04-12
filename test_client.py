#
#    client.py
#    Spencer Perry & Russ Gomez
#
#    Python test client for the chat server
#
#    Currently Loops, taking json file messages 
#    as input and sends to the server, and reads
#    the server's response.


from tornado.ioloop import IOLoop, PeriodicCallback
from tornado import gen
from tornado.websocket import websocket_connect

import time
import sys


class TestClient(object):
    def __init__(self, url, timeout):
        self.url = url
        self.timeout = timeout
        self.ioloop = IOLoop.current()
        self.ws = None
        self.connect()

        #PeriodicCallback(self.keep_alive, 30000).start()
        #elf.ioloop.start()

    @gen.coroutine
    def connect(self):
        print("trying to connect")
        try:
            print(self.ws)
            self.ws = yield websocket_connect(self.url)
            print(self.ws)
        except Exception as e:
            print("connection error")
        
        print("connected")

    @gen.coroutine
    def read(self):
        try:
            msg = yield self.ws.read_message()
            return msg
        except Exception as e:
            print (e) 

    @gen.coroutine
    def send(self, message):
        self.ws.write_message(message)


    def keep_alive(self):
        if self.ws is None:
            self.connect()
        else:
            self.ws.write_message("keep alive")


#if __name__ == "__main__":
#    client = Client("ws://chateautufts.herokuapp.com:80/ws", 5)