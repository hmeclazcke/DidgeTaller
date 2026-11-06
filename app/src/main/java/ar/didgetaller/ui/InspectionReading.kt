package ar.didgetaller.ui

/** Explicit inspection evidence takes precedence inside the detail sheet. */
internal fun inspectionReading(inspected:String?,timeline:String):String = inspected ?: timeline
