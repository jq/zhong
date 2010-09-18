from BeautifulSoup import BeautifulSoup
from google.appengine.api import memcache
import time
import logging
import urllib2
import re
import simplejson as json

def getSogouList(html):
  soup = BeautifulSoup(html)
  allMp3 = []
  timeStart = time.time()
  for item in soup.findAll("tr", id=re.compile("musicmc_*")):
    eachMp3 = {}
    songnameItem = item.find("td", "songname")
    songnameItem = songnameItem.contents[0]
    songnameItem["title"]
    eachMp3["songname"] = songnameItem["title"]
    singerItem = item.find("td", "singger")
    singerItem = singerItem.contents[0]
    eachMp3["singer"] = singerItem["title"]
    albumItem = singerItem.parent.findNextSibling("td", "singger").contents[0]["title"]
    eachMp3["singer"] = albumItem
    sizeItem = item.find("td", nowrap="nowrap").contents[0]
    eachMp3["size"] = sizeItem
    downlink = item.find("a", action="down")
    downlink = downlink["onclick"]
    p = re.compile("window.open\('([^']*)',")
    downlink = p.search(downlink).group(1)
    downlink_list = memcache.get(downlink)
    if downlink_list:
      pass
    else:
      downlink_list = []
      downlink_list.append(downlink)
    eachMp3["downlink"] = downlink_list
    allMp3.append(eachMp3)
  return allMp3

def getJson(key_word, page):
  try:
    url = "http://mp3.sogou.com/music.so?query="
    url = url+key_word
    url = url.strip()
    if (page != ''):
      url = url+"&page="+page
    url = url.strip()
    jsonResponse = memcache.get(url)
    if (jsonResponse is not None):
      return jsonResponse
    result = urllib2.urlopen(url)
    html = result.read()
    html = unicode(html, "gbk")
    mp3List = getSogouList(html)
    jsonResponse = json.dumps(mp3List, sort_keys=True)
  except Exception, e:
    logging.error(e)
    return '[]'
  memcache.set(url, jsonResponse, time=3600*24*7)
  return jsonResponse

def getDownloadLinkList(request):
  url = "http://mp3.sogou.com"+request
  linkList = []
  try:
    page = urllib2.urlopen(url)
    html = page.read()
    linkPattern = re.compile('http://[^"]+\.mp3|http://[^"]+\.wma', re.IGNORECASE)
    linkList = linkPattern.findall(html)
    memcache.set(request, str(linkList), 3600*24*7)
  except Exception, e:
    logging.error(e)
    pass
  return linkList
