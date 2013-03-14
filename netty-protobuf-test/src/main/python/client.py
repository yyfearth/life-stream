#!/usr/bin/python

import socket
import meta_pb2

def write():
    print "write"
    img = meta_pb2.Image()
    img.uuid = "xxxxxxxx"
    img.filename = "test2.jpg"
    print "img created: \n" + str(img)
    if img.IsInitialized():
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(("127.0.0.1", 8888))
        print "connected"
        sock.sendall(img.SerializeToString())
        sock.close()
        print "sent"


def read():
    print "read"
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect(("127.0.0.1", 8888))
    print "connected"
    pb_data = sock.recv(1024)
    #while 1:
    #    data = sock.recv(1024)
    #    if not data:
    #        break
    #    pb_data += data
    print 'get all data: \n"' + pb_data + '"'
    sock.close()
    img = meta_pb2.Image()
    img.ParseFromString(pb_data)
    print "img read: \n" + str(img)


def main():
    read()
    write()


if __name__ == "__main__":
    main()
