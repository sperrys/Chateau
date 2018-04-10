
import json


class GroupChat():
	def  __init__(self, chatname, recipients):
		self.chatname = chatname
		self.recipients = recipients
		self.message_id = 0

	def SendMessage(self, content, sender):
		for r in self.recipients:
			if r != sender:
				r.sock.write_message(json.dumps(content))