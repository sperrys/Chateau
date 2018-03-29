
import json


class Chat():
	def  __init__(self, chatname, recipients):
		self.chatname = chatname
		self.recipients = recipients

	def SendMessage(content, sender):
		for r in recipients:
			if r != sender:
				r.sock.write_message(json.dumps(content))