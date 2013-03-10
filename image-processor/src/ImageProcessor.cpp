#include <iostream>
#include <stdio.h>
#include <ostream>

#include <boost/thread.hpp>
#include <boost/bind.hpp>
#include <boost/array.hpp>
#include <boost/asio.hpp>

#include "meta.pb.h"

// #include "exif.h"

using namespace std;

using boost::asio::ip::tcp;

const size_t BUF_LEN = 1024;

class Client
{
public:
    Client(boost::asio::io_service& io_service, tcp::resolver::iterator endpoint_iterator);

    void write();
    void close();
private:
    boost::asio::io_service& io_service;
    tcp::socket socket;

    boost::array<char, BUF_LEN> buffer;

protected:
    void on_connect(const boost::system::error_code& err_code,
            tcp::resolver::iterator endpoint_iterator);

    void on_receive(const boost::system::error_code& err_code);

    void on_write(const boost::system::error_code& error);

    void do_close();
};

Client::Client(boost::asio::io_service& io_svc, tcp::resolver::iterator endpoint_iterator)
: io_service(io_svc), socket(io_svc)
{

    tcp::endpoint endpoint = *endpoint_iterator;

    socket.async_connect(endpoint,
            boost::bind(&Client::on_connect, this, boost::asio::placeholders::error, ++endpoint_iterator));
}

void Client::close()
{
    std::cout << "close requested" << std::endl;
    io_service.post(boost::bind(&Client::do_close, this));
}

void Client::on_connect(const boost::system::error_code& err_code,
    tcp::resolver::iterator endpoint_iterator)
{
    if (err_code == 0)
    // Successful connected
    {
        std::cout << "connected" << std::endl;

        write();

        socket.async_receive(boost::asio::buffer(buffer.data(), BUF_LEN),
                boost::bind(&Client::on_receive, this, boost::asio::placeholders::error));


    } else if (endpoint_iterator != tcp::resolver::iterator())
    {
        std::cout << "connect failed" << std::endl;
        socket.close();
        tcp::endpoint endpoint = *endpoint_iterator;

        socket.async_connect(endpoint,
                boost::bind(&Client::on_connect, this, boost::asio::placeholders::error, ++endpoint_iterator));
    }
}

void Client::on_receive(const boost::system::error_code& err_code)
{
    if (err_code == 0)
    {
        string str(buffer.data());
        std::cout << "received: " << str.length() << std::endl;

        meta::Image image;
        if (image.ParseFromString(str)) {
            std::cout << image.DebugString() << std::endl;
        } else {
            std::cout << "failed to parse" << std::endl;
        }

        socket.async_receive(boost::asio::buffer(buffer.data(), BUF_LEN),
                boost::bind(&Client::on_receive, this, boost::asio::placeholders::error));
    } else {
        do_close();
    }
}

void Client::write()
{
    meta::Image image;
    image.set_uuid("xxxxxxxxxxxxxxxxxxxxx");
    image.set_filename("test1.jpg");
    image.set_processed(true);
    string buf;
    image.SerializeToString(&buf);
    std::cout << "write " << buf.length() << std::endl << image.DebugString() << std::endl;
    boost::asio::write(socket, boost::asio::buffer(buf.c_str(), buf.length()));
    // boost::asio::async_write(socket,
    //     boost::asio::buffer(buf, buf.length()),
    //     boost::bind(&Client::on_write, this, boost::asio::placeholders::error));

}

void Client::on_write(const boost::system::error_code& error)
{
    if (!error)
    {
        // meta::Image image;
        // image.set_uuid("xxxxxxxxxxxxxxxxxxxxx");
        // image.set_filename("test1.jpg");
        // image.set_processed(true);
        // string buf;
        // image.SerializeToString(&buf);
        // boost::asio::async_write(socket,
        //     boost::asio::buffer(buf, buf.length()),
        //     boost::bind(&Client::on_write, this, boost::asio::placeholders::error));
    }
    else
    {
        do_close();
    }
}

void Client::do_close()
{
    std::cout << "do close" << std::endl;
    socket.close();

}

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

        tcp::resolver::iterator endpoint_iteratorator = resolver.resolve(query);

        Client client(io_service, endpoint_iteratorator);

        boost::thread thread(
                boost::bind(&boost::asio::io_service::run, &io_service));

        std::cout << "Client started." << std::endl;

        std::string input;
        while (input != "exit")
        {
            std::cin >> input;
        }

        client.close();
        thread.join();
    } catch (std::exception& e)
    {
        std::cerr << e.what() << std::endl;
    }

}

//int exif(int argc, char *argv[]) {
//  if (argc < 2) {
//    printf("Usage: demo <JPEG file>\n");
//    return -1;
//  }
//
//  // Read the JPEG file into a buffer
//  FILE *fp = fopen(argv[1], "rb");
//  if (!fp) {
//    printf("Can't open file.\n");
//    return -1;
//  }
//  fseek(fp, 0, SEEK_END);
//  unsigned long fsize = ftell(fp);
//  rewind(fp);
//  unsigned char *buf = new unsigned char[fsize];
//  if (fread(buf, 1, fsize, fp) != fsize) {
//    printf("Can't read file.\n");
//    delete[] buf;
//    return -2;
//  }
//  fclose(fp);
//
//  // Parse EXIF
//  EXIFInfo result;
//  int code = result.parseFrom(buf, fsize);
//  delete[] buf;
//  if (code) {
//    printf("Error parsing EXIF: code %d\n", code);
//    return -3;
//  }
//
//  // Dump EXIF information
//  printf("Camera make       : %s\n", result.Make.c_str());
//  printf("Camera model      : %s\n", result.Model.c_str());
//  printf("Software          : %s\n", result.Software.c_str());
//  printf("Bits per sample   : %d\n", result.BitsPerSample);
//  printf("Image width       : %d\n", result.ImageWidth);
//  printf("Image height      : %d\n", result.ImageHeight);
//  printf("Image description : %s\n", result.ImageDescription.c_str());
//  printf("Image orientation : %d\n", result.Orientation);
//  printf("Image copyright   : %s\n", result.Copyright.c_str());
//  printf("Image date/time   : %s\n", result.DateTime.c_str());
//  printf("Original date/time: %s\n", result.DateTimeOriginal.c_str());
//  printf("Digitize date/time: %s\n", result.DateTimeDigitized.c_str());
//  printf("Subsecond time    : %s\n", result.SubSecTimeOriginal.c_str());
//  printf("Exposure time     : 1/%d s\n", (unsigned) (1.0/result.ExposureTime));
//  printf("F-stop            : f/%.1f\n", result.FNumber);
//  printf("ISO speed         : %d\n", result.ISOSpeedRatings);
//  printf("Subject distance  : %f m\n", result.SubjectDistance);
//  printf("Exposure bias     : %f EV\n", result.ExposureBiasValue);
//  printf("Flash used?       : %d\n", result.Flash);
//  printf("Metering mode     : %d\n", result.MeteringMode);
//  printf("Lens focal length : %f mm\n", result.FocalLength);
//  printf("35mm focal length : %u mm\n", result.FocalLengthIn35mm);
//  printf("GPS Latitude      : %f deg\n", result.GeoLocation.Latitude);
//  printf("GPS Longitude     : %f deg\n", result.GeoLocation.Longitude);
//  printf("GPS Altitude      : %f m\n", result.GeoLocation.Altitude);
//
//  return 0;
//}
