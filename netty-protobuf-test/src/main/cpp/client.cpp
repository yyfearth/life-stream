#include <iostream>

#include <boost/array.hpp>
#include <boost/asio.hpp>

#include "meta.pb.h"

// #include "exif.h"

using namespace std;

using boost::asio::ip::tcp;

const size_t BUF_LEN = 1024;

int main(int argc, char* argv[])
{
    if (argc != 3)
    {
      std::cerr << "Usage: client <host> <port>" << std::endl;
      return 1;
    }

    try {
        boost::asio::io_service io_service;

        tcp::resolver resolver(io_service);
        tcp::resolver::query query(argv[1], argv[2]);

        tcp::resolver::iterator endpoint_iterator = resolver.resolve(query);

        tcp::socket socket(io_service);
        boost::asio::connect(socket, endpoint_iterator);

        std::cout << "client connected" << std::endl;

        if (1) { // read
            boost::array<char, 1024> buf;
            boost::system::error_code error;

            size_t len = socket.read_some(boost::asio::buffer(buf), error);

            if (error == boost::asio::error::eof)
                len = 0; // Connection closed cleanly by peer.
            else if (error)
                throw boost::system::system_error(error); // Some other error.

            if (len) {
                std::cout << "get binary data: " << len << std::endl;
                std::cout.write(buf.data(), len);
                std::cout << std::endl;

                string str(buf.data(), len);
                meta::Image image;
                if (image.ParseFromString(str)) {
                    std::cout << image.DebugString() << std::endl;
                } else {
                    std::cout << "failed to parse" << std::endl;
                }
            } else
                std::cout << "no content recieved" << std::endl;
        }

        if (0) { // write
            meta::Image image;
            image.set_uuid("xxxxxxxxxxxxxxxxxxxxx");
            image.set_filename("test1.jpg");
            image.set_processed(true);
            string buf;
            image.SerializeToString(&buf);
            std::cout << "write " << buf.length() << std::endl << image.DebugString() << std::endl;
            google::protobuf::uint32 size(buf.length());
            boost::asio::write(socket, boost::asio::buffer(&size, sizeof(size)));
            boost::asio::write(socket, boost::asio::buffer(buf, buf.length()));
            std::cout << "sent" << std::endl;
        }

        while(1) sleep(100);
        return 0;

    }
    catch (std::exception& e)
    {
        std::cerr << e.what() << std::endl;
        return -1;
    }

}
