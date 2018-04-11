
import json

from response import Response

class GroupChats():
	def __init__(self):
		self.chats = []

	def add(self, GroupChat):
		if self._unique_name(GroupChat):
			self.chats.append(GroupChat)
		else:
			raise ValueError("GroupChat with %s already exists", GroupChat.chatname)

	def remove(self, GroupChat):
		self.chats.remove(GroupChat)

	def find(self, chatname):
		for c in self.chats:
			if c.chatname == chatname:
				return c
		return None 

	def _unique_name(groupchat): 
		for c in self.chats:
			if c.chatname == groupchat.chatname:
				return False
		return True

class GroupChat():
	def  __init__(self, creator, chatname, clients, recipients):
		self.chatname = chatname
		self.message_id = 0

	# Send Message Content to everyone except sender
	def send(self, content, sender):
		for r in self.recipients:
			if r != sender:
				r.send(content)

		# Increment Message ID 		
		self.message_id += 1

	# Make sure that all recipients are valid clients
	def validate_recipients():
		for r in self.recipients:
			c = self.clients.find_w_username(r) 

			# If can't find recipent send error to creator,
			# stop creation of the chat
			if c == None:
				err_res = Response("ErrorResponse", 404)
                err_res.add_pair("message", "At least one client doesn't exist")
                self.creator.send(err.jsonify())
                return False
			else: 
				self.recipients.append(c)
		return True


