
# partition.py
# 
# Spencer Perry
# Networks Project
# 
# This file deals with the in-memory database 
# representation for the server. While this 
# certainly is not the fastest, or most effecient 
# way to do this, it was a necessary abstraction 
# to build in order to perform the fallback 
# partitioning scheme that we wanted to do for 
# the project.

import traceback
import json
import heapq


# A Partition Scheme Lays Out 
# how the application should partition
# its data based upon how many active
# community nodes exist

class PartitionScheme():

	def __init__(self, data_store, community_nodes_num, scheme_id):
		try: 
			self.data_store = data_store
			self.blocks_per_partition = int(data_store.get_size() / community_nodes_num)
			self.remainder = data_store.get_size() % self.blocks_per_partition
			self.num_partitions = community_nodes_num
			self.version = scheme_id
			self.partitions = []

			self.generate_partitions()

		except Exception as e: 
			print(traceback.format_exc())

	def assign(self, cns):
		try:
			for x in range(0, self.num_partitions):
				self.partitions[x].assign(cns[x].entry_point)

		except Exception as e: 
			print(traceback.format_exc())

	def get_partition(self, entry_point):
		try: 
			for p in partitions:
				if p.assignee == entry_point:
					return p
			return None

		except Exception as e: 
			print(traceback.format_exc())

	def generate_partitions(self):

		index = list(self.data_store.index())
		p_id = 1
		remainder = self.remainder

		last_end = -1

		try: 
			for x in range(0, self.num_partitions):
	
				begin  = int(last_end) + 1
				end = int(begin + self.blocks_per_partition - 1)

				last_end = end

				if remainder > 0:
					end += 1
					remainder -= 1

				last_end = end

				p = Partition(self.data_store, p_id, (begin, index[begin].key), 
							 (end, index[end].key), (end - begin + 1), self.version)

				self.partitions.append(p)

				p_id += 1

		except Exception as e: 
			print(traceback.format_exc())

# A Partition is the representation of the 
# individual units that comprise a partition scheme. 

class Partition():

	def __init__(self, DataStore, b_id, begin, end, size, scheme):
		try: 
			self.id = b_id
			self.size = size

			self.begin_i = begin[0]
			self.begin_k = begin[1]
			self.end_i = end[0]
			self.end_k = end[1]

			self.scheme = scheme
			self.assignee = None
			self.DataStore = DataStore

		except Exception as e: 
			print(traceback.format_exc())

	def get_size(self):
		try:
			return self.size

		except Exception as e: 
			print(traceback.format_exc())

	# Assign a community node to the partition Section
	def assign(self, entry_point):
		try: 
			self.assignee = cn
		except Exception as e: 
			print(traceback.format_exc())

	# Get the full slate of data for a partition
	def get_full_data(self):
		try:
			index = list(self.DataStore.index())
			return index[self.begin_i: self.end_i + 1]

		except Exception as e:
			print(traceback.format_exc())


# A Data Store is the in memory data abstraction repesentation
# A Data Store is composed of Data Block. Each Data Block is
# considered a single unit with a cost of 1.

class DataStore():
	def __init__(self):
		try:
			self.version = 0
			self.cur_block_id = 0 
			self.types = {}

		except Exception as e: 
			print(traceback.format_exc())

	def index(self):
		merged = []
		for (keys, values) in self.types.items():
			merged = heapq.merge(values, merged, key=lambda block: block.key)
		return merged


	# Returns the current size of the data store by 
	# computing the length of all representation's- block_lists
	# This value represents the number of data_blocks in the 
	# data store 

	def get_size(self):
		try: 
			size = 0
			for structure, block_list in self.types.items():
				size += len(block_list)
			return size

		except Exception as e: 
			print(traceback.format_exc())

	def find_block_w_index(self, type, index):
		try: 
			block_list = types["type"]
			return block_list[index]

		except Exception as e: 
			print(traceback.format_exc())


	# Add a new structure to the datastore,
	# intialize it with a an empty list of datablocks

	def add_structure(self, structure):
		try:
			self.types[structure] = []
			self.version= self.version + 1
		except Exception as e: 
			print(traceback.format_exc())

	# Returns the list associated with a given
	# structure 

	def get_structure_list(self, structure):
		
		try:
			return self.types[structure]

		except Exception as e:
			print(traceback.format_exc())

			
	# Add a new block to the data store. Add to 
	# the list that's keyed by the structure

	def add_block(self, block):
		try: 
			# Assign Block Id, increment the current block id. 
			block.set_id(self.cur_block_id + 1)
			self.cur_block_id = block.get_id()

			self.types[block.get_type()].append(block)
			self.version = self.version + 1

			list.sort(self.types[block.get_type()], key=lambda DataBlock: DataBlock.key)

		except Exception as e: 
			print(traceback.format_exc())

	# Removes a pre-existing block from the datastore,
	# does not change the ids 

	def remove_block(self, b_type, b_id):

		try:
			block_list = self.types[b_type]
			found = False
			
			for b in block_list:
				# if we find the block, remove it 
				if b.id == b_id:
					block_list.remove(b)
					self.version = self.version + 1
					return True
			return False

		except Exception as e: 
			print(traceback.format_exc())


	# Alters a current block by overwriting the data at 
	# that location, doesn't support partial data re-writes 
	# at the moment

	def alter_block(self, b_type, b_id, data):
	
		try: 
			block_list = self.types[b_type]

			for b in block_list:
				if b.id == b_id:
					b.data = data
					self.version = self.version + 1
					return True
			return False

		except Exception as e:
			print(traceback.format_exc())
		
		

# Datablocks are the unit of a DataStore.
class DataBlock():
	def __init__(self, obj, type, key):
		try:
			self.type = type
			self.id = None
			self.data = obj
			self.key = key

		except Exception as e: 
			print(traceback.format_exc())
	
	def get_key(self):
		try: 
			return self.key
		except Exception as e:
			print(traceback.format_exc())

	def set_key(self, key):
		try: 
			self.key = key
		except Exception as e:
			print(traceback.format_exc())

	def get_id(self):
		try:
			return self.id
		except Exception as e: 
			print(traceback.format_exc())	

	def set_id(self, b_id):
		try:
			self.id = b_id 
		except Exception as e: 
			print(traceback.format_exc())

	def get_type(self):
		try:
			return self.type
		except Exception as e: 
			print(traceback.format_exc())	

	def set_type(self, ty):
		try:
			self.type = ty
		except Exception as e: 
			print(traceback.format_exc())

	def jsonify(self):
		try: 
			return json.dumps(self.data.__dict__)
		except Exception as e: 
			print(traceback.format_exc())





