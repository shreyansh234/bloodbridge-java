"use client";

import {useEffect,useRef,useState} from 'react';
import {Mail,Send,RefreshCw,ArrowLeft,Plus,ShieldCheck,MessageSquare} from 'lucide-react';
import {Dialog,DialogContent,DialogHeader,DialogTitle,DialogDescription} from '@/components/ui/dialog';
import {Empty} from '@/components/ui/empty';
import type {ApiClient,InboxDetail,InboxThread,Session} from '@/lib/contracts';
import {toast} from 'sonner';

const kindLabels:Record<string,string>={all:'All notifications',family:'Family support',donation:'Blood requests',profile:'Donor profiles',message:'Messages'};
const date=(value:number)=>new Date(value).toLocaleString('en-IN',{day:'numeric',month:'short',hour:'2-digit',minute:'2-digit'});
const notify=()=>window.dispatchEvent(new Event('bb-inbox-changed'));

export function InboxPage({api,session,onLogin}:{api:ApiClient;session:Session|null;onLogin:()=>void}){
 return <main className="main-wrap inbox-page"><div className="eyebrow"><span className="eyebrow-line"/>YOUR COMMUNITY CONVERSATIONS</div><h1>Your inbox<span>.</span></h1><p className="page-lead">Request updates and private messages from the BloodBridge team.</p>{!session?.user?<Empty className="empty-state"><Mail size={34}/><h2>Sign in to open your inbox</h2><p>Your conversations stay with your account.</p><button className="btn primary" onClick={onLogin}>Sign in</button></Empty>:<InternalInbox key={session.user.id} api={api}/>}</main>;
}

export function InternalInbox({api,admin=false}:{api:ApiClient;admin?:boolean}){
 const base=admin?'/api/admin/inbox':'/api/inbox';
 const apiRef=useRef(api);apiRef.current=api;
 const [threads,setThreads]=useState<InboxThread[]>([]),[kind,setKind]=useState('all'),[onlyUnread,setOnlyUnread]=useState(false),[page,setPage]=useState(1),[pages,setPages]=useState(1),[unread,setUnread]=useState(0);
 const [loading,setLoading]=useState(true),[error,setError]=useState(''),[refresh,setRefresh]=useState(0);
 const [selected,setSelected]=useState(''),[detail,setDetail]=useState<InboxDetail|null>(null),[detailLoading,setDetailLoading]=useState(false),[detailError,setDetailError]=useState('');
 const [draft,setDraft]=useState(''),[reply,setReply]=useState(''),[busy,setBusy]=useState(false),[compose,setCompose]=useState(false);
 const sequence=useRef(0),selectedRef=useRef(''),replyKey=useRef(''),composeKey=useRef('');
 useEffect(()=>{
  let active=true;let first=true;const controller=new AbortController();
  async function load(){if(first)setLoading(true);try{const data=await apiRef.current(`${base}?kind=${kind}&unread=${onlyUnread}&page=${page}`,{signal:controller.signal});if(active){setThreads(data.threads);setPages(data.pages);setUnread(data.unread);setError('');}}catch(e){if(active&&(e as Error).name!=='AbortError')setError((e as Error).message);}finally{first=false;if(active)setLoading(false);}}
  void load();const timer=setInterval(()=>{if(!document.hidden)void load();},30000);return()=>{active=false;controller.abort();clearInterval(timer);};
 },[base,kind,onlyUnread,page,refresh]);
 useEffect(()=>()=>{sequence.current++;},[]);

 async function open(id:string,messagePage=1){
  const turn=++sequence.current;const changed=selectedRef.current!==id;
  selectedRef.current=id;setSelected(id);setDetailError('');setDetailLoading(true);
  if(changed){setDetail(null);setDraft('');setReply('');replyKey.current='';}
  try{
   const data:InboxDetail=await apiRef.current(`${base}/${id}?page=${messagePage}`);
   if(turn!==sequence.current)return;
   setDetail(data);setDraft(data.thread.draftText||'');
   if(messagePage===1){await apiRef.current(`${base}/${id}/read`,{method:'POST',body:JSON.stringify({seenAt:data.thread.updatedAt})});if(turn!==sequence.current)return;setRefresh(v=>v+1);notify();}
  }catch(e){if(turn===sequence.current)setDetailError((e as Error).message);}finally{if(turn===sequence.current)setDetailLoading(false);}
 }
 async function send(){
  if(!detail||busy)return;setBusy(true);setDetailError('');
  try{
   if(admin){await apiRef.current(`${base}/${selected}/send`,{method:'POST',body:JSON.stringify({body:draft,revision:detail.thread.draftRevision})});}
   else{replyKey.current||=crypto.randomUUID();await apiRef.current(`${base}/${selected}/messages`,{method:'POST',body:JSON.stringify({body:reply,clientId:replyKey.current})});setReply('');replyKey.current='';}
   toast.success(admin?'Sent to the member’s inbox.':'Message sent to the BloodBridge team.');await open(selected);setRefresh(v=>v+1);notify();
  }catch(e){setDetailError((e as Error).message);}finally{setBusy(false);}
 }
 function closeDetail(){sequence.current++;selectedRef.current='';setSelected('');setDetail(null);setDetailError('');}

 return <section className="internal-inbox" aria-label={admin?'Administrator internal inbox':'Private inbox'}>
  <div className="panel-heading"><div><h2><Mail size={23}/>{admin?'Internal inbox':'Messages & notifications'}{unread>0&&<span className="count-pill">{unread} unread</span>}</h2><p>{admin?'Notifications arrive here with a reply draft. Review it, edit if needed, then send.':'Open a conversation to read an update or reply to the team.'}</p></div><div className="inbox-toolbar"><button className="btn secondary" onClick={()=>setRefresh(v=>v+1)} aria-label="Refresh inbox"><RefreshCw size={16}/><span>Refresh</span></button>{!admin&&<button className="btn primary" onClick={()=>{composeKey.current=crypto.randomUUID();setCompose(true);}}><Plus size={16}/>New message</button>}</div></div>
  <div className="inbox-filters"><label htmlFor={admin?'admin-inbox-kind':'member-inbox-kind'}>Filter</label><select id={admin?'admin-inbox-kind':'member-inbox-kind'} value={kind} onChange={e=>{setKind(e.target.value);setPage(1);}}>{Object.entries(kindLabels).map(([value,label])=><option key={value} value={value}>{label}</option>)}</select><label className="inbox-unread-toggle"><input type="checkbox" checked={onlyUnread} onChange={e=>{setOnlyUnread(e.target.checked);setPage(1);}}/>Unread only</label></div>
  {error&&<p className="form-error" role="alert">{error}</p>}
  <div className={'inbox-layout '+(selected?'has-conversation':'')}>
   <div className="inbox-list" aria-label="Conversations" aria-busy={loading}>
    {loading?<p className="queue-empty">Loading inbox…</p>:!threads.length?<div className="inbox-empty"><Mail size={30}/><h3>{onlyUnread?'You’re all caught up':'No messages yet'}</h3><p>{onlyUnread?'Try All notifications to view earlier conversations.':'New profile, blood and family requests will appear here.'}</p></div>:threads.map(t=><button key={t.id} className={'inbox-item '+(selected===t.id?'selected ':'')+(t.unread?'unread':'')} onClick={()=>void open(t.id)} disabled={busy} aria-pressed={selected===t.id}><span className="inbox-item-meta"><span>{kindLabels[t.kind]}</span><time>{date(t.updatedAt)}</time></span><strong>{t.unread&&<i aria-label="Unread"/>}{t.subject}</strong>{admin&&t.draftText&&<small>Reply draft ready</small>}</button>)}
    {pages>1&&<div className="inbox-pagination"><button className="text-button" disabled={page<=1||loading} onClick={()=>setPage(v=>v-1)}>Previous</button><span>{page} / {pages}</span><button className="text-button" disabled={page>=pages||loading} onClick={()=>setPage(v=>v+1)}>Next</button></div>}
   </div>
   <section className="inbox-conversation" aria-label="Open conversation" aria-busy={detailLoading}>
    {!selected?<div className="inbox-empty"><MessageSquare size={36}/><h3>A place to keep things moving</h3><p>Select a conversation to see the request details and messages.</p><small><ShieldCheck size={14}/>Private to the member and administrators</small></div>:<>
     <div className="conversation-heading"><button className="text-button" onClick={closeDetail} disabled={busy}><ArrowLeft size={16}/>Inbox</button><button className="text-button" disabled={busy||detailLoading} onClick={()=>void open(selected)}><RefreshCw size={15}/>Refresh conversation</button></div>
     {detailError&&<p className="form-error" role="alert">{detailError}</p>}
     {detailLoading&&!detail?<p className="queue-empty">Opening conversation…</p>:detail&&<><h3>{detail.thread.subject}</h3><p className="conversation-channel">{admin?'Replies are sent privately to this member inside BloodBridge.':'This conversation is between you and the BloodBridge team.'}</p>
      {detail.pages>1&&<div className="inbox-pagination"><button className="text-button" disabled={detail.page>=detail.pages||busy||detailLoading} onClick={()=>void open(selected,detail.page+1)}>Older messages</button><span>Page {detail.page} / {detail.pages}</span><button className="text-button" disabled={detail.page<=1||busy||detailLoading} onClick={()=>void open(selected,detail.page-1)}>Newer messages</button></div>}
      <div className="inbox-message-list">{detail.messages.map(m=><article className={'inbox-message '+m.role} key={m.id}><div><strong>{m.role==='system'?'BloodBridge update':m.role==='admin'?'BloodBridge team':admin?'Member':'You'}</strong><time>{date(m.createdAt)}</time></div><p>{m.body}</p></article>)}</div>
      {admin?(detail.thread.draftText?<form className="inbox-compose" onSubmit={e=>{e.preventDefault();void send();}}><label htmlFor="admin-reply-draft">Suggested reply — review before sending</label><textarea id="admin-reply-draft" value={draft} onChange={e=>setDraft(e.target.value)} maxLength={4000} minLength={5} rows={6} required disabled={busy}/><p>The member will see this message after you send it.</p><button className="btn primary" disabled={busy||detailLoading||draft.trim().length<5}><Send size={16}/>{busy?'Sending…':'Send to member'}</button></form>:<p className="success-note">Reply sent. A new draft will appear when this request gets another update.</p>):<form className="inbox-compose" onSubmit={e=>{e.preventDefault();void send();}}><label htmlFor="member-reply">Reply to the team</label><textarea id="member-reply" value={reply} onChange={e=>{setReply(e.target.value);replyKey.current='';}} minLength={5} maxLength={2000} rows={4} required disabled={busy} placeholder="Share an update or ask a question…"/><button className="btn primary" disabled={busy||detailLoading||reply.trim().length<5}><Send size={16}/>{busy?'Sending…':'Send message'}</button></form>}
     </>}
    </>}
   </section>
  </div>
  <Dialog open={compose} onOpenChange={v=>{if(!busy)setCompose(v);}}><DialogContent className="bb-dialog"><DialogHeader><DialogTitle>Message the BloodBridge team</DialogTitle><DialogDescription>Your message will appear in the administrator’s internal inbox.</DialogDescription></DialogHeader><form className="profile-form" onSubmit={async e=>{e.preventDefault();const values=Object.fromEntries(new FormData(e.currentTarget));setBusy(true);try{const data=await apiRef.current('/api/inbox',{method:'POST',body:JSON.stringify({...values,clientId:composeKey.current})});setCompose(false);setRefresh(v=>v+1);setKind('all');setOnlyUnread(false);setPage(1);await open(data.id);notify();toast.success('Message sent.');}catch(e){toast.error((e as Error).message);}finally{setBusy(false);}}}><div className="form-field"><label htmlFor="inbox-subject">Subject</label><input id="inbox-subject" name="subject" required minLength={5} maxLength={180} disabled={busy}/></div><div className="form-field"><label htmlFor="inbox-body">Message</label><textarea id="inbox-body" name="body" required minLength={5} maxLength={2000} rows={5} disabled={busy}/></div><button className="btn primary" disabled={busy}><Send size={16}/>{busy?'Sending…':'Send message'}</button></form></DialogContent></Dialog>
 </section>;
}
