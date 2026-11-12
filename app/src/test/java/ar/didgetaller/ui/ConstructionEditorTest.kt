package ar.didgetaller.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import ar.didgetaller.core.*
import ar.didgetaller.data.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class ConstructionEditorTest {
    private val shape=AcousticModel.Shape()
    private fun values()=ConstructionForm.defaults(shape)+mapOf("stockKind" to "BOARDS","stockConfirmed" to "true")
    private fun editor(context:Context,scope:CoroutineScope,values:Map<String,String> = values(),load:Result<Map<String,String>> = Result.success(values)):ConstructionEditor {
        val initial=ConstructionJourney.restore(values)
        return ConstructionEditor(context,LocalStore.Design("same-id","",shape),mutableStateOf(initial),initial,load,scope)
    }
    private suspend fun ready(editor:ConstructionEditor) {
        withTimeout(30000) {while(editor.analysis is AnalysisState.Loading)yield()}
        assertTrue(editor.analysis is AnalysisState.Ready)
    }
    @Test fun savesUpdateIdentityAndResumeWithoutDroppingOtherDesigns()=runBlocking {
        val context=MemoryContext()
        LocalStore(context).saveDesign("other","Other",shape)
        val e=editor(context,this)
        e.set("planName","Named instrument");e.select(1)
        assertTrue(e.save(WorkshopLanguage.ENGLISH));assertTrue(e.save(WorkshopLanguage.ENGLISH))
        assertEquals(2,LocalStore(context).designs().size)
        assertEquals("Named instrument",LocalStore(context).designs().first {it.id=="same-id"}.name)
        val loaded=ConstructionJourney.restore(ConstructionStore(context).project("same-id"))
        assertEquals(1,loaded.step);assertEquals(e.draft.value.values,loaded.values)
        e.dispose()
    }
    @Test fun editsRevokeReadyPlansAndRefreshActualGeometry()=runBlocking {
        val e=editor(MemoryContext(),this)
        e.select(2);e.prepare(WorkshopLanguage.ENGLISH);ready(e)
        val old=e.current!!.plan;assertTrue(e.canPlan)
        e.set("geometry","0,32\n1020,43\n1500,90")
        assertNull(e.current);assertFalse(e.canPlan);assertFalse(e.available(3))
        e.export(old,"csv",ConstructionPdf.Labels(WorkshopLanguage.ENGLISH))
        assertSame(AnalysisState.Idle,e.exported)
        e.prepare(WorkshopLanguage.ENGLISH);ready(e)
        assertEquals(90.0,e.current!!.plan.ideal.diameter(1500),0.0)
        e.select(3);assertEquals(3,e.draft.value.step)
        e.back{};assertEquals(2,e.draft.value.step);e.dispose()
    }
    @Test fun pvcNeedsApprovalAndNeverOverwritesTheTargetBore()=runBlocking {
        val e=editor(MemoryContext(),this,values()+mapOf("method" to "PVC","pipes" to "a,32,40,1800","assembly" to "a,1500,-"))
        e.run("plan",WorkshopLanguage.ENGLISH);ready(e)
        assertFalse(e.canPlan);val ideal=e.current!!.plan.ideal
        e.approve(true);assertTrue(e.canPlan);assertEquals(ideal,e.current!!.plan.ideal)
        e.export(e.current!!.plan,"csv",ConstructionPdf.Labels(WorkshopLanguage.ENGLISH))
        withTimeout(30000){while(e.exported is AnalysisState.Loading)yield()}
        assertTrue(e.exported is AnalysisState.Ready);e.acknowledgeExport();assertSame(AnalysisState.Idle,e.exported)
        assertEquals("73.42",e.draft.value.values["target"])
        e.approve(false);assertFalse(e.canPlan)
        e.choose(ConstructionCandidate(e.current!!.plan,1.0,emptyList(),1.0,0.0))
        assertFalse(e.draft.value.accepted);assertNull(e.current)
        e.dispose()
    }
    @Test fun cancelledOrDisposedWorkersCannotPublishOrRestart()=runBlocking {
        val e=editor(MemoryContext(),this)
        e.run("plan",WorkshopLanguage.ENGLISH);e.cancel();yield()
        assertTrue(e.analysis is AnalysisState.Cancelled);assertNull(e.current)
        e.set("target","80");e.select(2);e.prepare(WorkshopLanguage.ENGLISH)
        e.dispose();yield();assertSame(AnalysisState.Disposed,e.analysis)
        e.run("plan",WorkshopLanguage.ENGLISH);e.prepare(WorkshopLanguage.ENGLISH)
        assertSame(AnalysisState.Disposed,e.analysis);assertSame(AnalysisState.Disposed,e.exported)
    }
    @Test fun unreadableSourceCannotAuthorizePersistenceAndUnsavedWorkPrompts()=runBlocking {
        val context=MemoryContext()
        val e=editor(context,this,load=Result.failure(IllegalArgumentException("Unreadable")))
        assertFalse(e.save(WorkshopLanguage.ENGLISH));assertTrue(e.failed)
        assertTrue(LocalStore(context).designs().isEmpty())
        var closed=false
        e.requestClose{closed=true};assertTrue(closed)
        e.set("planName","Keep this");e.requestClose{fail("Must warn")};assertTrue(e.leaving)
        e.select(0);e.back{fail("Must warn")};assertTrue(e.leaving)
        e.dispose()
    }
    @Test fun invalidDraftRetainsTheLastSavedGeometry()=runBlocking {
        val context=MemoryContext();val e=editor(context,this)
        e.set("geometry","0,32\n1020,43\n1500,90")
        assertTrue(e.save(WorkshopLanguage.ENGLISH))
        val valid=LocalStore(context).designs().single().shape
        e.set("geometry","unfinished")
        assertTrue(e.save(WorkshopLanguage.ENGLISH))
        assertEquals(valid,LocalStore(context).designs().single().shape)
        assertEquals("unfinished",ConstructionStore(context).project("same-id")["geometry"])
        e.dispose()
    }
    @Test fun namesAndCatalogPersistenceDoNotMasqueradeAsSavingTheDraft()=runBlocking {
        val e=editor(MemoryContext(),this)
        assertTrue(e.name(WorkshopLanguage.ENGLISH).startsWith("Didgeridoo "))
        e.set("target","invalid");assertEquals("Didgeridoo ",e.name(WorkshopLanguage.ENGLISH))
        e.set("planName","Real name");assertEquals("Real name",e.name(WorkshopLanguage.SPANISH))
        e.set("pipes","a,32,40,1800");e.saveCatalog();assertFalse(e.saved)
        e.set("pipes","");e.loadCatalog();assertEquals("a,32,40,1800",e.draft.value.values["pipes"])
        e.set("pipes","invalid");e.saveCatalog();assertTrue(e.failed);e.dispose()
    }
    private class MemoryContext:ContextWrapper(null) {
        private val documents=mutableMapOf<String,MutableMap<String,String>>()
        override fun getSharedPreferences(name:String,mode:Int):SharedPreferences {
            val values=documents.getOrPut(name){mutableMapOf()}
            return Proxy.newProxyInstance(SharedPreferences::class.java.classLoader,arrayOf(SharedPreferences::class.java)){_,method,args->
                when(method.name) {
                    "getString"->values[args!![0]] ?: args[1]
                    "edit"->editor(values)
                    else->error(method.name)
                }
            } as SharedPreferences
        }
        private fun editor(values:MutableMap<String,String>):SharedPreferences.Editor {
            val pending=mutableMapOf<String,String?>()
            return Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader,arrayOf(SharedPreferences.Editor::class.java)){self,method,args->
                when(method.name) {
                    "putString"->{pending[args!![0] as String]=args[1] as String?;self}
                    "commit"->{pending.forEach {(k,v)->if(v==null)values.remove(k) else values[k]=v};true}
                    else->error(method.name)
                }
            } as SharedPreferences.Editor
        }
    }
}
