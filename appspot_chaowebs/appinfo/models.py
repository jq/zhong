from google.appengine.ext import db

# Keyed by package
class AppInfo(db.Model):
  version = db.StringProperty(required=True)
  url = db.StringProperty()
  message = db.TextProperty()
  seq = db.IntegerProperty(default=0)
