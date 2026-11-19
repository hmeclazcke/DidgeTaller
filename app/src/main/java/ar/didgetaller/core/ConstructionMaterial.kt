package ar.didgetaller.core

/** Admission checks for the material step. Fit and acoustic predictions remain separate. */
internal object ConstructionMaterial {
    enum class Problem { MISSING, INVALID }

    val stockDimensions = listOf("stockLength", "stockDiameter", "stockWidth", "stockThickness", "stockBlockThickness")

    fun woodDimensions(kind: String?) = when (kind) {
        "LOG" -> listOf("stockLength", "stockDiameter")
        "BOARDS" -> listOf("stockLength", "stockWidth", "stockThickness")
        "BLOCK" -> listOf("stockLength", "stockWidth", "stockBlockThickness")
        else -> emptyList()
    }

    fun maximumDimension(field: String) = if (field == "stockLength") 10000 else 2000

    fun issue(values: Map<String, String>): String? = problems(values).keys.firstOrNull()

    fun problems(values: Map<String, String>): Map<String, Problem> = buildMap {
        when (values["method"]) {
            "SANDWICH" -> putAll(woodProblems(values))
            "PVC" -> putAll(pvcProblems(values))
            "LAMINATED" -> Unit
            else -> put("method", Problem.MISSING)
        }
        putAll(settingsProblems(values))
        if (isEmpty()) {
            val options = runCatching { ConstructionForm.options(values) }.getOrNull()
            when {
                options == null -> put("options", Problem.INVALID)
                options.method == ConstructionMethod.PVC && !validAssembly(values, options) -> put("assembly", Problem.INVALID)
            }
        }
    }

    private fun woodProblems(values: Map<String, String>): Map<String, Problem> = buildMap {
        val dimensions = woodDimensions(values["stockKind"])
        if (dimensions.isEmpty()) {
            put("stockKind", Problem.MISSING)
            return@buildMap
        }
        dimensions.forEach { key ->
            required(values, key) { value ->
                value.trim().replace(',', '.').toDoubleOrNull()?.let { it in 1.0..maximumDimension(key).toDouble() } == true
            }
        }
        if (isEmpty() && values["stockConfirmed"] != "true") put("stockConfirmed", Problem.MISSING)
    }

    private fun pvcProblems(values: Map<String, String>): Map<String, Problem> = buildMap {
        required(values, "pipes") { text ->
            runCatching { ConstructionCatalog.pipes(text).values.all { it.insideMm in 15.0..220.0 } }.getOrDefault(false)
        }
        val fittings = values.getValue("fittings")
        when {
            fittings.isBlank() && ConstructionCatalog.usesFittings(values.getValue("assembly")) -> put("fittings", Problem.MISSING)
            runCatching { ConstructionCatalog.connectors(fittings) }.isFailure -> put("fittings", Problem.INVALID)
        }
        required(values, "assembly") { true }
        required(values, "clearance") { it.isAmount(0.0) }
    }

    private fun settingsProblems(values: Map<String, String>): Map<String, Problem> = buildMap {
        required(values, "interval") { it.toIntOrNull()?.let { number -> number in 50..500 } == true }
        if (values["method"] != "PVC") {
            required(values, "wall") { it.toDoubleOrNull()?.let { number -> number.isFinite() && number > 0.0 } == true }
            required(values, "tolerance") { it.isAmount(0.0) }
        }
    }

    private fun String.isAmount(minimum: Double) = toDoubleOrNull()?.let { it.isFinite() && it >= minimum } == true

    private fun MutableMap<String, Problem>.required(values: Map<String, String>, key: String, valid: (String) -> Boolean) {
        val value = values[key].orEmpty()
        when {
            value.isBlank() -> put(key, Problem.MISSING)
            !valid(value) -> put(key, Problem.INVALID)
        }
    }

    private fun validAssembly(values: Map<String, String>, options: ConstructionOptions) = runCatching {
        ConstructionProjects.create("PVC", ConstructionForm.shape(values), options)
    }.isSuccess
}
