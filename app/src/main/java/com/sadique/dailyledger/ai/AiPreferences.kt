package com.sadique.dailyledger.ai
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
class AiPreferences(context:Context,owner:String) {
    private val prefs=context.getSharedPreferences("ai_preferences",Context.MODE_PRIVATE);private val prefix=digest(owner)+":"
    var enabled:Boolean
        get()=prefs.getBoolean(prefix+"enabled",false)
        set(value){prefs.edit().putBoolean(prefix+"enabled",value).apply();if(!value)prefs.edit().remove(prefix+"hash").remove(prefix+"tips").remove(prefix+"attempt").apply()}
    var attemptedAt:Long
        get()=prefs.getLong(prefix+"attempt",0)
        set(value){prefs.edit().putLong(prefix+"attempt",value).apply()}
    fun cached(hash:String):List<SpendingTip>?=runCatching{if(prefs.getString(prefix+"hash","")!=hash)return null;AiProtocol.decodeInsights(prefs.getString(prefix+"tips",null)?:return null)}.getOrNull()
    fun save(hash:String,tips:List<SpendingTip>){val rows=JSONArray().apply{tips.forEach{put(JSONObject().put("title",it.title).put("detail",it.detail))}};prefs.edit().putString(prefix+"hash",hash).putString(prefix+"tips",JSONObject().put("suggestions",rows).toString()).apply()}
    companion object{fun digest(s:String)=MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString(""){"%02x".format(it)}}
}
