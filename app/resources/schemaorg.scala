package resources

import com.hp.hpl.jena.rdf.model.ModelFactory

class schema {
  
  protected val NS = "https://schema.org/"
  protected val m_model = ModelFactory.createDefaultModel();
  val Prefix = m_model.setNsPrefix("schema", NS)
}

object Place extends schema {

  val Place = m_model.createResource(NS + "Place")

  val address = m_model.createProperty(NS + "address")
  val geo = m_model.createProperty(NS + "geo")
  val photo = m_model.createProperty(NS + "photo")
  val alternateName = m_model.createProperty(NS + "alternateName")
  val description = m_model.createProperty(NS + "description")
  val sameAs = m_model.createProperty(NS + "sameAs")
  val name = m_model.createProperty(NS + "name")
}

object GeoCoordinates extends schema{

  val GeoCoordinates = m_model.createResource( NS + "GeoCoordinates")

  val latitude = m_model.createProperty(NS + "latitude")
  val longitude = m_model.createProperty(NS + "longitude")
}

object PostalAddress extends schema {

  val PostalAddress = m_model.createResource(NS + "PostalAddress")

  val addressCountry = m_model.createProperty(NS + "addressCountry")
  val addressLocality = m_model.createProperty(NS + "addressLocality")
  val addressRegion = m_model.createProperty(NS + "addressRegion")
  val postalCode = m_model.createProperty(NS + "postalCode")
  val streetAddress = m_model.createProperty(NS + "streetAddress")
}

object ImageObject extends schema {

  val ImageObject = m_model.createResource(NS + "ImageObject")

  val contentUrl = m_model.createProperty(NS + "contentUrl")
  val url = m_model.createProperty(NS + "url")
}