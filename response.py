import json

class ErrorResponse():
	msg_type = "ErrorResponse"
	
	def __init__(self, status_code):
		self.status = status_code

	def jsonify(self):
		response = {  
					  "type"  : self.msg_type,
                      "status": self.status
                   }
                   
		return json.dumps(response)