

from unittest import TestCase, main
from partition import DataStore, DataBlock
from chatclient import ChatClient
 
class DataStoreTest(TestCase):
 
    def setUp(self):
        self.db = DataStore()
      
    def test_add_structure(self):
 
        self.db.add_structure("client")
        # Verify that the version has changed
        self.assertEqual(self.db.version, 1)
        # Verify size of db is still 0
        self.assertEqual(self.db.get_size(), 0)
        # Verify that key exists and returns the empty set
        self.assertEqual(self.db.get_structure_list('client'), [])

    def test_add_block(self):

        # Dummy Client Data 
        client = ChatClient(4)
        client.set_username("ClientTest1")
        client.registered = False
        client.hashpw = ""

        # Add the client Structure to the database
        self.db.add_structure("client")

        # Create A Data Block for ClientTest1
        nb = DataBlock(client, "client", "ClientTest1")
        
        # Try adding the new block to the db
        self.db.add_block(nb)

        # Verify that db version has changed 
        self.assertEqual(self.db.version, 2)

        # Verify that size of the db is now 1 block
        self.assertEqual(self.db.get_size(), 1)

        client_list = self.db.get_structure_list("client")

        # Verify Size of Client is 1
        self.assertEqual(len(client_list), 1)

        added_block = client_list[0]

        # Verify that the added block has the same info
        self.assertEqual(added_block.id, 1)
        self.assertEqual(added_block.type, "client")

        print(added_block.jsonify())


    def test_alter_block(self):
        # Dummy Client Data 
        client = ChatClient(4)
        client.set_username("ClientTest1")
        client.registered = False
        client.hashpw = ""


        # Add the client Structure to the database
        self.db.add_structure("client")

        # Create A Data Block for ClientTest1
        nb = DataBlock(client, "client", "ClientTest1")
    
        # Try adding the new block to the db
        self.db.add_block(nb)

        b_id = nb.id

        # Verify that db version has changed 
        self.assertEqual(self.db.version, 2)

        # Verify that size of the db is now 1 block
        self.assertEqual(self.db.get_size(), 1)

        client.registered = False
        res = self.db.alter_block('client', b_id, client)
        self.assertEqual(res, True)

        # Verify that db version has changed 
        self.assertEqual(self.db.version, 3)
        client_list = self.db.get_structure_list('client')
        alt_block = client_list[0]

        self.assertEqual(alt_block.id, b_id)
        self.assertEqual(self.db.get_size(), 1)

    def test_remove_block(self):
        # Dummy Client Data 
        client = ChatClient(4)
        client.set_username("ClientTest1")
        client.registered = False
        client.hashpw = ""

        # Add the client Structure to the database
        self.db.add_structure("client")

        # Create A Data Block for ClientTest1
        nb = DataBlock(client, "client", "ClientTest1")
   
        # Try adding the new block to the db
        self.db.add_block(nb)

        b_id = nb.id

        # Verify that db version has changed 
        self.assertEqual(self.db.version, 2)

        # Verify that size of the db is now 1 block
        self.assertEqual(self.db.get_size(), 1)

        # Now try removing the block
        res = self.db.remove_block("client", b_id)
        self.assertEqual(res, True)

        self.assertEqual(self.db.version, 3)
        self.assertEqual(self.db.get_size(), 0)
        self.assertEqual(len(self.db.get_structure_list('client')), 0)

    def test_index(self):
        client = ChatClient(5)
        client.set_username("ClientTest1")
        client.registered = False
        client.hashpw = ""

        client2 = ChatClient(5)
        client2.set_username("AlientTest2")
        client2.registered = False
        client2.hashpw = ""

        # Add the client Structure to the database
        self.db.add_structure("client")
        nb = DataBlock(client, "client", "ClientTest1")
        self.db.add_block(nb)

        nb = DataBlock(client2, "client", "AlientTest2")
        self.db.add_block(nb)

        res = self.db.index()
        
        for i in res:
            print(i.key)


if __name__ == '__main__':
    main()

         
