# Server
```console
docker build -t masss_server .
docker run --name MaSSS_Server -i -p 5002:5002 -v $PWD/db:/app/db masss_server 
```