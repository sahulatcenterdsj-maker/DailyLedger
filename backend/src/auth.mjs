const enc=new TextEncoder();
function decode(s){if(!/^[A-Za-z0-9_-]+$/.test(s))throw Error('unauthorized');return Uint8Array.from(atob(s.replaceAll('-','+').replaceAll('_','/')),c=>c.charCodeAt(0));}
export async function verifyFirebaseToken(token,project,getKeys,now=Date.now()){
 if(typeof token!=='string'||token.length>8192)throw Error('unauthorized');const p=token.split('.');if(p.length!==3)throw Error('unauthorized');
 const h=JSON.parse(new TextDecoder().decode(decode(p[0]))),c=JSON.parse(new TextDecoder().decode(decode(p[1]))),sec=Math.floor(now/1000);
 if(h.alg!=='RS256'||typeof h.kid!=='string'||h.kid.length>100||c.aud!==project||c.iss!==`https://securetoken.google.com/${project}`||typeof c.sub!=='string'||!c.sub||c.sub.length>128||!Number.isFinite(c.exp)||c.exp<=sec||!Number.isFinite(c.iat)||c.iat>sec+60||!Number.isFinite(c.auth_time)||c.auth_time>sec+60||(c.nbf!==undefined&&(!Number.isFinite(c.nbf)||c.nbf>sec+60))||!c.firebase||typeof c.firebase.sign_in_provider!=='string'||c.firebase.sign_in_provider==='anonymous')throw Error('unauthorized');
 const jwk=(await getKeys()).find(k=>k.kid===h.kid&&k.kty==='RSA');if(!jwk)throw Error('unauthorized');
 const key=await crypto.subtle.importKey('jwk',jwk,{name:'RSASSA-PKCS1-v1_5',hash:'SHA-256'},false,['verify']);
 if(!await crypto.subtle.verify('RSASSA-PKCS1-v1_5',key,decode(p[2]),enc.encode(`${p[0]}.${p[1]}`)))throw Error('unauthorized');return c.sub;
}
let cache;
export async function firebaseKeys(){if(cache&&Date.now()<cache.expires)return cache.keys;const r=await fetch('https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com',{signal:AbortSignal.timeout(8000)});if(!r.ok)throw Error('auth_unavailable');const{keys}=await r.json();if(!Array.isArray(keys)||!keys.length)throw Error('auth_unavailable');const age=Number(r.headers.get('cache-control')?.match(/max-age=(\d+)/)?.[1]||300);cache={keys,expires:Date.now()+Math.min(Math.max(age,60),3600)*1000};return keys;}
