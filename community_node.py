
#
# Spencer Perry
# community_node.py
# 

import argparse

from servicer import app
from request import Request

import os

# Class for A Group of Community 
# Nodes, also called Community Peers

class CommunityPeers():
    def __init__(self):
        try: 
            self.peers = []
        except Exception as e: 
            print(traceback.format_exc())

    def add(self, cn):
        try: 
            self.peers.append(cn)
        except Exception as e: 
            print(traceback.format_exc())

    def remove(self, cn):
        try: 
            self.peers.remove(cn)
        except Exception as e: 
            print(traceback.format_exc())

    def size(self):
        try:
            return len(self.peers)
        except Exception as e:
            print(traceback.format_exc())

    def broadcast_new_partitions(self, p):
        try: 
            for p in self.peers:
                p.sock.send_message(p)

        except Exception as e: 
            print(traceback.format_exc())



# Class For A Single Community Node

class CommunityNode():

    def __init__(self, sa, entry_point):

        # Required Configuration By Caller
        self.sa = sa 
        self.entry_point = entry_point

        # Intialize Variables
        self.partition_version = 0
        self.partition = None
        self.peers = {}
        self.sock = None

    def set_sock(self, sock):
        try:
            self.sock = sock
        except Exception as e: 
            print(traceback.format_exc())

    def GetSAPartitionConfiguration(self):

        # Check to See if the SA is up and alive at all. If so, 
        # update the community node with new partition info.
        # If not up, set configuration to play servicer role 
        # for clients

        try: 
            res = self.RequestShiftConfiguration()

            if res != None:
                self.partition = res["partition"]
                self.peers = res["peers"]
            else: 
                self.StartService()

        except Exception as e: 
            print(traceback.format_exc())

    # This function takes in a list of peers provided from 
    # an outside source and updates its own list of peers with 
    # the new information 

    def UpdatePeers(self, fresh_peers):

        for p in fresh_peers:
            # Reset List Since all will be overwritten
            # except those who no longer are active
            self.peers = {}
            self.peers[p.entry_point] = p
            

    # Start the community node in service mode where the 
    # service authority is detached and the community node 
    # takes the burden of providing the service to the clients.

    def Start_Service():
        pass


    def RequestShiftConfiguration(self):

        try: 
            # Try connecting to known location of SA
            client = yield websocket.websocket_connect(self.sa)

            # Send Partition Request with info
            req = Request("PartitionRequest")
            req.add_pair("entry_point", self.entry_point)
            request = client.write_message(req)

            # Parse Response
            response = yield client.read_message()
            data = json.loads(response)
            partition = data['partition']
            peers = data['peers']

            return partition, peers

        # If there is any error, the service 
        # should not be considered active
        except Exception as e:
            return None, None


if __name__ == '__main__':
        
    # Parse Command Line Input
    parser = argparse.ArgumentParser(description='Run a Community Node for Chateau')
    parser.add_argument('entry_point', metavar='E', type=str, help='the community node entry_point for handling requests')
    args = parser.parse_args()



