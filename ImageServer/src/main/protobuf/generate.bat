@echo off
protoc.exe image.proto --cpp_out=. --java_out=. --python_out=.
@pause