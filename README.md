HTWK Buildings Microservice
=================================

This microservice collects all known data about buildings of the University of Applied Science Leipzig and provisions it through a REST-JSON API.

The data is fetched, cleaned and enhanced every 24 houres to ensure latest data.

### Using this Service ###

Once your Server is running all you need to do is open your browser pointing to the host/port you just published and look at the raw JSON-data.

### Compilation/Running the Server  ###

Install the [Typesafe Acticator](//www.playframework.com/documentation/2.3.x/Installing).

For **developement mode**, execute the following commands:

- activator update
- activator run

If you wanna start this application in **production mode**, please note that this application was designed to be deployed with docker and instrumented (Metrics, ...) by NewRelic. Inside the project root, you'll find several scripts.

- dockerDeploy: cleans the project, prepare docker environment, copy {Dockerfile, NewRelic-Agent, NewRelicLicense}, build docker image
- dockerRun: Executes the image, build by dockerDeploy

The File "NewRelicLicense" is **missing** inside this repository. It's a file, containing your NewRelic APM License Code. This Code is needed for NewRelic Instrumentation.
