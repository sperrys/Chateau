
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
	def  __init__(self, chatname, recipients):
		self.chatname = chatname
		self.recipients = recipients
		self.message_id = 0

	def SendMessage(self, content, sender):
		for r in self.recipients:
			if r != sender:
				r.send(json.dumps(content))


