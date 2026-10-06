package com.sadique.dailyledger.ai
import com.sadique.dailyledger.data.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

data class SpendingTip(val title:String,val detail:String)
data class SpendingSnapshot(val month:String,val through:String,val summary:SalarySummary,val count:Int,val categories:List<Pair<String,Long>>,val comparisonDays:Int,val previousComparable:Long,val currentComparable:Long,val tips:List<SpendingTip>)
object SpendingInsights {
    private fun rs(v:Long)="Rs "+BigDecimal.valueOf(v,2).stripTrailingZeros().toPlainString()
    fun calculate(owner:String,transactions:List<TransactionEntity>,today:LocalDate=LocalDate.now()):SpendingSnapshot {
        val month=YearMonth.from(today);val previous=month.minusMonths(1);val days=minOf(today.dayOfMonth,previous.lengthOfMonth())
        val own=transactions.filter{it.ownerId==owner && runCatching{LocalDate.parse(it.date)<=today}.getOrDefault(false)}
        val current=own.filter{it.date.startsWith("$month-")};val summary=monthlySalarySummary(current,month.toString())
        val categories=current.filter{it.type=="EXPENSE"}.groupBy{it.category.trim().ifBlank{"Other"}}.map{(name,rows)->name to rows.sumOf{it.amountMinor}}.sortedByDescending{it.second}
        fun comparable(m:YearMonth)=own.filter{it.type=="EXPENSE"&&it.date.startsWith("$m-")&&LocalDate.parse(it.date).dayOfMonth<=days}.sumOf{it.amountMinor}
        val now=comparable(month);val old=comparable(previous);val tips=mutableListOf<SpendingTip>()
        if(categories.isEmpty()) tips+=SpendingTip("Start your monthly picture","Kharchay record karein; unki bunyad par yahan bachat ke mashwaray milenge.")
        else {
            val biggest=categories.first();tips+=SpendingTip("Sab se zyada kharcha","${biggest.first}: ${rs(biggest.second)}. Is category ki entries dekh kar apna budget tay karein.")
            if(old>0 && now>old) tips+=SpendingTip("Kharchay mein izafa","Dono mahino ke pehle $days din: ${rs(now-old)} (${String.format(Locale.US,"%.0f",(now-old).toDouble()/old*100)}%) zyada kharcha. Comparison sirf recorded entries par hai.")
            else if(old>0 && now<old) tips+=SpendingTip("Kharcha kam hua","Pichle mahine ke pehle $days dinon ke muqablay mein ${rs(old-now)} kam kharcha record hua.")
            else if(old==0L) tips+=SpendingTip("Comparison ke liye data chahiye","Pichle mahine ke pehle $days dinon ki expense entries nahi hain; abhi izafay ka faisla nahi kar sakte.")
            val flexible=setOf("restaurant","takeaway","tea & coffee","snacks","subscriptions","cinema & outings","games & hobbies")
            val possible=categories.filter{it.first.lowercase(Locale.ROOT) in flexible}.sumOf{it.second}/5
            if(possible>0) tips+=SpendingTip("Bachat ka ek option","Bahar ke khane, snacks ya entertainment ka kharcha 20% kam ho to ${rs(possible)} bach sakte hain. Apni zarooriyat ke mutabiq budget rakhein.")
            if(summary.salary>0 && summary.remaining<0) tips+=SpendingTip("Salary se zyada kharcha","Expenses salary se ${rs(-summary.remaining)} zyada hain. Pehle zaroori bills alag karein, phir optional kharchay ka budget tay karein.")
            else if(summary.salary>0) tips+=SpendingTip("Salary ka baqi hisaab","${rs(summary.remaining)} salary baqi hai. Pending bills dekh kar saving tay karein; yeh guaranteed saving nahi hai.")
        }
        return SpendingSnapshot(month.toString(),today.toString(),summary,current.size,categories,days,old,now,tips.take(4))
    }
}
