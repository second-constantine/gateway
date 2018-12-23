# Gateway
**tags:** kotlin, gateway, ngrok

**how build:**
```
gradle clean build
```

**example how use:**

start server
```
./build/libs/gateway.jar 1222 2122
```
start client
```
./build/libs/gateway.jar yourhost.com 1222 localhost 22
```
connect by ssh
```
ssh yourhost.com -p 2122
```
