if (Test-Path .\output)
{
    Remove-Item -Path output -Recurse
}

New-Item -Path output -ItemType directory
Start-Process -FilePath protoc.exe -ArgumentList "image.proto --cpp_out=./output --java_out=./output --python_out=./output" -NoNewWindow
Start-Process -FilePath protoc.exe -ArgumentList "heartbeat.proto --cpp_out=./output --java_out=./output --python_out=./output" -NoNewWindow
pause