package controllers

import java.util.concurrent.TimeUnit

import org.influxdb.InfluxDBFactory
import org.influxdb.dto.Serie

object InfluxDB {

  private val influxDB = InfluxDBFactory.connect("http://localhost:8086", "data", "data")

  def logAccess(path: String) = {
    val serie = new Serie.Builder("access.buildings").columns(path).values(1.toString()).build()
    influxDB.write("data", TimeUnit.MILLISECONDS, serie);
  }
  def logError(message: String) = {
    val serie = new Serie.Builder("logger.buildings").columns("error").values(message).build()
    influxDB.write("data", TimeUnit.MILLISECONDS, serie);
  }
  
  def logInfo(message: String) = {
    val serie = new Serie.Builder("logger.buildings").columns("info").values(message).build()
    influxDB.write("data", TimeUnit.MILLISECONDS, serie);
  }
}