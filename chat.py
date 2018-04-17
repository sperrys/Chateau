
import json

from response import Response

class GroupChats():
    def __init__(self):
        self.chats = []

    def add(self, GroupChat):
        if self._unique_name(GroupChat):
            self.chats.append(GroupChat)
            return True
        else:
            return False
            raise ValueError("GroupChat with %s already exists", GroupChat.chatname)

    def remove(self, GroupChat):
        self.chats.remove(GroupChat)

    def find(self, chatname):
        for c in self.chats:
            if c.chatname == chatname:
                return c
        return None 

    def _unique_name(self, groupchat): 
        for c in self.chats:
            print (c.chatname)
            if c.chatname == groupchat.chatname:
                return False
        return True

class GroupChat():
    def  __init__(self, creator, chatname, recipients):
        self.chatname = chatname
        self.message_id = 0
        self.recipients = recipients
        self.creator = creator
        self.valid_clients = []

    # Send Message Content to everyone except sender
    def send(self, content, sender):
        for r in self.valid_clients:
            if r != sender:
                r.send(content)

        # Increment Message ID      
        self.message_id += 1

    # Make sure that all recipients are valid clients
    def validate_recipients(self, clients, msg_id):
        print("Recipients: ", self.recipients)
        print(len(self.recipients))
        for r in self.recipients:
            c = clients.find_w_username(r) 
            # If can't find recipent send error to creator,
            # stop creation of the chat
            if c == None:
                err_res = Response("ErrorResponse", 404)
                err_res.add_pair("msg_id", msg["msg_id"])
                err_res.add_pair("detail", "At least one client doesn't exist")

                # Send Error Response to Chat Creator Return invalid
                self.creator.send(err_res.jsonify())
                return False
            
            # If they do exist, add them to the list of valid clients 
            # in the chat.
            self.valid_clients.append(c)

        return True


