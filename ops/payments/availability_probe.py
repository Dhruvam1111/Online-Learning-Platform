#!/usr/bin/env python3
"""Sample readiness and calculate a measured monthly SLA, exposing monitoring gaps."""
import argparse,calendar,datetime,json,pathlib,time,urllib.request,urllib.error
UTC=datetime.timezone.utc

def monthly_report(rows,month,maintenance=(),now=None,max_gap=120):
    now=now or datetime.datetime.now(UTC)
    start=datetime.datetime.strptime(month,'%Y-%m').replace(tzinfo=UTC)
    end=(start.replace(day=28)+datetime.timedelta(days=4)).replace(day=1)
    cutoff=min(now,end)
    if cutoff<=start:raise ValueError('The requested monitoring month has not started')
    windows=[]
    for item in maintenance:
        left=max(start,datetime.datetime.fromisoformat(item['start']));right=min(cutoff,datetime.datetime.fromisoformat(item['end']))
        if right>left:windows.append((left,right))
    merged=[]
    for left,right in sorted(windows):
        if merged and left<=merged[-1][1]:merged[-1]=(merged[-1][0],max(merged[-1][1],right))
        else:merged.append((left,right))
    def eligible(left,right):
        return max(0,(right-left).total_seconds()-sum(max(0,(min(right,b)-max(left,a)).total_seconds()) for a,b in merged))
    observed=sorted({datetime.datetime.fromisoformat(row['time']):bool(row['up']) for row in rows}.items())
    up=down=0.0
    for index,(stamp,state) in enumerate(observed):
        next_stamp=observed[index+1][0] if index+1<len(observed) else cutoff
        if next_stamp-stamp>datetime.timedelta(seconds=max_gap):continue
        left=max(stamp,start);right=min(next_stamp,cutoff)
        if right<=left:continue
        duration=eligible(left,right)
        if state:up+=duration
        else:down+=duration
    total=eligible(start,cutoff);unknown=max(0,total-up-down)
    complete=now>=end and unknown<.001
    return {'month':month,'upSeconds':round(up,3),'downSeconds':round(down,3),'unknownSeconds':round(unknown,3),'scheduledMaintenanceExcludedSeconds':round((cutoff-start).total_seconds()-total,3),'observedAvailabilityPercent':round(100*up/(up+down),5) if up+down else None,'monthlySlaEstablished':complete,'meets99point5Percent':up/total>=.995 if complete and total>0 else None,'note':'Incomplete months or gaps cannot establish the monthly SLA.'}

def main():
    p=argparse.ArgumentParser();p.add_argument('--url');p.add_argument('--log',required=True);p.add_argument('--samples',type=int,default=1);p.add_argument('--interval',type=float,default=60);p.add_argument('--summary-month');p.add_argument('--maintenance');p.add_argument('--max-gap',type=float,default=120);p.add_argument('--report');a=p.parse_args()
    if a.samples<1 or a.interval<0 or a.max_gap<=0:p.error('Choose positive samples/max-gap and a nonnegative interval')
    path=pathlib.Path(a.log);path.parent.mkdir(parents=True,exist_ok=True)
    if not a.summary_month:
        if not a.url:p.error('--url is required for sampling')
        for index in range(a.samples):
            start=time.perf_counter();ok=False
            try:
                with urllib.request.urlopen(a.url,timeout=10) as response:ok=response.status==200 and json.load(response).get('status')=='UP'
            except (OSError,ValueError,urllib.error.URLError):pass
            row={'time':datetime.datetime.now(UTC).isoformat(),'up':ok,'seconds':round(time.perf_counter()-start,3)}
            with path.open('a') as out:out.write(json.dumps(row)+'\n')
            if index+1<a.samples:time.sleep(a.interval)
    rows=[json.loads(line) for line in path.read_text().splitlines() if line.strip()]
    if a.summary_month:
        result=monthly_report(rows,a.summary_month,json.loads(pathlib.Path(a.maintenance).read_text()) if a.maintenance else (),max_gap=a.max_gap)
    else:result={'observations':len(rows),'first':rows[0]['time'],'last':rows[-1]['time'],'observedSuccessPercent':round(100*sum(row['up'] for row in rows)/len(rows),3),'monthlySlaEstablished':False}
    if a.report:pathlib.Path(a.report).write_text(json.dumps(result,indent=2))
    print(json.dumps(result))
if __name__=='__main__':main()
