from google.appengine.ext import db
import csv
import datetime
import logging

from ringtone.utils import debug

class MyStringListProperty(db.StringListProperty):
  def __init__ (self, *args, **kwargs):
    super(MyStringListProperty, self).__init__(*args, **kwargs)
    # allow an empty list: djangoforms.StringListProperty's
    # make_value_from_form() can handle an
    # empty value, returning an empty list; leaving as-is is rejected
    # in CharField's clean()
    self.required = False

class Ringtone(db.Model):
  """
  Ringtone contains the intrinstic information about a ringtone. It does not
  contain dynamic information like number of downloads and rating. Typically
  modificaition of any of these fields should result in change of the index.
  """

  CSV_FILEDS = ["key" , "title" , "artist" ,"album", "release_date", "duration",
      "size", "location", "small_image", "medium_image", "large_image",
      "extra_large_image", "tags", "description", "creation_time",
      "modification_time"]

  # Title (name or track) of the ringtone. Required field. User may use
  # "Unknown" if can't specify one.
  title = db.StringProperty(required=True)
  artist = db.StringProperty()
  album = db.StringProperty()
  release_date = db.DateProperty()
  duration = db.IntegerProperty()  # In milliseconds
  size = db.IntegerProperty()
  location = db.StringProperty()   # Where we store it, the download link

  # Image locations
  small_image = db.StringProperty()
  medium_image = db.StringProperty()
  large_image = db.StringProperty()
  extra_large_image = db.StringProperty()

  tags = MyStringListProperty()   # Mainly used for search
  description = db.TextProperty()

  creation_time = db.DateTimeProperty(auto_now_add=True)
  modification_time = db.DateTimeProperty(auto_now=True)
  # For indexing purpose, we don't remove the entry directly once we discard it.
  # Instead, we mark it as delete. We will truly delete it after it has been
  # sent to the indexer, typically by a garbage collection worker.
  is_deleted = db.BooleanProperty(default=False)

  @classmethod
  def _ParseTags(cls, tag_csv):
    r = csv.reader([tag_csv]).next()
    return [unicode(x.strip(), 'utf-8') for x in r]

  @classmethod
  def _DumTags(cls, tags):
    """Dump tags in csv format"""

    import StringIO
    t = [ x.encode('utf-8').strip() for x in tags ]
    output = StringIO.StringIO()
    writer = csv.writer(output)
    writer.writerow(t)
    return output.getvalue().rstrip()

  @classmethod 
  def ParseFromCSVRow(cls, row):
      d = {}
      d['title'] = unicode(row['title'].strip(), 'utf-8') or None
      d['artist'] = unicode(row['artist'].strip(), 'utf-8') or None
      d['album'] = unicode(row['album'].strip(), 'utf-8') or None
      d['release_date'] = row['release_date'].strip() and \
          datetime.datetime.strptime(
          row['release_date'].strip(), "%Y-%m-%dT%H:%M:%S.%fZ").date() or None
      d['duration'] = row['duration'].strip() and \
          int(row['duration'].strip()) or None
      d['size'] = row['size'].strip() and int(row['size'].strip()) or None
      d['location'] = row['location'].strip() or None
      d['small_image'] = row['small_image'].strip() or None
      d['medium_image'] = row['medium_image'].strip() or None
      d['large_image'] = row['large_image'].strip() or None
      d['extra_large_image'] = row['extra_large_image'].strip() or None
      d['tags'] = cls._ParseTags(row['tags'].strip())
      d['description'] = unicode(row['description'].strip(), 'utf-8') or None
      return Ringtone(**d)

  def DumpToCSVRow(self):
    d = {}
    d['key'] = str(self.key())
    d['title'] = self.title and self.title.encode('utf-8') or ""
    d['artist'] = self.artist and self.artist.encode('utf-8') or ""
    d['album'] = self.album and self.album.encode('utf-8') or ""
    d['release_date'] = self.release_date and self.release_date.strftime(
        "%Y-%m-%dT%H:%M:%S.%fZ") or None
    d['duration'] = self.duration != None and str(self.duration) or ""
    d['size'] = self.size != None and str(self.size) or ""
    d['location'] = self.location or ""
    d['small_image'] = self.small_image or ""
    d['medium_image'] = self.medium_image or ""
    d['large_image'] = self.large_image or ""
    d['extra_large_image'] = self.extra_large_image or ""
    d['tags'] = self.tags and Ringtone._DumTags(self.tags) or ""
    d['description'] = self.description and self.description.encode('utf-8') or ""
    d['creation_time'] = self.creation_time.strftime("%Y-%m-%dT%H:%M:%S.%fZ")
    d['modification_time'] = self.modification_time.strftime("%Y-%m-%dT%H:%M:%S.%fZ")
    return d


# We only provide non blocking locking.
class Lock(db.Model):
  is_set = db.BooleanProperty(default=False)

  # Runs in transaction.
  def _TestAndSetInternal(self):
    if self.is_set:
      return True
    else:
      self.is_set = True
      self.save()
      return False
    
  # Test if the lock was acquired. If not, set and return.
  # Returns if the lock was acquired.
  # Returns None if Rollback occurs.
  def TestAndSet(self):
    return db.run_in_transaction(self._TestAndSetInternal)


  def TryLockInternal(self):
    return not self.TestAndSet()

  def UnlockInternal(self):
    self.is_set = False
    self.save()

  @classmethod
  def TryLock(cls, name):
    lock = Lock.get_or_insert("Lock:" + name)
    return lock.TryLockInternal()

  @classmethod
  def Unlock(cls, name):
    lock = Lock.get_or_insert("Lock:" + name)
    return lock.UnlockInternal()


class LastCreationTime(db.Model):
  """A singleton model which stores the biggest timestamp of the *committed*
  ringtone entry."""

  timestamp = db.DateTimeProperty(auto_now=True)
  
  @classmethod
  def Get(cls):
    t = LastCreationTime.get_or_insert("LastCreationTime:", timestamp=0)
    return t.timestamp

  @classmethod
  def SetNow(cls):
    t = LastCreationTime.get_or_insert("LastCreationTime:", timestamp=0)
    t.save()
