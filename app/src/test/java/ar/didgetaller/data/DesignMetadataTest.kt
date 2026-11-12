package ar.didgetaller.data

import ar.didgetaller.core.AcousticModel
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class DesignMetadataTest {
    @Test fun repeatedSaveUpdatesOneIdentityAndPreservesOtherDesignsAndFutureFields() {
        val rows=JSONArray("[{\"id\":\"first\",\"name\":\"Original\",\"future\":17},{\"id\":\"second\",\"keep\":true}]")
        val other=rows.getJSONObject(1).toString()
        repeat(3){DesignMetadata.upsert(rows,"first","Río claro",AcousticModel.Shape())}
        assertEquals(2,rows.length());assertEquals(other,rows.getJSONObject(1).toString())
        assertEquals(17,rows.getJSONObject(0).getInt("future"));assertEquals("Río claro",rows.getJSONObject(0).getString("name"))
    }
    @Test fun legacyFallbackAndNewIdentitiesRetainExactProfilePoints() {
        val rows=JSONArray("[{\"name\":\"Old\",\"unknown\":\"keep\"},7]")
        val shape=AcousticModel.Shape().withControlPoints(listOf(AcousticModel.BorePoint(0,30.25),AcousticModel.BorePoint(1500,83.5)))
        DesignMetadata.upsert(rows,"legacy-0","Old",shape)
        DesignMetadata.upsert(rows,"new","New",shape)
        DesignMetadata.upsert(rows,"new","New",shape)
        assertEquals(3,rows.length());assertEquals(7,rows.getInt(1))
        assertEquals("keep",rows.getJSONObject(0).getString("unknown"))
        assertEquals(83.5,rows.getJSONObject(2).getJSONArray("points").getJSONArray(1).getDouble(1),0.0)
    }
    @Test fun invalidOrAmbiguousRequestsNeverMutateTheArray() {
        val rows=JSONArray("[{\"id\":\"same\"},{\"id\":\"same\"}]");val before=rows.toString()
        for((id,name) in listOf("" to "Name","id" to "","same" to "Name")) {
            try {DesignMetadata.upsert(rows,id,name,AcousticModel.Shape());fail("Invalid save")}catch(_:IllegalArgumentException){}
            assertEquals(before,rows.toString())
        }
    }
}
