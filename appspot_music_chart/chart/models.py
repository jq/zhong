from google.appengine.ext import db

class JsonData(db.Model):
  data = db.TextProperty(required=True)
  @classmethod
  def Get(cls, key):
    json = cls.get_by_key_name(key)
    if not json:
      return None
    return json.data

  @classmethod
  def Set(cls, key, value):
    json = cls.get_by_key_name(key)
    if json == None:
      json = JsonData(data = value, key_name = key)
    else:
      json.data = value
    json.save()
