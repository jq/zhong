import logging
import urllib2
import sys
import util
import getdownloadlink
from BeautifulSoup import BeautifulSoup
from google.appengine.api import urlfetch
from google.appengine.api import users
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app

class MainPage1(webapp.RequestHandler):
    
    
  def get(self):
    key_word = self.request.get("key")
    page = self.request.get("page")
    json_response = util.getJson(key_word, page)
    self.response.out.write(json_response)



application = webapp.WSGIApplication([('/search', MainPage1), ('/downloadlink', getdownloadlink.DownloadLinkPage)], debug=True)


def main():
    run_wsgi_app(application)


if __name__ == "__main__":
    main()
