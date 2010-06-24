
from django.http import HttpResponse
from django.http import HttpResponseNotFound
from django.http import HttpResponseServerError
from django.utils import simplejson as json
from google.appengine.api import memcache
import os.path
import logging
import urllib2
import traceback

def Search(request):
  try:
    url = "http://mp3.sogou.com/" + os.path.basename(request.get_full_path())
    logging.info(url)
    data = memcache.get(url)
    if data:
      return HttpResponse(data)
    data = urllib2.urlopen(url).read()
    if len(data) > 15000:
      memcache.set(url, data, time = 1800)
    return HttpResponse(data)
  except:
    logging.error(traceback.format_exc())
    return HttpResponseServerError("Error")

def Stats(request):
  stats = memcache.get_stats()
  memcache.flush_all()
  ret = "Cache hits: %s, Cache misses: %s" % (stats["hits"], stats["misses"])
  return HttpResponse(ret)
