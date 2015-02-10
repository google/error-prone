#! /usr/bin/env python

import sys
import csv
import bs4

with open(sys.argv[1], 'r') as myfile:
	s=myfile.read()

page    = s
soup    = bs4.BeautifulSoup(page)

fname = ''

for table in soup.findAll('table'):
	if table.h4 != None:
		fname = table.h4.text
		if fname.endswith('.java'):
			fname = fname.split(' ')[2]
		else:
			fname = ''
		#print fname
			
	else:
		if fname != '':
			for row in table.findAll('tr'):
				tds = row.findAll('td')
				if len(tds) > 0:
					func = tds[0].text[4:]
					if func.endswith('()'):
						print '%s:%s,%s'%(fname,func,tds[1].text)
	
