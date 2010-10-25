import Image
import sys
import time
import pygame
import random
import string
from BeautifulSoup import BeautifulStoneSoup
import os

def checkImage(filename):
	if not os.path.exists(filename):
		print 'image file not found'
		return -1
	try:
		image = Image.open(filename)
	except:
		logList.append('image error')
		return -1
	else:
		return 0	#print 'image success'

def checkMp3(filename):
	#print filename
	while True:
		if pygame.mixer.music.get_busy()==False:
			try:
				pygame.mixer.music.load(filename)
			except:
				print 'mp3 error'
				logList.append('mp3 error')
				return -1
			else:
				return 0	#print 'mp3 success'
		else:
			time.sleep(2)
			
def parseXML(index):
	xmlname = filedir+'record'+str(index)+'.xml'
	if not os.path.exists(xmlname):
		print 'xml file not found'
		return []
	
	try:
		f = open(xmlname,'r')
	except:
		logList.append('xml open error')
		return []
	else:
		try:
			soup = BeautifulStoneSoup(f.read())
		except:
			logList.append('xml read error')
			return []
		else:
			return [soup.find('image').next,soup.find('ring').next]
		finally:
			f.close()

def run():
	visit_list = []
	pygame.init()
	
	
	for i in range(296):
		# generate random number
		"""index = random.randint(1, 1000)
		while index in visit_list:
				index = random.randint(1, 1000)"""
		
		index = i + 1
		
		print 'checking %d'%(index)
		logList.append('checking %d'%(index))
		
		# parse xml	
		# visit_list.append(index) 	
		pair = parseXML(index)
		if len(pair) == 0:
			continue
		#print str(pair[0])
		#print str(pair[1])
		
		# check image
		checkImage(filedir+pair[0])
		# check mp3
		checkMp3(filedir+pair[1])
		
		time.sleep(1)
		
	pygame.quit()

logList = []

if __name__ == "__main__":
	filedir='../1-fetch/download_Holiday/'
	run()
	
	f = open('log','wb')
	for elem in logList:
		f.write(elem)
	f.close()
