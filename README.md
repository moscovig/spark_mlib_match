# spark_mlib_match

1. Find the data files under /data/*.csv   - partner1.csv, partner2.csv, training.csv 
2. The data files are a result of 
	a.openning the /data/match.xksx file with a Spreadshit tool, replacing all "," with ""
		and saving each sheet as csv.
	b. for each csv, remove headers: cat file.csv | tail -n +2 > no_headers_file.csv	
3. 
