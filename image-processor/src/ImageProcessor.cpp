#include <iostream>
#include <boost/array.hpp>
#include <boost/asio.hpp>
// #include "exif.h"

using boost::asio::ip::tcp;

int main(int argc, char* argv[])
{
  try
  {
    if (argc != 3)
    {
      std::cerr << "Usage: client <host> <port>" << std::endl;
      return 1;
    }

    boost::asio::io_service io_service;

    tcp::resolver resolver(io_service);
    tcp::resolver::query query(argv[1], argv[2]);
    tcp::resolver::iterator endpoint_iterator = resolver.resolve(query);

    tcp::socket socket(io_service);
    boost::asio::connect(socket, endpoint_iterator);

    for (;;)
    {
      boost::array<char, 128> buf;
      boost::system::error_code error;

      size_t len = socket.read_some(boost::asio::buffer(buf), error);

      if (error == boost::asio::error::eof)
        break; // Connection closed cleanly by peer.
      else if (error)
        throw boost::system::system_error(error); // Some other error.

      std::cout.write(buf.data(), len);
    }
  }
  catch (std::exception& e)
  {
    std::cerr << e.what() << std::endl;
  }

  return 0;
}

//int test(int argc, char *argv[]) {
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
