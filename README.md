# SimpleReverseProxy

## Build

```
gradle fatJar
```

## Usage

To setup reverse proxy for https://hoge.com as http://127.0.0.1:8080

```
java -jar ./build/libs/simple_reverse_proxy.jar https://hoge.com 8080
```
