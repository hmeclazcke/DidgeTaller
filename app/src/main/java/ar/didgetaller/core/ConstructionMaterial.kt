package ar.didgetaller.core

/** Admission checks for the material step. Fit and acoustic predictions remain separate. */
internal object ConstructionMaterial {
    val stockDimensions = listOf("stockLength", "stockDiameter", "stockWidth", "stockThickness", "stockBlockThickness")

    fun woodDimensions(kind: String?) = when (kind) {
        "LOG" -> listOf("stockLength", "stockDiameter")
        "BOARDS" -> listOf("stockLength", "stockWidth", "stockThickness")
        "BLOCK" -> listOf("stockLength", "stockWidth", "stockBlockThickness")
        else -> emptyList()
    }

    fun invalidDimension(field: String, value: String) =
        value.trim().replace(',', '.').toDoubleOrNull()?.let { it !in stockRange(field) } ?: true

    private fun stockRange(field: String): ClosedFloatingPointRange<Double> = when (field) {
        "stockLength" -> 1.0..10000.0
        else -> 1.0..2000.0
    }

    fun issue(values: Map<String, String>): String? = when (values["method"]) {
        "SANDWICH" -> woodIssue(values) ?: optionsIssue(values)
        "PVC" -> pvcIssue(values) ?: optionsIssue(values)
        "LAMINATED" -> optionsIssue(values)
        else -> "method"
    }

    private fun woodIssue(values: Map<String, String>): String? {
        val dimensions = woodDimensions(values["stockKind"])
        if (dimensions.isEmpty()) return "stockKind"
        dimensions.firstOrNull { invalidDimension(it, values.getValue(it)) }?.let { return it }
        return "stockConfirmed".takeUnless { values["stockConfirmed"] == "true" }
    }

    private fun pvcIssue(values: Map<String, String>): String? {
        val pipes = runCatching { ConstructionCatalog.pipes(values.getValue("pipes")) }.getOrNull()
        if (pipes.isNullOrEmpty() || pipes.values.any { it.insideMm !in 15.0..220.0 }) return "pipes"
        if (runCatching { ConstructionCatalog.connectors(values.getValue("fittings")) }.isFailure) return "fittings"
        val clearance = values["clearance"]?.toDoubleOrNull()
        if (clearance == null || !clearance.isFinite() || clearance < 0.0) return "clearance"
        return null
    }

    private fun optionsIssue(values: Map<String, String>): String? {
        val options = runCatching { ConstructionForm.options(values) }.getOrElse { return "options" }
        if (options.intervalMm !in 50..500) return "interval"
        return "assembly".takeIf { options.method == ConstructionMethod.PVC && !validAssembly(values, options) }
    }

    private fun validAssembly(values: Map<String, String>, options: ConstructionOptions) = runCatching {
        ConstructionProjects.create("PVC", ConstructionForm.shape(values), options)
    }.isSuccess
}
