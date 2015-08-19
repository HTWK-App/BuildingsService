HTWK Buildings Microservice
=================================
[![Build Status](https://snap-ci.com/HTWK-App/BuildingsService/branch/master/build_image)](https://snap-ci.com/HTWK-App/BuildingsService/branch/master)
[![License](https://img.shields.io/badge/license-GPLv3-blue.svg)](https://github.com/HTWK-App/BuildingsService/blob/master/LICENSE)
[![Language](https://img.shields.io/badge/language-Scala%20(2.11.7)-blue.svg)](http://www.scala-lang.org/)
[![Framework](https://img.shields.io/badge/framework-PlayFramework%20(2.3.10)-blue.svg)](https://www.playframework.com/)
[![Lines of Code](https://img.shields.io/badge/loc-592-lightgrey.svg)](https://github.com/HTWK-App/BuildingsService/tree/master/app)

This microservice collects all known data about buildings of the [University of Applied Science Leipzig](https://www.htwk-leipzig.de/en) and provisions it through a REST-JSON and Linked-Data API.

The data is fetched, cleaned and enhanced every 24 houres to ensure latest updates.

### Using this Service ###

Once your Server is running all you need to do is open your browser pointing to the host/port you just published and look at the raw JSON-data. The default Port is 9000, so you got to call:

``` http://localhost:9000/buildings ```

in a Webbrowser.

As Linked-Data, RDF/XML, Turtle and Notation-3 are provided. You can access them from the commandline by calling the following with the right Accept header:

```
curl -H "Accept: application/rdf+xml" http://localhost:9000/buildings
```

### Compilation/Running the Server  ###

Install the [Typesafe Acticator](//www.playframework.com/documentation/2.3.x/Installing).

For **developement mode**, execute the following commands:

```
# May take some time...
activator update
activator run
```

To package the application for **production mode**, execute the following command. You will be told where the resulting dist zip is placed. Inside this zip, theres a run script. Start it and your ready to go.

```
activator dist
```

Please note that this application was designed to be deployed with docker and instrumented (Metrics, ...) by NewRelic. Inside the projects root, you'll find a scipt named **dockerDeploy.sh**. This script will build a docker image, containing the app, and try to start it with the tool docker-compose. The needed compose file is not (!!!) included in this repository, so this call will fail. Execute the following to start the docker image:

```
docker run -it --rm -p 9000:9000 rmeissn/buildings:latest
```
