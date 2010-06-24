
from google.appengine.ext import db

class CrashEntry(db.Model):
  package = db.StringProperty(required=True)
  version = db.StringProperty(required=False)
  time = db.DateTimeProperty(auto_now_add=True)
  stacktrace = db.TextProperty()
