import tornado 
import datetime

class TimeoutWebSocketService():
    _default_timeout_delta_ms = 10 * 60 * 1000  # 10 min

    def __init__(self, websocket, ioloop=None, timeout=None):
        # Timeout
        self.ioloop = ioloop or tornado.ioloop.IOLoop.current()
        self.websocket = websocket
        self._timeout = None
        self._timeout_delta_ms = timeout or TimeoutWebSocketService._default_timeout_delta_ms

    def _close_on_timeout(self):
        self._timeout = None
        if self.websocket.ws_connection:
            self.websocket.close()

    def refresh_timeout(self, timeout=None):
        timeout = timeout or self._timeout_delta_ms
        if timeout > 0:
            # Clean last timeout, if one exists
            self.clean_timeout()

            # Add a new timeout (must be None from clean).
            self._timeout = self.ioloop.add_timeout(
                datetime.timedelta(milliseconds=timeout), self._close_on_timeout)

    def clean_timeout(self):
        if self._timeout is not None:
            # Remove previous timeout, if one exists.
            self.ioloop.remove_timeout(self._timeout)
            self._timeout = None