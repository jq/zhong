'''
Module to fetch data from billboard.com.
'''

_API_KEY = "77gdmp33g783udnhs2u97q2x"
_MAX_RESULTS_PER_REQUEST = 50

_BASE_URL = "http://api.billboard.com/apisvc/chart/v1"
_DEBUG = False

from django.utils import simplejson as json

import operator
import time
import logging
import urllib2
import traceback


def GetUri(code, start):
  if start == 1 or start == 0:
    return (_BASE_URL +
        "/list?id=%d&format=json&api_key=%s&sort=date-" % (code, _API_KEY))
  else:
    return (_BASE_URL +
        "/list?id=%d&format=json&api_key=%s&start=%d&sort=date-" %
        (code, _API_KEY, start))
  
def FetchData(url):
  try:
    data = urllib2.urlopen(url).read()
    return json.loads(data)
  except:
    return None

def ExtractData(data, issue_date):
  result = []
  for item in data["searchResults"]["chartItem"]:
    if item["chart"]["issueDate"] != issue_date:
      return (result, True)
    result.append((item["song"], item["artist"], item["rank"])) 
  return (result, False)
    

def GetChartData(code, num = 200):
  result = []
  issue_date = None
  start = 1
  try:
    while len(result) < num:
      url = GetUri(code, start)
      print "chart url:", url
      data = FetchData(url)
      start += int(data["searchResults"]["totalReturned"])
      if not issue_date:
        issue_date = data["searchResults"]["chartItem"][0]["chart"]["issueDate"]
      (r, finished) = ExtractData(data, issue_date)
      result += r
      if finished:
        break
      time.sleep(2)
    result.sort(key=operator.itemgetter(2))
    return {"list" : result}
  except:
    logging.error(traceback.format_exc())
    return None

if __name__ == "__main__":
  import sys
  code = 396
  num = 100
  if len(sys.argv) > 1:
    code = int(sys.argv[1])
  if len(sys.argv) > 2:
    num = int(sys.argv[2])
  print "code: %d, num: %d" % (code, num)
  print GetChartData(code, num)

