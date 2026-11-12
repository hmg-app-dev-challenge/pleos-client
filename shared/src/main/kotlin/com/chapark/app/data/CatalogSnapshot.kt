package com.chapark.app.data

/** An accepted local snapshot; replacement is allowed only when starting a new trip. */
data class CatalogSnapshot(val version:String,val places:List<NavigationPlace>,val evidence:Map<String,PlaceEvidence>)
object AcceptedCatalog { @Volatile var snapshot:CatalogSnapshot?=null; private set
    fun install(value:CatalogSnapshot){snapshot=value}
}
