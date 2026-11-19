package ar.didgetaller.core

import ar.didgetaller.core.ConstructionMaterial.Problem

/** Projects the guided editor onto the existing catalog and assembly records. */
internal data class SinglePipeDraft(val inside: String = "", val outside: String = "", val length: String = "") {
    fun fields() = mapOf("pipeInside" to inside, "pipeOutside" to outside, "pipeLength" to length)

    fun problems(): Map<String, Problem> = buildMap {
        val inner = inside.toDoubleOrNull()
        val outer = outside.toDoubleOrNull()
        problem("pipeInside", inside, inner != null && inner in 15.0..220.0)
        problem("pipeOutside", outside, outer != null && outer.isFinite() && outer > maxOf(inner ?: 0.0, 0.0))
        problem("pipeLength", length, length.toIntOrNull()?.let { it in 500..10000 } == true)
    }

    fun record() = listOf(ID, inside, outside, length).joinToString(",")

    fun cutFor(designLength: Int): String? = length.toIntOrNull()?.takeIf { problems().isEmpty() }
        ?.let { "$ID,${minOf(it, designLength)},-" }

    companion object {
        const val ID = "measured-pipe"

        fun read(records: String): SinglePipeDraft? {
            if (records.isBlank()) return SinglePipeDraft()
            val row = records.trim().split(',')
            if (row.size != 4 || row[0] != ID || '\n' in records.trim()) return null
            return SinglePipeDraft(row[1], row[2], row[3])
        }

        fun fromFields(fields: Map<String, String>) = SinglePipeDraft(
            fields.getValue("pipeInside"), fields.getValue("pipeOutside"), fields.getValue("pipeLength"),
        )

        private fun MutableMap<String, Problem>.problem(key: String, text: String, valid: Boolean) {
            if (text.isBlank()) put(key, Problem.MISSING)
            else if (!valid) put(key, Problem.INVALID)
        }
    }
}

internal data class PvcCutDraft(val pipe: String = "", val length: String = "", val nextFitting: String = "") {
    companion object {
        fun pending(text: String): Boolean = read(text)?.let { cuts ->
            cuts.withIndex().any { (index, cut) ->
                cut.pipe.isBlank() || cut.length.isBlank() || (index < cuts.lastIndex && cut.nextFitting.isBlank())
            }
        } == true

        fun read(text: String): List<PvcCutDraft>? {
            if (text.isBlank()) return listOf(PvcCutDraft())
            val rows = runCatching { ConstructionCatalog.assemblyRows(text) }.getOrNull() ?: return null
            return rows.mapIndexed { index, (pipe, length, fitting) ->
                PvcCutDraft(pipe, length, if (index == rows.lastIndex) "" else fitting)
            }
        }

        fun records(cuts: List<PvcCutDraft>) = cuts.mapIndexed { index, cut ->
            listOf(cut.pipe, cut.length, if (index == cuts.lastIndex) "-" else cut.nextFitting).joinToString(",")
        }.joinToString("\n")
    }
}
