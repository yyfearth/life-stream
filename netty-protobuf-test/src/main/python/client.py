#!/usr/bin/python

import sys
import socket
import meta_pb2
import struct

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
        data = img.SerializeToString()
        size = sys.getsizeof(data)
        size_buf = struct.pack('>I', size)
        sock.send(size_buf)
        sock.sendall(data)
        raw_input("Press ENTER to exit")
        sock.close()
        print "sent: " + str(size)


def read():
    print "read"
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect(("127.0.0.1", 8888))
    print "connected"
    size_buf = sock.recv(4)
    size = struct.unpack('>I', size_buf)[0]
    pb_data = sock.recv(size)
    print "get: " + str(size)
    sock.close()
    img = meta_pb2.Image()
    img.ParseFromString(pb_data)
    print "img read: \n" + str(img)


def main():
#    read()
    write()


if __name__ == "__main__":
    main()
