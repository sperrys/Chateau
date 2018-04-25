
#
#   partition_handlers.py 
#
#
#   These Handlers Deal with Partitioning
#   and Network Topology Shifts
#
#

from partition import PartitionScheme, Partition
from community_node import CommunityNode, CommunityPeers


# Handle Partition Request Message
def PartitionRequestHandler(sock, msg, cp, DataStore):
    print("Partition Request")
    try:
        # Initialize New Community Node, Add to 
        # List of Community Peers
        cn = CommunityNode("sa.domain", msg["entry_point"])
        cn.set_sock(sock)
        cp.append(cn)
       
        # Determine Partition Scheme Based on # of community
        # peers and the current state of the DataStore
        p = DeterminePartitionScheme(cp.size(), DataStore)
        
        cn.update_partition()

    # Send Generic Error Back to Client
    except Exception as e:
        print(traceback.format_exc())
        sock.write_message(Response("ErrorResponse", 400))


# Determine the Current Partition Scheme based upon
# the number of community nodes.

def DeterminePartitionScheme(community_nodes_num, DataStore):
    print("DeterminingPartitionScheme")
    try:
        scheme_id = 0 
        data_size = DataStore.get_size()
        return PartitionScheme(data_size, community_nodes_num, scheme_id)
    
    except Exception as e: 
        print(traceback.format_exc())

