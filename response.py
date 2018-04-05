import json

class ErrorResponse():
    msg_type = "ErrorResponse"
    
    def __init__(self, status_code):
        self.pairs = []
        self.status = status_code

        self.pairs.append(("type", self.msg_type))
        self.pairs.append(("status", status_code))

    def jsonify(self):
        print (self.pairs)
        return json.dumps(dict(self.pairs))

    def add_pair(self, key, value):
        self.pairs.append((key, value))
        return self.jsonify()
                   