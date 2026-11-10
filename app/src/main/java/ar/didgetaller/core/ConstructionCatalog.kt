package ar.didgetaller.core

/** Explicit measured CSV, not a commercial availability database. Names are unique identifier tokens. */
object ConstructionCatalog {
    data class Connector(val fitting:WorkshopPlans.Fitting,val profile:List<WorkshopPlans.Station>)

    private fun rows(text:String):List<List<String>> {
        require(text.length<=64000)
        return text.lineSequence().map {it.trim()}.filter {it.isNotEmpty()}.map {it.split(',').map(String::trim)}.toList()
    }
    private fun identifier(id:String):String {
        require(id.matches(Regex("[A-Za-z0-9_-]{1,40}")))
        return id
    }
    fun pipes(text:String):Map<String,WorkshopPlans.Pipe> {
        val values=rows(text).map {r->
            require(r.size==4)
            WorkshopPlans.Pipe(identifier(r[0]),r[1].toDouble(),r[2].toDouble(),r[3].toInt())
        }
        require(values.size<=100 && values.map {it.id}.distinct().size==values.size)
        return values.associateBy {it.id}
    }
    fun connectors(text:String):Map<String,Connector> {
        val values=rows(text).map(::connector)
        require(values.size<=100 && values.map {it.fitting.id}.distinct().size==values.size)
        return values.associateBy {it.fitting.id}
    }
    private fun connector(r:List<String>):Connector {
        require(r.size==8)
        val profile=r[7].split('|').map {p->
            val fields=p.split(':');require(fields.size==2)
            WorkshopPlans.Station(fields[0].toInt(),fields[1].toDouble())
        }
        require(profile.size in 1..32)
        val f=WorkshopPlans.Fitting(identifier(r[0]),r[1].toDouble(),r[2].toDouble(),r[3].toInt(),r[4].toInt(),r[5].toInt(),r[6].toDouble())
        require(profile.first().positionMm==0 && profile.last().positionMm==f.bodyLengthMm-f.leftInsertionMm-f.rightInsertionMm)
        require(profile.zipWithNext().all {(a,b)->b.positionMm>a.positionMm})
        require(profile.all {it.diameterMm in 15.0..220.0})
        require(kotlin.math.abs(profile.minOf {it.diameterMm}-f.passageMm)<1e-8)
        return Connector(f,profile)
    }
    fun plan(name:String,shape:AcousticModel.Shape,o:ConstructionOptions):ConstructionPlan {
        val pipes=pipes(o.pipes);val fittings=connectors(o.fittings)
        val assembly=rows(o.assembly)
        require(assembly.size in 1..100 && assembly.all {it.size==3})
        require(assembly.last()[2]=="-")
        val cuts=assembly.map {r->WorkshopPlans.Cut(requireNotNull(pipes[r[0]]),r[1].toInt())}
        val connections=assembly.dropLast(1).map {requireNotNull(fittings[it[2]])}
        val measured=WorkshopPlans.pvc(cuts,connections.map {it.fitting},o.socketClearanceMm)
        val pieces=pieces(cuts,connections)
        require(kotlin.math.abs(pieces.sumOf {it.lengthMm}-measured.lengthMm)<1e-8)
        val parts=cuts.mapIndexed {i,c->"${i+1}: ${c.pipe.id}; L=${c.lengthMm} mm; ID=${c.pipe.insideMm}; OD=${c.pipe.outsideMm}; Lmax=${c.pipe.stockLengthMm} mm"}+
            connections.mapIndexed {i,c->"${c.fitting.id}: x=${measured.fittingStartsMm[i]} mm; L=${c.fitting.bodyLengthMm}; I=${c.fitting.leftInsertionMm}/${c.fitting.rightInsertionMm} mm; ID(x)=${connections[i].profile.joinToString("|"){"${it.positionMm}:${it.diameterMm}"}}"}
        return ConstructionPlan(name,shape,o,pieces,parts)
    }
    private fun pieces(cuts:List<WorkshopPlans.Cut>,connectors:List<Connector>):List<ConstructionPiece> = buildList {
        cuts.forEachIndexed {i,c->
            add(ConstructionPiece(c.pipe.id,c.lengthMm.toDouble(),c.pipe.insideMm,c.pipe.insideMm))
            connectors.getOrNull(i)?.let {connector->connector.profile.zipWithNext().forEach {(a,b)->
                add(ConstructionPiece(connector.fitting.id,(b.positionMm-a.positionMm).toDouble(),a.diameterMm,b.diameterMm))
            }}
        }
    }
}
