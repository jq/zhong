import urllib2
import sys
import util
import simplejson as json

from google.appengine.api import memcache
from google.appengine.api import urlfetch
from google.appengine.api import users
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app


class DownloadLinkPage(webapp.RequestHandler):
    
    
  def get(self):
    key_word = self.request.get("key")
    url = key_word
    url = url.strip()
    json_response = memcache.get(url)
    if (json_response is None):
      linkList = util.getDownloadLinkList(url)
      if (len(linkList)==0):
        json_response = "[]"
      else:
        json_response = json.dumps(linkList, sort_keys=True)
        memcache.add(url, json_response, time=3600*24*30)
    self.response.out.write(json_response)

