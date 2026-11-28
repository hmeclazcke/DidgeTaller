package ar.didgetaller.study

import ar.didgetaller.core.*

/** Frozen research oracle is intentionally independent of Android implementation changes. */
internal fun checkAndroidHarmonicParity() {
    val reference = InstrumentProfile.entries.associateWith { EvidenceReader(it.windowSamples, Route.PERIOD) }
    val engines = InstrumentProfile.entries.associateWith { HarmonicYin(it.windowSamples) }
    var count = 0
    for (observation in challenge(true) + independentSignals(true).map { it.trial }) {
        val expected = reference.getValue(observation.profile).read(observation).pitch
        val t = observation
        var diagnostic: PitchDetector.Diagnostic? = null
        val raw = PitchDetector.analyzeWithDiagnostics(t.pcm, t.rate, t.profile.minHz, t.upperHz, false) { diagnostic = it }
        val result = engines.getValue(t.profile).estimate(t.pcm, t.rate, t.profile, t.upperHz, raw.copy(diagnostic = diagnostic))
        val actual = Decision(result.hz, result.rank)
        check(expected == actual) { "Frame $count: expected $expected, observed $actual" }
        count++
    }
    println("Exact pitch and score parity on $count frames")
    for (profile in InstrumentProfile.entries) for (algorithm in PitchAlgorithm.entries)
        streamingAllocation(profile, algorithm)
}

private fun streamingAllocation(profile: InstrumentProfile, algorithm: PitchAlgorithm) {
    val bean = java.lang.management.ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
    val stream = PitchStream(profile, algorithm = algorithm)
    val tone = challenge(true).first { it.profile == profile }.pcm
    val chunks = tone.asList().chunked(4096).map { it.toShortArray() }
    repeat(64) { stream.append(chunks[it % chunks.size]) }
    val costs = mutableListOf<Long>()
    repeat(96) {
        val start = bean.currentThreadAllocatedBytes
        val reading = stream.append(chunks[it % chunks.size])
        val bytes = bean.currentThreadAllocatedBytes - start
        if (reading != null) costs += bytes
    }
    println("HotSpot median allocated bytes per analyzed frame: $profile $algorithm ${costs.sorted()[costs.size/2]}")
}
