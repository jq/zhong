from BeautifulSoup import BeautifulSoup
import urllib
import string
from Queue import Queue
import threading
import time
import socket

class  DownloadThread(threading.Thread):
     def __init__(self, url, fName, index):
          threading.Thread.__init__(self, name='downloadThread')
          self.url = url
          self.downloadDir = "./download_Rock/"
          self.fName= self.downloadDir+fName
          self.index = index
     
     def  finish(self):
         downloadQueue.get()
         
     def run(self):
           try:
                 f = open(self.fName, 'wb')
           except:
                 print  'record '+ self.index+': output open err'
                 self.finish()
                 return -1
                   
 
      
           numTry  = 3
           while  numTry > 0:     
                 try:   
                     u = urllib.urlopen(self.url)   
                 except:
                     numTry = numTry - 1
                     #print  'record '+self.index+': '+self.url+' open  err '+str(3-numTry)+' try'
                     time.sleep(1)
                 else:
                     try:
                         data = u.read()
                     except:
                         numTry = numTry - 1
                         #print  'record '+self.index+': '+self.url+' url data '+str(3-numTry)+' try'
                         time.sleep(1)
                         u.close()
                     else:
                         f.write(data)
                         u.close()
                         break
           
           f.close() 
           self.finish()
           if  numTry > 0:
                 return 0
           else:
                 print 'record '+self.index+' urlname:'+self.url+' filename:'+self.fName
                 return -1

"""
class DownloadScheduler(threading.Thread):
     def __init__(self):
          threading.Thread.__init__(self, name = 'downloadScheduler')
     
     def  run(self): 
        while  running[0]:
            downloadThreadPool = []
            while  not  downloadQueue.empty():
                downloadThreadPool.append(DownloadThread(downloadQueue.get()))
            for  downloadThread  in  downloadThreadPool:
                downloadThread.start()
            for  downloadThread  in  downloadThreadPool:
                downloadThread.join()
            #time.sleep(0.2)
"""            
"""
class Scheduler(threading.Thread):
     def __init__(self):
          threading.Thread.__init__(self, name = 'scheduler')
          
     def run(self):
          while running[0]:
               ringThreadPool = []
               while not queue.empty():
                    ringThreadPool.append(RingThread(queue.get()))
                
               for  ringThread  in  ringThreadPool:
                    ringThread.start()
               for  ringThread  in  ringThreadPool:
                    ringThread.join()
               # time.sleep(0.2)
"""

class RingThread(threading.Thread):
    def  __init__(self, url, index):
         threading.Thread.__init__(self, name='RingThread')
         self.url =  url 
         self.index = str(index)
         self.downloadDir = "./download_Rock/"
         
    def  logError(self, info):
         print  info + ' of record ' + self.index
         
    def  finish(self):
         queue.get()
           
    def  run(self):
         numTry = 3
         while numTry > 0:
             try:
                 #print self.url
                 u = urllib.urlopen(self.url)
             except:
                 numTry = numTry - 1
                 self.logError('url can not open '+str(3-numTry)+ ' try')
                 #time.sleep(1)
             else:
                 try:
                     data = u.read()
                 except:
                         numTry = numTry - 1
                         self.logError('url data not get '+str(3-numTry)+ ' try')
                         u.close()
                         #time.sleep(1)
                 else :
                     break;
                 
         if  numTry <= 0:        
             self.finish()
             return -1
         
        
         # ring url open success
         soup = BeautifulSoup(data)
         u.close()
         record = ['<Record>']
         
         # attri Title
         temp = soup.find('h4')
         if  temp !=None:
             temp = str(temp.next)
             record.append('<Title>'+temp[temp.index('Free')+5:temp.index('Ringtone')-1]+'</Title>')
         else:
             self.logError('title not found')
             self.finish()
             return -1
         
         # attri  Image
         temp = soup.find('div', attrs={'class':'image'})
         if temp != None:
             imageurl = str(temp.next['src'])
             splitPath = imageurl.split('/')
             imagefile = splitPath.pop().strip()
             record.append('<Image>'+imagefile+'</Image>')
             # download image
             #print 'imageurl '+ imageurl
             imageThread = DownloadThread(imageurl, imagefile, self.index) 
             imageThread.start()
             downloadQueue.put(1)
             """imageThread = DownloadThread(imageurl, imagefile)
             imageThread.start()"""
             """if  imageThread.getResult() < 0:
                 self.logError('image download error')
                 return -1"""
         else: 
             self.logError('image not found')
             self.finish()
             return -1 
             
         # attrs including Artist,Downloads,Size etc
         for infoElem in soup.findAll('div',attrs={'class':'info'}):
               for specInfoElem in infoElem.findAll('span', attrs={'class':'grey'}):
                    item = specInfoElem.next
                    itemname = str(item).strip()[:-1]
                    if itemname=='Artist' or itemname=='Category':
                         record.append('<'+itemname+'>'+str(item.nextSibling.next).strip()+'</'+itemname+'>')
                    elif itemname=='Date Added':
                         record.append('<Date>'+str(specInfoElem.nextSibling).strip()+'</Date>')
                    else:
                         record.append('<'+itemname+'>'+str(specInfoElem.nextSibling).strip()+'</'+itemname+'>')
          
         # start-rating 
         infoElem = soup.find('li',attrs={'id':'rsli'})
         itemname = str(infoElem['style'])
         record.append('<Mark>'+itemname[itemname.index(':')+1:itemname.index('%')]+'</Mark>')  
          
          
          # attri  Ring
         divElem = soup.find('div',attrs={'class':'det2'})
         if divElem == None:
              self.logError('ring not found')
              self.finish()
              return -1
         ringurl = str(divElem.find('a')['href'])
         if  ringurl == None:
              self.logError('ring not found')
              self.finish()
              return -1
         
         pos = ringurl.find('.mp3')+4 
         if    pos == 3:
               pos = ringurl.find('.wav')+4
               if pos == 3:
                     self.logError('not mp3 or wav format')
                     self.finish()
                     return -1
         ringfile = ringurl[ringurl.index('file=')+5:pos].strip()         
         record.append('<Ring>'+ringfile+'</Ring>')
         ringurl = 'http://music.mabilo.com/dl'+ringurl[ringurl.index('.php'):pos]
         #download ring
         #print 'ringurl '+ringurl
         downloadThread = DownloadThread(ringurl, ringfile, self.index)
         downloadThread.start()
         downloadQueue.put(1)
         """ringThread = DownloadThread(ringurl, ringfile)
         ringThread.start()""" 
         """if   ringThread.getResult()< 0:
              self.logError('ring download error')
              return -1"""
         soup.close()     
         record.append('</Record>')
         
         self.storeRecord(record) 
         self.finish()
         return 0
         
     # store  the record to disk
    def  storeRecord(self, record):
          try:
                rdFile = open(self.downloadDir+'record'+str(self.index)+'.xml','wb')
          except:
                self.logError('store record to dist error')
                return  -1
               
          rdFile.write(''.join(record))
          rdFile.close() 
          return 0
               
         
         
         
def  solveEachCategory(origurl):
     global  recordIndex,  urlHeader
     cnt = 1
     while True:
         if cnt == 1:
             url = origurl
         else :
             url = origurl[0:origurl.index('.htm')]+'-'+str(cnt)+'-tr.htm'
         cnt = cnt + 1
         
         if cnt > 1501:
             break
         
         numTry = 3
         while  numTry > 0:      
             try:
                 u = urllib.urlopen(url)
             except:
                 numTry = numTry - 1
                 print  'category '+url+' open err '+str(3-numTry)+ ' try'
                 #time.sleep(1)
             else:
                 try:
                     data = u.read()
                 except:
                     u.close()
                     numTry = numTry - 1
                     print 'category '+url+' get data  err '+str(3-numTry)+' try'
                     #time.sleep(1)
                 else :
                     break;    
          
         if  numTry <= 0:
             recordIndex += 10       # assume there are 10 rings per page
             continue
                    
         if u.url != url:                         # judge if reach the last page 
             u.close()
             return 0          
         
         soup = BeautifulSoup(data)
         u.close()

         for divElem in soup.findAll('div',attrs={'class':'row2'}):  # get TAG with attribute
             # process   each  ring
             print 'processing '+str(recordIndex)
             # process  a   ring 
             ringurl = divElem.find('a')['href'];
             if ringurl == None:
                 self.errorLog('ring')
                 continue
             ringurl = urlHeader+ringurl
             ringThread = RingThread(ringurl, recordIndex)
             ringThread.start()
             queue.put(1)
             recordIndex = recordIndex + 1
             # for test
             #if  recordIndex > 1080:
             #	 return;
             
         soup.close()
              

recordIndex = 1;

if __name__ == "__main__":     
     queue = Queue(3)
     downloadQueue = Queue(5)
     
     """
     running = []
     running.append(True)
     
     scheduler = Scheduler()
     scheduler.start()
     downloadScheduler = DownloadScheduler()
     downloadScheduler.start()
     """
     
     mainUrl = 'http://mabilo.com/ringtones.htm'
     urlHeader = 'http://mabilo.com'
     
     timeout = 40
     socket.setdefaulttimeout(timeout)
     
     # get category_list
     u = urllib.urlopen(mainUrl)
     category_list = []
     soup = BeautifulSoup(u.read())
     u.close()
     for elem in soup.findAll('ul')[-1]:
         elem = str(elem)
         start = elem.find('"',0)
         stop = elem.find('"',start+1)
         category_list.append(elem[start+1:stop])
     soup.close()
     
     # process category_list
     for elem in category_list[15:16]:
         solveEachCategory(urlHeader+elem) 
     """
     while  not downloadQueue.empty():
         time.sleep(20)
     """
     
