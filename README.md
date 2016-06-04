# anubis-remoteagent
Remote execution agent to be used with anubis-server

## Prerequisites
Install [anubis-server](https://github.com/sbekti/anubis-server) first!

## Installation
Clone the repository:
~~~shell
git clone https://github.com/sbekti/anubis-remoteagent.git
cd anubis-remoteagent
~~~
Compile the project:
~~~shell
gradle fatJar
~~~
Edit the config file as necessary:
~~~shell
vim conf/agent.properties
~~~
Run the agent:
~~~shell
bin/start.sh
~~~

## Protocol

Send a JSON string containing the command to the input topic (the default is `remote_agent_requests`) to execute it on the remote side. You can use [anubis-client](https://github.com/sbekti/anubis-client) to send the stringified JSON.

Example JSON message body:
~~~json
{
  "requestId": "test-request",
  "nodeId": "d8af1316-dbc4-46a3-842d-838b0544f4b3",
  "command": ["/bin/bash", "-c", "docker run hello-world"]
}
~~~
After sending the command, you will get the execution result on the output topic (the default is `remote_agent_responses`). Because all executions happen asynchronously, you will need to correlate the requests and the responses based on their `requestId`.

## License

(The MIT License)

Copyright (c) 2016 Samudra Harapan Bekti <samudra.bekti@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
'Software'), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
