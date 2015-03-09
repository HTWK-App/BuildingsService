HTWK Buildings Microservice
=================================

This microservice collects all known data about buildings of the University of Applied Science Leipzig and provisions it through a REST-JSON API.

The data is fetched, cleaned and enhanced every 24 houres to ensure latest data.

### Using this Service ###

Once your Server is running all you need to do is open your browser pointing to the host/port you just published and look at the raw JSON-data.

### Compilation/Running the Server  ###

All you have to do is to install the [Typesafe Acticator](//www.playframework.com/documentation/2.3.x/Installing) and subsequent execute the following commands:

- activator update
- activator run

If you wanna package the Application and deploy it to some kind of Server-System, please have a look at the [Activator documentation](//typesafe.com/activator/docs).

