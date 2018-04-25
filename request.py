import json

valid_types = [
               "RegisterRequest",
               "GroupMessageInitRequest",
               "RandomMessageRequest",
               "ClientListRequest",
               "PartitionRequest",
               "ClientListRequest"
               ]

class Request():
    
    def __init__(self, msg_type):
        self.pairs = []
        self.msg_type = msg_type

        # add Type and Status
        self.pairs.append(("type", self.msg_type))

    def jsonify(self):
        return json.dumps(dict(self.pairs))

    def add_pair(self, key, value):
        self.pairs.append((key, value))
        return self.jsonify()
                   
    def _validate_type(self):
        for ty in valid_types:
            if self.msg_type == ty:
                return True
                
        raise ValueError("Invalid Response Type")