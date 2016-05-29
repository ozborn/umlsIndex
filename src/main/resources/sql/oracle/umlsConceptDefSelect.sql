SELECT
thetable.thecui,
minconstr || ' ' || maxconstr || ' ' || modeconstr,
mintui||' '||maxtui||' '||modetui
-- || ' ' || modecondef
FROM
(
SELECT
mrconso.cui as thecui
-- ,listagg(tui,' ') WITHIN GROUP (ORDER BY tui) as stypes
,MIN(tui) as mintui,MAX(tui) as maxtui,stats_mode(tui) as modetui --12 characters
,cast (MIN(mrconso.str) as varchar(150)) as minconstr
,cast (MAX(mrconso.str) as varchar(150)) as maxconstr
,cast(stats_mode(mrconso.str) as varchar(150)) as modeconstr -- 450 characters for strings
,cast(stats_mode(mrdef.def)as varchar(3500)) as modecondef
FROM mrconso LEFT JOIN MRDEF ON (mrconso.cui=mrdef.cui)
JOIN MRSTY ON (mrconso.cui=mrsty.cui)
WHERE mrconso.LAT='ENG'
-- AND mrconso.ts = 'P'
-- AND mrconso.stt = 'PF'
-- AND mrconso.ispref = 'Y'
-- AND tui IN ('T046','T047')
GROUP BY mrconso.cui
) thetable
