#!/usr/bin/env python3
"""Encrypted PostgreSQL/assets backups, authenticated manifests and isolated restore checks.
Connections use standard PG* environment variables. Never embeds database credentials.
"""
import argparse,base64,datetime,hashlib,hmac,json,os,pathlib,shutil,subprocess,tarfile,tempfile,time,uuid
UTC=datetime.timezone.utc
TABLES=('users','courses','lectures','payments','refunds','instructor_earnings','wallets','wallet_transactions','webhook_events','financial_ledger')
def key():
    value=os.environ.get('OLP_BACKUP_KEY','')
    try: raw=base64.b64decode(value,validate=True)
    except Exception: raise ValueError('OLP_BACKUP_KEY must be a base64-encoded 32-byte secret')
    if len(raw)!=32:raise ValueError('OLP_BACKUP_KEY must contain exactly 32 random bytes')
    return hmac.new(raw,b'olp-backup-manifest-v1',hashlib.sha256).digest()
def sign(data):return hmac.new(key(),json.dumps(data,sort_keys=True,separators=(',',':')).encode(),hashlib.sha256).hexdigest()
def digest(path):
    value=hashlib.sha256()
    with open(path,'rb') as source:
        for chunk in iter(lambda:source.read(1024*1024),b''):value.update(chunk)
    return value.hexdigest()
def run(command,**kwargs):return subprocess.run(command,check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE,**kwargs)
def counts(database):
    query=';'.join('SELECT count(*) FROM '+table for table in TABLES)
    data=run(['psql','-X','-At','-v','ON_ERROR_STOP=1','-d',database,'-c',query]).stdout.decode().splitlines()
    return dict(zip(TABLES,map(int,data),strict=True))
def backup(args):
    key();database=os.environ.get('PGDATABASE')
    if not database:raise ValueError('PGDATABASE is required')
    if args.retention_days<2557:raise ValueError('Financial backup retention must be at least 2557 days')
    destination=pathlib.Path(args.directory).resolve();destination.mkdir(parents=True,exist_ok=True,mode=0o700)
    now=datetime.datetime.now(UTC);name='finance-'+now.strftime('%Y%m%dT%H%M%SZ')+'-'+uuid.uuid4().hex
    target=destination/(name+'.enc')
    with tempfile.TemporaryDirectory() as scratch:
        root=pathlib.Path(scratch);dump=root/'database.dump'
        # pg_dump takes a consistent database snapshot. Counts are embedded from that dump's restored contents during verification.
        run(['pg_dump','--format=custom','--no-owner','--no-acl','--file',str(dump),database])
        with tarfile.open(root/'backup.tar.gz','w:gz') as archive:
            archive.add(dump,arcname='database.dump')
            if args.assets:
                assets=pathlib.Path(args.assets).resolve()
                if not assets.is_dir():raise ValueError('Assets directory does not exist')
                for path in sorted(assets.rglob('*')):
                    if path.is_symlink():raise ValueError('Asset backups reject symbolic links')
                    if path.is_file():archive.add(path,arcname='assets/'+path.relative_to(assets).as_posix(),recursive=False)
        run(['openssl','enc','-aes-256-cbc','-pbkdf2','-iter','200000','-salt','-in',str(root/'backup.tar.gz'),'-out',str(target),'-pass','env:OLP_BACKUP_KEY'])
    os.chmod(target,0o600)
    manifest={'format':'olp-finance-backup-v1','createdAt':now.isoformat(),'retainUntil':(now+datetime.timedelta(days=args.retention_days)).isoformat(),'database':database,'archive':target.name,'sha256':digest(target),'assetsIncluded':bool(args.assets)}
    manifest['authentication']=sign(manifest)
    meta=target.with_suffix('.json');meta.write_text(json.dumps(manifest,indent=2));os.chmod(meta,0o600)
    print(json.dumps({'archive':str(target),'manifest':str(meta),'encrypted':True,'retainUntil':manifest['retainUntil'],'assetsIncluded':bool(args.assets)}))
def authenticated(archive):
    path=pathlib.Path(archive).resolve();data=json.loads(path.with_suffix('.json').read_text());signature=data.pop('authentication','')
    if not hmac.compare_digest(signature,sign(data)) or data.get('archive')!=path.name or not hmac.compare_digest(data.get('sha256',''),digest(path)):raise ValueError('Backup authentication failed; refusing to decrypt or prune')
    return path,data

def verify(args):
    path,data=authenticated(args.archive)
    host=os.environ.get('PGHOST','')
    if host not in ('127.0.0.1','localhost','::1'):raise ValueError('Restore checks are restricted to an explicitly configured local PostgreSQL server')
    database=args.restore_database
    if not database.endswith('_restore_check') or not all(c.isalnum() or c=='_' for c in database):raise ValueError('Use a new local database ending in _restore_check')
    start=time.monotonic()
    with tempfile.TemporaryDirectory() as scratch:
        root=pathlib.Path(scratch)
        run(['openssl','enc','-d','-aes-256-cbc','-pbkdf2','-iter','200000','-in',str(path),'-out',str(root/'backup.tar.gz'),'-pass','env:OLP_BACKUP_KEY'])
        with tarfile.open(root/'backup.tar.gz','r:gz') as archive:
            member=archive.getmember('database.dump')
            if not member.isfile():raise ValueError('Invalid database dump')
            with archive.extractfile(member) as source,open(root/'database.dump','wb') as dest:shutil.copyfileobj(source,dest)
            if data['assetsIncluded']:
                if not args.restore_assets:raise ValueError('Provide a new empty --restore-assets directory to verify asset restoration')
                assets=pathlib.Path(args.restore_assets).resolve()
                if assets.exists():raise ValueError('Restore assets directory must not already exist')
                assets.mkdir(parents=True,mode=0o700)
                for item in archive.getmembers():
                    if not item.name.startswith('assets/'):continue
                    relative=pathlib.PurePosixPath(item.name).relative_to('assets')
                    if '..' in relative.parts or not item.isfile():raise ValueError('Invalid asset archive member')
                    dest=assets.joinpath(*relative.parts);dest.parent.mkdir(parents=True,exist_ok=True)
                    with archive.extractfile(item) as source,open(dest,'wb') as target:shutil.copyfileobj(source,target)
        # createdb fails if a target already exists; no overwrite/drop operation is performed.
        run(['createdb',database]);run(['pg_restore','--exit-on-error','--no-owner','--no-acl','--dbname',database,str(root/'database.dump')])
    result={'restoredDatabase':database,'sourceArchive':path.name,'seconds':round(time.monotonic()-start,3),'rowCounts':counts(database),'verifiedAt':datetime.datetime.now(UTC).isoformat(),'assetsRestored':data['assetsIncluded']}
    pathlib.Path(args.report).write_text(json.dumps(result,indent=2));print(json.dumps(result))
def prune(args):
    now=datetime.datetime.now(UTC);removed=[]
    for file in pathlib.Path(args.directory).glob('finance-*.enc'):
        path,data=authenticated(file)
        created=datetime.datetime.fromisoformat(data['createdAt']);until=datetime.datetime.fromisoformat(data['retainUntil'])
        if until-created<datetime.timedelta(days=2557):raise ValueError('Invalid financial retention interval')
        if now>=until:path.unlink();path.with_suffix('.json').unlink();removed.append(path.name)
    print(json.dumps({'removed':removed,'retainedBeforeExpiry':True}))
def main():
    parser=argparse.ArgumentParser();sub=parser.add_subparsers(dest='operation',required=True)
    p=sub.add_parser('backup');p.add_argument('--directory',required=True);p.add_argument('--assets');p.add_argument('--retention-days',type=int,default=2557);p.set_defaults(func=backup)
    p=sub.add_parser('verify');p.add_argument('--archive',required=True);p.add_argument('--restore-database',required=True);p.add_argument('--restore-assets');p.add_argument('--report',required=True);p.set_defaults(func=verify)
    p=sub.add_parser('prune');p.add_argument('--directory',required=True);p.set_defaults(func=prune)
    args=parser.parse_args()
    try:args.func(args)
    except subprocess.CalledProcessError:parser.exit(1,'Database/encryption command failed. Check the connection, tool versions and target permissions.\n')
    except (ValueError,OSError,KeyError) as exc:parser.exit(1,str(exc)+'\n')
if __name__=='__main__':main()
