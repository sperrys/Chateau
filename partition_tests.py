# Partition Tests

import traceback

from unittest import TestCase, main
from partition import DataStore, DataBlock, Partition, PartitionScheme
from chatclient import ChatClient


class PartitionTest(TestCase):
 
    def setUp(self):
        self.db = DataStore()
      
        client = ChatClient(5)
        client.set_username("Alan")
        client.registered = False
        client.hashpw = ""

        client2 = ChatClient(5)
        client2.set_username("John")
        client2.registered = False
        client2.hashpw = ""

        # Add the client Structure to the database
        self.db.add_structure("client")
        nb = DataBlock(client, "client", client.get_username())
        self.db.add_block(nb)

        nb = DataBlock(client2, "client", client2.get_username())
        self.db.add_block(nb)

        client3 = ChatClient(5)
        client3.set_username("Mary")
        client3.registered = False
        client3.hashpw = ""

        client4 = ChatClient(5)
        client4.set_username("Dan")
        client4.registered = False
        client4.hashpw = ""

        nb = DataBlock(client3, "client", client3.get_username())
        self.db.add_block(nb)

        nb = DataBlock(client4, "client", client4.get_username())
        self.db.add_block(nb)

    def test_single_cn(self):

        p = PartitionScheme(self.db, 1, 1)

        for i in p.partitions:
            self.assertEqual(i.begin_k, "Alan")
            self.assertEqual(i.end_k, "Mary")


    def test_2_cn(self):
        
        p = PartitionScheme(self.db, 2, 1)

        p1 =  p.partitions[0]
        self.assertEqual(p1.begin_k, "Alan")
        self.assertEqual(p1.end_k, "Dan")
        print(p1.get_full_data())
            
        p2 = p.partitions[1]
        self.assertEqual(p2.begin_k, "John")
        self.assertEqual(p2.end_k, "Mary")
        print(p2.get_full_data())

    def test_full_partition_data(self):

        p = PartitionScheme(self.db, 1, 1)
        p1 =  p.partitions[0]
        

        print(p1.get_full_data())



if __name__ == '__main__':
    main()
        


