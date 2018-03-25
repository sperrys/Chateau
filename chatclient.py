#!/usr/bin/env python3

import time
import sys

class ChatClient():
    def __init__(self, WebSocketHandler):
        self.sock = WebSocketHandler
        self.username = ""

    def set_username(username):
        self.username = username

    def get_username():
        return self.username
