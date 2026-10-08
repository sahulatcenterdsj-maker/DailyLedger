package com.sadique.dailyledger.ai
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sadique.dailyledger.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
@RunWith(AndroidJUnit4::class) class AiDataTest {
 private val context:Context=ApplicationProvider.getApplicationContext()
 private val valid="""{"message":"Check","transactions":[{"type":"EXPENSE","amount_pkr":"1500.25","category":"Fuel","note":"Petrol","date":"2026-10-06"}]}"""
 @Test fun parseExactMoneyRejectMalformed(){assertEquals(150025L,AiProtocol.decodeDrafts(valid).entries.single().amountMinor);for(bad in listOf(valid.replace("1500.25","-1"),valid.replace("1500.25","1.001"),valid.replace("2026-10-06","2026-02-30"),valid.replace("EXPENSE","DELETE"),valid.replace("\"type\":","\"ownerId\":\"other\",\"type\":"),"not json"))assertTrue(runCatching{AiProtocol.decodeDrafts(bad)}.exceptionOrNull() is AiException)}
 @Test fun atomicIdempotentAccountScopedBatch()=runBlocking {
  val db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
  try{val repo=LedgerRepository(db,"owner");LedgerRepository(db,"other").saveTransaction("EXPENSE",777,"Other","","2026-10-06");val first=AiProtocol.decodeDrafts(valid).entries.single();val batch="c98951b9-5f90-48a4-b1a3-433b80a47c1c";val drafts=listOf(first,first.copy(category="Milk",amountMinor=30000));repo.saveDraftBatch(drafts,batch);repo.saveDraftBatch(drafts,batch);assertEquals(2,db.ledgerDao().transactionsNow("owner").size);assertEquals(777L,db.ledgerDao().transactionsNow("other").single().amountMinor);assertTrue(runCatching{repo.saveDraftBatch(listOf(first.copy(amountMinor=999)),batch)}.isFailure)
   db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_bad_draft BEFORE INSERT ON transactions WHEN NEW.category='FAIL' BEGIN SELECT RAISE(ABORT, 'fixture failure'); END")
   assertTrue(runCatching{repo.saveDraftBatch(listOf(first,first.copy(category="FAIL")),"ae6cbb44-abba-45cd-9c54-c4c0e1eb38d2")}.isFailure);assertEquals(2,db.ledgerDao().transactionsNow("owner").size)
  }finally{db.close()}
 }
 @Test fun privateSummaryAndConsentIsolation(){val row=TransactionEntity("secret-id","owner","EXPENSE",150000,"Fuel","private-note","2026-10-06",1,1);val json=AiProtocol.context(SpendingInsights.calculate("owner",listOf(row),LocalDate.parse("2026-10-06"))).toString();assertFalse(json.contains("secret-id"));assertFalse(json.contains("owner"));assertFalse(json.contains("private-note"));val a=AiPreferences(context,"test-a");val b=AiPreferences(context,"test-b");a.enabled=false;b.enabled=false;a.enabled=true;assertFalse(b.enabled);a.save("hash",listOf(SpendingTip("title","detail")));assertNotNull(a.cached("hash"));assertNull(b.cached("hash"));a.enabled=false;assertNull(a.cached("hash"))}
 @Test fun catalogAndGeminiSummaryPrivacy(){
  val c=CategoryCatalog.load(context);assertEquals(105,c.size);assertEquals(105,c.map{it.label}.distinct().size);assertTrue(c.any{it.label=="Salary"&&it.type=="INCOME"})
  val rows=listOf(TransactionEntity("private-id","me","EXPENSE",10000,"Private person's bill","private note","2026-10-06",1,1),TransactionEntity("id2","me","EXPENSE",20000,"Another private name","","2026-10-06",1,1))
  val input=AiPrompts.insights(SpendingInsights.calculate("me",rows,LocalDate.parse("2026-10-06")),c).input
  assertFalse(input.contains("private",ignoreCase=true));assertFalse(input.contains("Another"));assertTrue(input.contains("Other expenses"));assertTrue(input.contains("300.00"))
 }
 @Test fun oldProviderConsentDoesNotEnableGeminiAndLimitsAreAccountScoped(){
  val owner="gemini-migration-test";val raw=context.getSharedPreferences("ai_preferences",Context.MODE_PRIVATE)
  val old=AiPreferences.digest(owner)+":";val fresh="gemini-v1:"+old
  raw.edit().putBoolean(old+"enabled",true).remove(fresh+"enabled").remove(fresh+"quota_day").commit()
  val prefs=AiPreferences(context,owner);assertFalse(prefs.enabled);prefs.enabled=true;assertTrue(prefs.enabled)
  val now=java.time.Instant.parse("2026-10-07T01:00:00Z").toEpochMilli()
  repeat(10){assertTrue(prefs.reserve(AiTask.AUTOFILL,now))};assertFalse(prefs.reserve(AiTask.AUTOFILL,now))
  repeat(2){assertTrue(prefs.reserve(AiTask.INSIGHTS,now))};assertFalse(prefs.reserve(AiTask.INSIGHTS,now))
  val other=AiPreferences(context,"another-"+java.util.UUID.randomUUID());assertTrue(other.reserve(AiTask.AUTOFILL,now))
  assertTrue(prefs.reserve(AiTask.AUTOFILL,now+86400000));prefs.enabled=false
 }
}
