
#
# Spencer Perry
# community_node.py
# 

import argparse

from servicer import app
from ../request import Request

import os

URL = "service_authority_url_here"
PORT = "service_authority_port_here"

uuid = os.urandom(LEN_UUID)
peers = []


def main(threshold):

	# Check to See if the Service Authority is Up, If so, 
	# update the community node with its partition. If its down,
	# shift into a servicer role, and maintain its partioned  
	# service data 

	(partition, peers) = PartitionServiceAuthority(URL + PORT, threshold):

	if partition != None:
		MigrateNode(partition, peers)
	else: 
		Service()


# Migrating a Node means updating information from 
# an existing sevice authority in order to better handle 
# the process of topologically shifting and moving into 
# a servicer role with other community node peers.

def MigrateNode(partition, peers):
	pass


# Start the community node in service mode where the 
# service authority is detached and the community node 
# takes the burden of providing the service to the clients.

def Service():
	pass



def PartitionServiceAuthority(service, threshold):

	ws_url = "ws://" + service + "/ws" 

	try: 
		# Try connecting to a known location for the
		# service authority.
		client = yield websocket.websocket_connect()

		req = Request("PartitionRequest")
		req.add_pair("uuid", uuid)
		req.add_pair("threshold", threshold)

		request = client.write_message(req)
		response = yield client.read_message()
        data = json.loads(response)

        partition = data['partition']
        peers = data['peers']

        return partition, peers

	# If there is any error, the service 
	# authority is not active 
	except Exception as e:
		return None, None


if __name__ == '__main__':
	
	# Parse Command Line Input
	parser = argparse.ArgumentParser(description='Run a Community Node for Chateau')
	parser.add_argument('threshold', metavar='N', type=int, help='an integer for the max amount in (gb) of servicer data to hold')
	args = parser.parse_args()

	# Start Running the Community Node tuned to personal preferences. 
	main(args.threshold)


