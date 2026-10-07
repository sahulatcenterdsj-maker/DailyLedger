import {verifyFirebaseToken,firebaseKeys} from './auth.mjs';
import {validateInput,groqRequest,validateOutput,takeQuota} from './protocol.mjs';

const reply=(body,status=200)=>Response.json(body,{status,headers:{'Cache-Control':'no-store','X-Content-Type-Options':'nosniff'}});
const BUILD='2026-10-07.1';
const PROVIDER_CODES=new Set(['invalid_api_key','model_not_found','model_decommissioned','model_permission_blocked_org','model_permission_blocked_project','json_validate_failed','invalid_request_error','rate_limit_exceeded']);
const PROVIDER_PARAMS=new Set(['model','response_format','reasoning_effort','reasoning_format','temperature','max_completion_tokens']);
const apiKey=env=>typeof env.GROQ_API_KEY==='string'?env.GROQ_API_KEY.trim():'';

// Only fixed operational codes go to logs. Never log a token, prompt, response body,
// UID, provider error message, exception text or a financial record.
function report(deps,code,extra={}) {
 const entry={event:'daily_ledger_ai_error',build:BUILD,code,...extra};
 if(deps.report)deps.report(entry);else console.warn(JSON.stringify(entry));
}
async function boundedBody(response,max) {
 const reader=response.body?.getReader();if(!reader)throw Error('empty');
 const chunks=[];let size=0;
 try {while(true){const{done,value}=await reader.read();if(done)break;size+=value.length;if(size>max)throw Error('too_large');chunks.push(value);}}
 finally {await reader.cancel().catch(()=>{});}
 const bytes=new Uint8Array(size);let at=0;for(const c of chunks){bytes.set(c,at);at+=c.length;}
 return JSON.parse(new TextDecoder().decode(bytes));
}
async function providerFailure(response,deps) {
 const status=response.status;
 const reason=status===401?'provider_key_rejected':status===403?'provider_access_denied':status===404?'provider_model_unavailable':status===429?'provider_limit':status>=500?'provider_unavailable':'provider_request_rejected';
 const detail={provider_status:status};
 try {
  const error=(await boundedBody(response,16384))?.error;
  if(PROVIDER_CODES.has(error?.code))detail.provider_code=error.code;
  if(PROVIDER_PARAMS.has(error?.param))detail.parameter=error.param;
 } catch {}
 report(deps,reason,detail);
 return reply({error:reason},status===429?429:503);
}
export async function handle(req,env,deps={}) {
 const path=new URL(req.url).pathname;
 if(req.method==='GET'&&path==='/health')return reply({
  service:'daily-ledger-ai',ready:Boolean(apiKey(env)),features:['suggestions','autofill'],
  build:BUILD,quotaConfigured:Boolean(env.AI_QUOTA)
 });
 const route={'/v1/autofill':'autofill','/v1/insights':'insights'}[path];
 if(!route)return reply({error:'not_found'},404);
 if(req.method!=='POST')return reply({error:'method_not_allowed'},405);
 if(!req.headers.get('content-type')?.startsWith('application/json'))return reply({error:'json_required'},415);
 const auth=req.headers.get('authorization')||'';
 if(!auth.startsWith('Bearer '))return reply({error:'sign_in_required'},401);
 let uid;
 try {uid=await(deps.verify||verifyFirebaseToken)(auth.slice(7),env.FIREBASE_PROJECT_ID,deps.keys||firebaseKeys);}
 catch {return reply({error:'sign_in_required'},401);}
 let body;
 try {body=validateInput(route,await boundedBody(req,12000));}
 catch {return reply({error:'invalid_input'},400);}
 const key=apiKey(env);
 if(!key){report(deps,'setup_pending');return reply({error:'setup_pending'},503);}
 const digest=await crypto.subtle.digest('SHA-256',new TextEncoder().encode(uid));
 const hash=Array.from(new Uint8Array(digest),b=>b.toString(16).padStart(2,'0')).join('');
 let quota;
 try {
  quota=await(deps.quota||(async data=>{
   const gate=env.AI_QUOTA.get(env.AI_QUOTA.idFromName('daily-quota'));
   const r=await gate.fetch('https://quota/check',{method:'POST',body:JSON.stringify(data)});
   // A storage/binding failure is not a user's exhausted quota.
   if(!r.ok&&r.status!==429)throw Error('quota_unavailable');
   return r.ok;
  }))({uid:hash,route});
 } catch {report(deps,'quota_unavailable');return reply({error:'quota_unavailable'},503);}
 if(!quota)return reply({error:'daily_limit'},429);
 let response;
 try {
  response=await(deps.fetch||fetch)('https://api.groq.com/openai/v1/chat/completions',{
   method:'POST',headers:{Authorization:'Bearer '+key,'Content-Type':'application/json'},
   body:JSON.stringify(groqRequest(route,body)),signal:AbortSignal.timeout(25000),redirect:'error'
  });
 } catch {report(deps,'provider_connection_failed');return reply({error:'provider_connection_failed'},503);}
 if(!response.ok)return providerFailure(response,deps);
 try {
  const envelope=await boundedBody(response,65536),c=envelope.choices?.[0];
  if(c?.finish_reason!=='stop'||typeof c.message?.content!=='string'||c.message.tool_calls||c.message.refusal)throw Error('incomplete');
  return reply(validateOutput(route,JSON.parse(c.message.content)));
 } catch {report(deps,'invalid_ai_response');return reply({error:'invalid_ai_response'},502);}
}
export default {
 async fetch(req,env) {
  try {return await handle(req,env);}
  catch {report({},'worker_unavailable');return reply({error:'temporarily_unavailable'},503);}
 }
};
// SQLite-backed storage holds only hashed quota counters, never financial data or prompts.
export class QuotaGate {
 constructor(state){this.state=state;}
 async fetch(req) {
  const{uid,route}=await req.json();
  if(!/^[a-f0-9]{64}$/.test(uid)||!['autofill','insights'].includes(route))return reply({},400);
  const allowed=await this.state.storage.transaction(async tx=>{
   const r=takeQuota(await tx.get('counts'),uid,route,Date.now());
   if(r.allowed)await tx.put('counts',r.data);return r.allowed;
  });
  return reply({},allowed?200:429);
 }
}
