
// web service to generate JSON from XML
var http = require('http')
var xml2js = require('xml2js')

// Port to listen for requests as TCP
const PORT = 8001
var connCount = 0
var parser = new xml2js.Parser({explicitArray: false})

// We need a function which handles requests and send response
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

    request.on('end', function () {
      parser.parseString(body, function (err, jsonResult) {
        var jsonStr = JSON.stringify(jsonResult, null, 4)
        response.end(jsonStr)
        if (err) {
          console.log(err)
        }
      })
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

