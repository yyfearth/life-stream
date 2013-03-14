#!/usr/bin/python

import socket
import meta_pb2

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

def server():
    sock = socket.socket()
    sock.bind(('', 80))
    serversocket.listen(2)
    while 1:
    #accept connections from outside
    (clientsocket, address) = serversocket.accept()
    #now do something with the clientsocket
    #in this case, we'll pretend this is a threaded server
    ct = client_thread(clientsocket)
    ct.run()

def main():
    server()


if __name__ == "__main__":
    main()
