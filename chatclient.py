#!/usr/bin/env python3

import time
import sys


class Clients():
	def __init__(self):
		self.clients = []

	# Add a new client to the clients, 
	# assumes that username is unique
	def add(self, ChatClient):
		self.clients.append(ChatClient)


	# Finds client based on username returns 
	# client if found, None if not
	def find(self, ChatClient):
		for c in self.clients:
			if c.username == username:
				return c
		return None

	# Remove Client Based on Username,
	# assumes unique usernames
	def remove(self, ChatClient)
		for c in self.clients:
			if c.username == username: 
				self.clients.remove(c) 
				return True
		return False

	# checks whether a given username is 
	# already taken or not, uses find_client
	def unique_username(self, username):
		if self.find(username) == None:
			return True
		else:
			return False


class ChatClient():
    def __init__(self, WebSocketHandler):
        self.sock = WebSocketHandler
        self.username = ""
        self.registered = False

    def set_username(username):
        self.username = username

    def get_username():
        return self.username
