package com.sadique.dailyledger.ai
import android.content.Context
import com.sadique.dailyledger.auth.FirebaseRuntime
import com.sadique.dailyledger.auth.awaitResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URL
import java.time.LocalDate
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.cancellation.CancellationException

class AiService(private val context:Context,private val owner:String) {
    private var endpoint:String?=null;private var checkedAt=0L
    private suspend fun request(url:String,body:String?=null,token:String?=null):String=withContext(Dispatchers.IO){
        val c=URL(url).openConnection() as HttpsURLConnection
        try {
            ensureActive();c.instanceFollowRedirects=false;c.connectTimeout=12000;c.readTimeout=30000;c.setRequestProperty("Accept","application/json")
            if(body!=null){c.requestMethod="POST";c.doOutput=true;c.setRequestProperty("Content-Type","application/json; charset=utf-8");c.setRequestProperty("Authorization","Bearer $token");val bytes=body.toByteArray(Charsets.UTF_8);c.setFixedLengthStreamingMode(bytes.size);c.outputStream.use{it.write(bytes)}}
            val status=c.responseCode
            if(status !in 200..299) throw AiException(when(status){401,403->"Please sign in again to use AI.";429->"AI usage limit reached. Local insights and manual entries still work.";503->"Cloud AI setup is pending or temporarily unavailable. Local insights still work.";else->"AI is temporarily unavailable. Try again later."})
            val result=c.inputStream.use{input->val out=ByteArrayOutputStream();val buf=ByteArray(4096);while(true){ensureActive();val n=input.read(buf);if(n<0)break;if(out.size()+n>65536)throw AiException("AI response was too large.");out.write(buf,0,n)};out.toString("UTF-8")}
            ensureActive();result
        }finally{c.disconnect()}
    }
    private suspend fun serviceUrl():String {
        if(System.currentTimeMillis()-checkedAt>15*60000L){val config=JSONObject(request(CONFIG_URL));val candidate=config.optString("endpoint").trimEnd('/');require(candidate.isEmpty()||validEndpoint(candidate));endpoint=candidate.takeIf{it.isNotEmpty()};checkedAt=System.currentTimeMillis()}
        return endpoint?:throw AiException("Cloud AI is being set up. Local insights and manual entries are available.")
    }
    private suspend fun call(path:String,body:JSONObject):String=try{
        val user=FirebaseRuntime.auth(context).currentUser;check(user?.uid==owner)
        val url=serviceUrl();val token=user!!.getIdToken(false).awaitResult().token?:throw AiException("Please sign in again.")
        check(FirebaseRuntime.auth(context).currentUser?.uid==owner);request("$url/v1/$path",body.toString(),token)
    }catch(e:CancellationException){throw e}catch(e:AiException){throw e}catch(_:Exception){throw AiException("Could not reach AI. Check your internet connection and try again.")}
    suspend fun drafts(input:String):AiDraftResult{require(input.isNotBlank()&&input.length<=AiProtocol.MAX_INPUT);return AiProtocol.decodeDrafts(call("autofill",JSONObject().put("text",input.trim()).put("today",LocalDate.now().toString())))}
    suspend fun insights(snapshot:SpendingSnapshot)=AiProtocol.decodeInsights(call("insights",AiProtocol.context(snapshot)))
    companion object {
        const val CONFIG_URL="https://raw.githubusercontent.com/sahulatcenterdsj-maker/DailyLedger/codex/build-apk-20261005/ai-service.json"
        fun validEndpoint(value:String):Boolean=runCatching{val u=URL(value);u.protocol=="https"&&u.host.endsWith(".workers.dev")&&u.userInfo==null&&u.port == -1&&u.path in listOf("","/")&&u.query==null&&u.ref==null}.getOrDefault(false)
    }
}
