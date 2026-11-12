"""Local development API. No external dependencies; loopback only by default."""
import hashlib, hmac, json, os, sqlite3, threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = Path(os.environ.get('CHAPARK_DATA_DIR', str(ROOT / '.local-server')))
DATA.mkdir(parents=True, exist_ok=True)
TOKEN_FILE = DATA / 'token.txt'
if not TOKEN_FILE.exists():
    import secrets
    TOKEN_FILE.write_text(secrets.token_urlsafe(32))
    TOKEN_FILE.chmod(0o600)
TOKEN = TOKEN_FILE.read_text().strip()
LOCK = threading.Lock()
DB = sqlite3.connect(DATA / 'records.sqlite3', check_same_thread=False)
DB.execute('CREATE TABLE IF NOT EXISTS records (id TEXT PRIMARY KEY, payload TEXT, digest TEXT, deleted INTEGER NOT NULL DEFAULT 0)')
DB.commit()

class Handler(BaseHTTPRequestHandler):
    timeout = 10
    def log_message(self, *_): pass
    def reply(self, code, body):
        raw = json.dumps(body, ensure_ascii=False).encode()
        self.send_response(code); self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(raw))); self.end_headers(); self.wfile.write(raw)
    def authorized(self):
        if not hmac.compare_digest(self.headers.get('Authorization', ''), 'Bearer ' + TOKEN):
            self.reply(401, {'error':'unauthorized'}); return False
        return True
    def do_GET(self):
        if not self.authorized(): return
        if self.path == '/v1/records':
            with LOCK: rows = DB.execute('SELECT id,payload FROM records WHERE deleted=0').fetchall()
            self.reply(200, {'schemaVersion':1,'records':[{'id':i,'data':json.loads(p)} for i,p in rows]})
        elif self.path == '/health': self.reply(200, {'status':'ok','schemaVersion':1})
        else: self.reply(404, {'error':'not_found'})
    def do_PUT(self):
        if not self.authorized(): return
        if not self.path.startswith('/v1/records/'): return self.reply(404, {'error':'not_found'})
        key=self.path.removeprefix('/v1/records/')
        if not key or len(key)>100 or not all(c.isalnum() or c in '-_' for c in key): return self.reply(400, {'error':'invalid_id'})
        try:
            length=int(self.headers.get('Content-Length','0'))
            if not 0<length<=1_000_000: return self.reply(413, {'error':'invalid_size'})
            body=json.loads(self.rfile.read(length))
            if not isinstance(body,dict) or not isinstance(body.get('records'),list) or not isinstance(body.get('placeId'),str):
                return self.reply(400, {'error':'invalid_record'})
            payload=json.dumps(body,sort_keys=True,separators=(',',':'),ensure_ascii=False)
        except (ValueError,UnicodeError): return self.reply(400, {'error':'invalid_json'})
        digest=hashlib.sha256(payload.encode()).hexdigest()
        with LOCK:
            old=DB.execute('SELECT digest,deleted FROM records WHERE id=?',(key,)).fetchone()
            if old and old[1]: return self.reply(410, {'error':'deleted'})
            if old and old[0]!=digest: return self.reply(409, {'error':'record_conflict'})
            DB.execute('INSERT OR IGNORE INTO records(id,payload,digest) VALUES(?,?,?)',(key,payload,digest)); DB.commit()
        self.reply(200, {'id':key,'status':'stored','digest':digest})
    def do_DELETE(self):
        if not self.authorized(): return
        if not self.path.startswith('/v1/records/'): return self.reply(404, {'error':'not_found'})
        key=self.path.removeprefix('/v1/records/')
        if not key or len(key)>100 or not all(c.isalnum() or c in '-_' for c in key): return self.reply(400, {'error':'invalid_id'})
        with LOCK:
            DB.execute('INSERT INTO records(id,deleted) VALUES(?,1) ON CONFLICT(id) DO UPDATE SET payload=NULL,digest=NULL,deleted=1',(key,)); DB.commit()
        self.reply(200, {'id':key,'status':'deleted'})

if __name__=='__main__':
    print('ChaPark local API http://127.0.0.1:8091 (token stored in .local-server/token.txt)',flush=True)
    ThreadingHTTPServer(('127.0.0.1',8091),Handler).serve_forever()
