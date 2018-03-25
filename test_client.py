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


class Client(object):
    def __init__(self, url, timeout):
        self.url = url
        self.timeout = timeout
        self.ioloop = IOLoop.current()
        self.ws = None
        self.connect()

        PeriodicCallback(self.keep_alive, 30000).start()
        self.ioloop.start()

    @gen.coroutine
    def connect(self):
        print("trying to connect")
        try:
            self.ws = yield websocket_connect(self.url)
        except Exception as e:
            print("connection error")
        else:
            print("connected")
            self.run()

    @gen.coroutine
    def run(self):
        while True:
            message = ""

            # Prompt User for Message 
            stdin = input("Message File or Read for read input (r): ")

            # Open File, Read and Format Nicely
            try:

                if stdin != 'r':
                    with open(stdin) as fp:
                      for line in fp:
                        message += line

                    # Send Message to Server
                    print("Sending Message: ", message)
                    self.ws.write_message(message)

                # Read Response from Serve
                msg = yield self.ws.read_message()
                print(msg)
            
                # Sleep Because Tired 
                time.sleep(1)

                # If the connection was closed by Server
                if msg is None:
                    print("connection closed")
                    self.ws = None
                    break

            except Exception as e:
                print("Server Caught Exception:", e)

    def keep_alive(self):
        if self.ws is None:
            self.connect()
        else:
            self.ws.write_message("keep alive")


if __name__ == "__main__":
    client = Client("ws://localhost:8888/ws", 5)