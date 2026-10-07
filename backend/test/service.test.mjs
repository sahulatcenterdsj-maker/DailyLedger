import test from'node:test';import assert from'node:assert/strict';
import{verifyFirebaseToken}from'../src/auth.mjs';import{validateInput,validateOutput,takeQuota}from'../src/protocol.mjs';import{handle,QuotaGate}from'../src/worker.mjs';
const now=Date.now(),pair=await crypto.subtle.generateKey({name:'RSASSA-PKCS1-v1_5',modulusLength:2048,publicExponent:new Uint8Array([1,0,1]),hash:'SHA-256'},true,['sign','verify']);
const jwk={...await crypto.subtle.exportKey('jwk',pair.publicKey),kid:'test'};
const claims={aud:'daily-ledger-4d8ef',iss:'https://securetoken.google.com/daily-ledger-4d8ef',sub:'owner',exp:Math.floor(now/1000)+3600,iat:Math.floor(now/1000),auth_time:Math.floor(now/1000),firebase:{sign_in_provider:'google.com'}};
async function token(changes={},head={}){const b=x=>Buffer.from(JSON.stringify(x)).toString('base64url');const data=b({alg:'RS256',kid:'test',...head})+'.'+b({...claims,...changes});const sig=await crypto.subtle.sign('RSASSA-PKCS1-v1_5',pair.privateKey,new TextEncoder().encode(data));return data+'.'+Buffer.from(sig).toString('base64url');}
const env={FIREBASE_PROJECT_ID:'daily-ledger-4d8ef',GROQ_API_KEY:'test-placeholder'},keys=async()=>[jwk];
const entry={type:'EXPENSE',amount_pkr:'1500.25',category:'Fuel',note:'Petrol',date:'2026-10-06'},out={message:'Check karein',transactions:[entry]};
const request=(body={text:'Petrol 1500.25',today:'2026-10-06'},auth='Bearer fixture',path='/v1/autofill')=>new Request('https://ledger.example'+path,{method:'POST',headers:{'content-type':'application/json',authorization:auth},body:JSON.stringify(body)});
const deps={report:()=>{},verify:async()=> 'owner',quota:async()=>true,fetch:async()=>Response.json({choices:[{finish_reason:'stop',message:{content:JSON.stringify(out)}}]})};
test('Firebase signature verifies account',async()=>assert.equal(await verifyFirebaseToken(await token(),env.FIREBASE_PROJECT_ID,keys,now),'owner'));
test('wrong claims, expired and anonymous users rejected',async()=>{for(const c of[{aud:'wrong'},{iss:'wrong'},{exp:0},{iat:9e10},{auth_time:9e10},{sub:''},{sub:'x'.repeat(129)},{firebase:{sign_in_provider:'anonymous'}}])await assert.rejects(verifyFirebaseToken(await token(c),env.FIREBASE_PROJECT_ID,keys,now));});
test('bad algorithm and tampered signature rejected',async()=>{await assert.rejects(verifyFirebaseToken(await token({},{alg:'none'}),env.FIREBASE_PROJECT_ID,keys,now));const p=(await token()).split('.');const b=Buffer.from(p[2],'base64url');b[0]^=1;p[2]=b.toString('base64url');await assert.rejects(verifyFirebaseToken(p.join('.'),env.FIREBASE_PROJECT_ID,keys,now));});
test('no chat route',async()=>assert.equal((await handle(request({},'Bearer fixture','/v1/chat'),env,deps)).status,404));
test('unauthenticated caller cannot consume quota',async()=>assert.equal((await handle(request(undefined,''),env,{...deps,quota:()=>assert.fail('quota used')})).status,401));
test('strict output and key non-disclosure',async()=>{let payload;const r=await handle(request(),env,{...deps,fetch:async(url,init)=>{assert.equal(url,'https://api.groq.com/openai/v1/chat/completions');payload=JSON.parse(init.body);return deps.fetch();}});const b=await r.json();assert.deepEqual(b,out);assert.equal(payload.response_format.json_schema.strict,true);assert.ok(!JSON.stringify(b).includes(env.GROQ_API_KEY));});
test('no client model, prompt, owner or action overrides',async()=>{for(const extra of[{model:'other'},{messages:[]},{ownerId:'foreign'},{action:'delete'}])assert.equal((await handle(request({text:'fuel',today:'2026-10-06',...extra}),env,{...deps,fetch:()=>assert.fail('called')})).status,400);});
test('invalid amounts and dates rejected',()=>{assert.throws(()=>validateInput('autofill',{text:'x'.repeat(1501),today:'2026-10-06'}));for(const change of[{amount_pkr:'-1'},{amount_pkr:'0'},{amount_pkr:'1.001'},{type:'DELETE'},{ownerId:'other'},{date:'2026-02-30'}])assert.throws(()=>validateOutput('autofill',{...out,transactions:[{...entry,...change}]}));});
test('limits, missing setup and truncation return errors',async()=>{assert.equal((await handle(request(),env,{...deps,quota:async()=>false})).status,429);assert.equal((await handle(request(),{...env,GROQ_API_KEY:''},deps)).status,503);assert.equal((await handle(request(),env,{...deps,fetch:async()=>Response.json({choices:[{finish_reason:'length',message:{content:JSON.stringify(out)}}]})})).status,502);});
test('user quota isolation and next-day reset',()=>{let s;for(let i=0;i<2;i++){const q=takeQuota(s,'a','insights',now+i*60000);assert.ok(q.allowed);s=q.data;}assert.equal(takeQuota(s,'a','insights',now+120000).allowed,false);assert.ok(takeQuota(s,'b','insights',now+120000).allowed);assert.ok(takeQuota(s,'a','insights',now+86400000).allowed);});
test('global day and minute caps',()=>{let s;const t=Date.parse('2026-10-06T01:00:00Z');for(let i=0;i<100;i++){const q=takeQuota(s,'u'+i,'autofill',t+i*60000);assert.ok(q.allowed);s=q.data;}assert.equal(takeQuota(s,'new','autofill',t+100*60000).allowed,false);s=undefined;for(let i=0;i<8;i++)s=takeQuota(s,'u'+i,'autofill',now).data;assert.equal(takeQuota(s,'ninth','autofill',now).allowed,false);});
test('summary arithmetic and no notes accepted',()=>{const b={month:'2026-10',through:'2026-10-06',salary:'60000',expenses:'4000',remaining:'56000',other_income:'0',comparison_days:6,previous_comparable:'3000',current_comparable:'4000',record_count:4,categories:[{name:'Fuel',amount:'4000'}]};assert.equal(validateInput('insights',b),b);assert.throws(()=>validateInput('insights',{...b,remaining:'999'}));assert.throws(()=>validateInput('insights',{...b,notes:['private']}));});
test('durable quota check is transactional',async()=>{let data,count=0;const gate=new QuotaGate({storage:{transaction:async f=>{count++;return f({get:async()=>data,put:async(_,v)=>{data=v;}});}}});const r=()=>new Request('https://quota/check',{method:'POST',body:JSON.stringify({uid:'a'.repeat(64),route:'insights'})});assert.equal((await gate.fetch(r())).status,200);assert.equal((await gate.fetch(r())).status,200);assert.equal((await gate.fetch(r())).status,429);assert.equal(count,3);});

test('health identifies deployed build and binding without revealing secrets',async()=>{
 const r=await handle(new Request('https://ledger.example/health'),env);
 const b=await r.json();assert.equal(b.build,'2026-10-07.1');assert.equal(b.ready,true);assert.equal(b.quotaConfigured,false);
 assert.ok(!JSON.stringify(b).includes(env.GROQ_API_KEY));
 const empty=await handle(new Request('https://ledger.example/health'),{...env,GROQ_API_KEY:'  \n '});
 assert.equal((await empty.json()).ready,false);
});
test('surrounding key whitespace is trimmed only in upstream authorization',async()=>{
 let header;const e={...env,GROQ_API_KEY:' \t'+env.GROQ_API_KEY+'\n'};
 const r=await handle(request(),e,{...deps,fetch:async(_,init)=>{header=init.headers.Authorization;return deps.fetch();}});
 assert.equal(r.status,200);assert.equal(header,'Bearer '+env.GROQ_API_KEY);
});
test('provider failures have bounded safe diagnostics and never log raw bodies',async()=>{
 for(const [status,code] of [[400,'provider_request_rejected'],[401,'provider_key_rejected'],[403,'provider_access_denied'],[404,'provider_model_unavailable'],[429,'provider_limit'],[500,'provider_unavailable']]){
  const events=[];const privateText='PRIVATE '+env.GROQ_API_KEY+' bearer fixture user owner milk 150';
  const r=await handle(request(),env,{...deps,report:e=>events.push(e),fetch:async()=>Response.json({error:{message:privateText,code:privateText,param:privateText,failed_generation:privateText}},{status})});
  assert.equal(r.status,status===429?429:503);assert.deepEqual(await r.json(),{error:code});
  assert.deepEqual(events,[{event:'daily_ledger_ai_error',build:'2026-10-07.1',code,provider_status:status}]);
 }
});
test('allowlisted provider codes and parameters aid diagnosis',async()=>{
 const events=[];
 await handle(request(),env,{...deps,report:e=>events.push(e),fetch:async()=>Response.json({error:{code:'invalid_request_error',param:'response_format',message:'PRIVATE'}},{status:400})});
 assert.equal(events[0].provider_code,'invalid_request_error');assert.equal(events[0].parameter,'response_format');assert.ok(!JSON.stringify(events).includes('PRIVATE'));
});
test('provider connection exceptions do not expose exception text',async()=>{
 const events=[];
 const r=await handle(request(),env,{...deps,report:e=>events.push(e),fetch:async()=>{throw Error('PRIVATE '+env.GROQ_API_KEY);}});
 assert.equal(r.status,503);assert.deepEqual(await r.json(),{error:'provider_connection_failed'});assert.equal(events[0].code,'provider_connection_failed');assert.ok(!JSON.stringify(events).includes(env.GROQ_API_KEY));
});
test('missing or broken quota bindings stop before provider use',async()=>{
 for(const binding of [undefined,{idFromName:()=> 'id',get:()=>({fetch:async()=>new Response('',{status:500})})}]){
  const events=[];
  const r=await handle(request(),{...env,AI_QUOTA:binding},{...deps,quota:undefined,report:e=>events.push(e),fetch:()=>assert.fail('provider called')});
  assert.equal(r.status,503);assert.deepEqual(await r.json(),{error:'quota_unavailable'});assert.equal(events[0].code,'quota_unavailable');
 }
});
test('provider malformed and oversized errors produce no body disclosure',async()=>{
 for(const payload of ['<html>PRIVATE</html>','PRIVATE'.repeat(4000)]){
  const events=[];
  const r=await handle(request(),env,{...deps,report:e=>events.push(e),fetch:async()=>new Response(payload,{status:400})});
  assert.equal(r.status,503);assert.equal(events[0].code,'provider_request_rejected');assert.ok(!JSON.stringify(events).includes('PRIVATE'));
 }
});
