
// Lets require/import the HTTP module
var http = require('http')
var bb = require('blue-button')

// Lets define a port we want to listen to
const PORT = 8000
var connCount = 0

// We need a function which handles requests and sends a response
function handleRequest (request, response) {
  connCount++
  if (request.method === 'POST') {
    var body = []
    request.on('data', function (chunk) {
      body.push(chunk)
    }).on('end', function () {
      body = Buffer.concat(body).toString()
// at this point, `body` has the entire request body stored in it as a string
    })

    console.log('Processing Connection: ' + connCount)

// at the end of the request, prase the CCD and then return
// pretty print version of JSON
    request.on('end', function () {
      var json = bb.parse(body)
      response.end(JSON.stringify(json, null, 4))
    })
  } else {
    response.end('Issue processing: ' + request.url)
  }
}

// Create a server
var server = http.createServer(handleRequest)

// Lets start our server
server.listen(PORT, function () {
    // Callback triggered when server is successfully listening. Hurray!
  console.log('Server listening on: http://localhost:%s', PORT)
})

