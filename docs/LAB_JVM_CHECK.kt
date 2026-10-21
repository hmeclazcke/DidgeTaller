import ar.didgetaller.core.AcousticModel
import ar.didgetaller.core.BoreOptimizer
import kotlin.math.abs
import kotlin.system.measureTimeMillis

fun main() {
    val pvc = AcousticModel.Shape(1500, 40, 40, 40)
    val conical = AcousticModel.Shape(1500, 32, 60, 95)
    val p = AcousticModel.evaluate(pvc)
    val c = AcousticModel.evaluate(conical)
    println("PVC resonancias="+p.frequenciesHz.take(5))
    println("Conico resonancias="+c.frequenciesHz.take(5))
    check(p.impedance.size == 621)
    check(p.baseHz in 40.0..125.0) { "fundamental física inesperada: ${p.baseHz}" }
    check(abs(p.baseHz-c.baseHz) > 0.15)
    check(p.frequenciesHz.zipWithNext().all { (a,b)->b>a })
    val detailed=pvc.withControlPoints(listOf(
        AcousticModel.BorePoint(0, 32.0),
        AcousticModel.BorePoint(500, 34.0),
        AcousticModel.BorePoint(900, 50.0),
        AcousticModel.BorePoint(1500, 85.0)
    ))
    check(detailed.points.size == 4)
    check(detailed.diameter(700) > 34 && detailed.diameter(700) < 50)
    check(detailed.csv().lines().size == 6)
    var steps=0
    var firstError=0.0
    val duration=measureTimeMillis {
        val best=BoreOptimizer.search(conical,
            BoreOptimizer.Target(73.42,174.61,293.66),
            BoreOptimizer.Settings(generations=8,population=10),
            progress={ gen, candidate -> if(gen==1)firstError=candidate.fitness; steps=gen }
        ) ?: error("Unexpected cancel")
        println("optimizer fitness inicial=$firstError best=${best.fitness}; peaks=${best.peaks.take(4)}; shape=${best.shape}")
        check(best.fitness <= firstError + 1e-5)
        check(best.shape.points.size == 6)
        check(steps == 8)
    }
    println("Optimización 8*10 duró ${duration}ms")
    val cancelled=BoreOptimizer.search(pvc,BoreOptimizer.Target(),BoreOptimizer.Settings(2,4),cancelled={true})
    check(cancelled==null)
    println("PASS")
}
