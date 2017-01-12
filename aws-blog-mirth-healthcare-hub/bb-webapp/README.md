#bb-webapp service

Used in the Mirth Channel examples.

This HTTP listener receives a POST from Mirth Connect and converts the content to JSON

####Installation:

```
npm install http querystring blue-button
node bb-conversion-listener.js &
```

By default this service will listen for connections on localhost port 8000
