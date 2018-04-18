
# Application to demonstrate testing tornado websockets.
# Try it with: python -m tornado.testing discover


import json

from request import Request

from tornado import testing, httpserver, gen, websocket
from server import app

from test_client import TestClient

class TestChatHandler(testing.AsyncHTTPTestCase):

    def get_app(self):
        return app


            
    @testing.gen_test
    def test_register_anon_w_sperry(self):
       
        ws_url = "ws://localhost:" + str(self.get_http_port()) + "/ws"
        client = yield websocket.websocket_connect(ws_url)

        req = Request("RegisterRequest")
        req.add_pair("username", "sperry02")
        req.add_pair("msg_id", 1)
        req.add_pair("password", "")
        req.add_pair("auth", "false")

        client.write_message(req.jsonify())
        
        response = yield client.read_message()
        print(response)

        data = json.loads(response)
        # Make sure that we got a 'hello' not 'bye'
        self.assertEqual(data["type"], "RegisterResponse")
        self.assertEqual(data["status"], 200)


    @testing.gen_test
    def test_register_anon_w_mgomez(self):
        ws_url = "ws://localhost:" + str(self.get_http_port()) + "/ws"
        client = yield websocket.websocket_connect(ws_url)
        
        req = Request("RegisterRequest")
        req.add_pair("username",  "mgomez")
        req.add_pair("msg_id", 1)
        req.add_pair("password", "")
        req.add_pair("auth", "false")

        client.write_message(req.jsonify())
        
        response = yield client.read_message()
        data = json.loads(response)

        self.assertEqual(data["type"], "RegisterResponse")
        self.assertEqual(data["status"], 200)
        
    @testing.gen_test
    def test_clientlist(self):

        ws_url = "ws://localhost:" + str(self.get_http_port()) + "/ws"

        # Register mgomez
        russ = yield websocket.websocket_connect(ws_url)     
        
        reg = Request("RegisterRequest")
        reg.add_pair("username",  "russ2")
        reg.add_pair("password", "")
        reg.add_pair("msg_id", 1)
        reg.add_pair("auth", "false")
        
        russ.write_message(reg.jsonify())
        response = yield russ.read_message()
        
        # Register Sperry02
        spencer = yield websocket.websocket_connect(ws_url)     

        reg1 = Request("RegisterRequest")
        reg1.add_pair("username", "sperry2")
        reg1.add_pair("msg_id", 1)
        reg1.add_pair("password", "")
        reg1.add_pair("auth", "false")
        
        spencer.write_message(reg1.jsonify())
        response = yield spencer.read_message()

        # Mgomez request client list
        cli_list = Request("ClientListRequest")
        cli_list.add_pair("msg_id", 1)
        russ.write_message(cli_list.jsonify())

        response = yield russ.read_message()
        data = json.loads(response)

        self.assertEqual(data["type"], "ClientListResponse")
        self.assertEqual(data["status"], 200)
        self.assertEqual(data["clients"], ["sperry2"])

    @testing.gen_test
    def test_single_chat_valid(self):

        ws_url = "ws://localhost:" + str(self.get_http_port()) + "/ws"

        # Register mgomez
        russ = yield websocket.websocket_connect(ws_url)     
        
        reg = Request("RegisterRequest")
        reg.add_pair("username",  "russ3")
        reg.add_pair("password", "")
        reg.add_pair("auth", "false")
        reg.add_pair("msg_id", 1)
        
        russ.write_message(reg.jsonify())
        response = yield russ.read_message()
        
        # Register Sperry02
        spencer = yield websocket.websocket_connect(ws_url)     

        reg1 = Request("RegisterRequest")
        reg1.add_pair("username", "sperry3")
        reg1.add_pair("password", "")
        reg1.add_pair("msg_id", 2)
        reg1.add_pair("auth", "false")
        
        spencer.write_message(reg1.jsonify())
        response = yield spencer.read_message()

        # Mgomez3 chats sperry3
        msg = Request("MessageRequest")
        msg.add_pair("recipient", "sperry3")
        msg.add_pair("msg_id", 3)
        msg.add_pair("content", "hey")

        russ.write_message(msg.jsonify())

        send_response = yield russ.read_message()
        data = json.loads(send_response)

        self.assertEqual(data["type"], "MessageSendResponse")
        self.assertEqual(data["status"], 200)

        # Read Response from Recipient
        recv_response = yield spencer.read_message()
        data = json.loads(recv_response)

        self.assertEqual(data["type"], "MessageRecv")
        self.assertEqual(data["content"], "hey")
        self.assertEqual(data["status"], 200)
        self.assertEqual(data["sender"], "russ3")
        self.assertEqual(data["chatname"], "russ3")
        self.assertEqual(data["groupchat"], False)


    @testing.gen_test
    def test_groupchat_valid(self):

        ws_url = "ws://localhost:" + str(self.get_http_port()) + "/ws"

        # Register mgomez
        russ = yield websocket.websocket_connect(ws_url)       
        reg = Request("RegisterRequest")
        reg.add_pair("username", "russ4")
        reg.add_pair("password", "")
        reg.add_pair("msg_id", 1)
        reg.add_pair("auth", "false")
        russ.write_message(reg.jsonify())
        response = yield russ.read_message()
        
        # Register Sperry02
        spencer = yield websocket.websocket_connect(ws_url)     
        reg1 = Request("RegisterRequest")
        reg1.add_pair("username", "sperry4")
        reg1.add_pair("password", "")
        reg1.add_pair("msg_id", 1)
        reg1.add_pair("auth", "false") 
        spencer.write_message(reg1.jsonify())
        response = yield spencer.read_message()

        # Register fahad
        fahad = yield websocket.websocket_connect(ws_url)     
        reg2 = Request("RegisterRequest")
        reg2.add_pair("username", "fahad")
        reg2.add_pair("password", "")
        reg2.add_pair("msg_id", 1)
        reg2.add_pair("auth", "false") 
        fahad.write_message(reg2.jsonify())
        response = yield fahad.read_message()

        # Mgomez3 create group chat
        chat_init = Request("GroupMessageInitRequest")
        chat_init.add_pair("recipients", ["sperry4", "fahad"])
        chat_init.add_pair("chatname", "comp112")
        chat_init.add_pair("msg_id", 3)

        russ.write_message(chat_init.jsonify())

        send_response = yield russ.read_message()
        data = json.loads(send_response)
        print(data)

        self.assertEqual(data["type"], "GroupMessageInitResponse")
        self.assertEqual(data["status"], 200)

        # Read Response from those in group chat

        # Make Sure Spencer Gets Create Message
        recv_response = yield spencer.read_message()
        data = json.loads(recv_response)
        print(data)

        self.assertEqual(data["type"], "GroupMessageInitResponse")
        self.assertEqual(data["status"], 201)
        self.assertEqual(data["chatname"], "comp112")

        # Make sure Fahad Gets Create Message
        recv_response = yield fahad.read_message()
        data = json.loads(recv_response)
        print(data)

        self.assertEqual(data["type"], "GroupMessageInitResponse")
        self.assertEqual(data["status"], 201)
        self.assertEqual(data["chatname"], "comp112")

        # Spencer Sends Message to Group
        chat_msg = Request("MessageRequest")
        chat_msg.add_pair("recipient", "comp112")
        chat_msg.add_pair("content", "hey")
        chat_msg.add_pair("msg_id", 1)

        spencer.write_message(chat_msg.jsonify())        

        # Make sure Fahad Gets Conent Message
        recv_response = yield fahad.read_message()
        data = json.loads(recv_response)
        print(data)

        self.assertEqual(data["type"], "MessageRecv")
        self.assertEqual(data["status"], 200)
        self.assertEqual(data["chatname"], "comp112")
        self.assertEqual(data["sender"], "sperry4")
        self.assertEqual(data["content"], "hey")








    



        

