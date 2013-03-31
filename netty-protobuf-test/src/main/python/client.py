#!/usr/bin/python

import socket
import meta_pb2
import struct

def write():
    print "write"
    img = meta_pb2.Image()
    img.uuid = "b9e5f791-faa2-4c37-9f76-46f288ace593"
    img.filename = "test2.jpg"
    print "img created: \n" + str(img)
    if img.IsInitialized():
        data = img.SerializeToString()
        buf = struct.pack('>I', len(data))
        # print buf.encode("hex")
        buf += data
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(("127.0.0.1", 8888))
        print "connected"
        sock.sendall(buf)
        raw_input("Press ENTER to exit")
        sock.shutdown(socket.SHUT_RDWR)
        sock.close()


def read():
    print "read"
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect(("127.0.0.1", 8888))
    print "connected"
    size_buf = sock.recv(4)
    # print size_buf.encode("hex")
    size = struct.unpack('>I', size_buf)[0]
    data_buf = sock.recv(size, socket.MSG_WAITALL)
    print "get: " + str(size) + ", " + str(len(data_buf))
    sock.shutdown(socket.SHUT_RDWR)
    sock.close()
    img = meta_pb2.Image()
    img.ParseFromString(data_buf)
    print "img read: \n" + str(img)[:64]
    f = open("output.jpg", "w")
    f.write(img.data)
    f.close()



def main():
    read()
    write()


if __name__ == "__main__":
    main()
