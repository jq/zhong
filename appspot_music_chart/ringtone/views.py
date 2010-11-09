from django import forms
from django.http import Http404
from django.http import HttpResponse
from django.http import HttpResponseRedirect
from django.core.urlresolvers import reverse
from django.shortcuts import render_to_response
from django.utils import simplejson as json
from django.utils.http import urlquote
from google.appengine.ext.db import djangoforms
from google.appengine.api import urlfetch

from ringtone import models
from ringtone.utils import debug
from ringtone.utils import admin_required

import csv
import datetime
import logging
import traceback
import urllib2

_CSV_LOCK = "csv_lock"

class RingtoneForm(djangoforms.ModelForm):
  class Meta:
    model = models.Ringtone

class SearchForm(forms.Form):
  q = forms.CharField(max_length=100)

# List/Add ringtones.
def ListRingtones(request):
  ringtones = models.Ringtone.all()
  return render_to_response(
      'ringtone/list_ringtones.html',
      { "ringtones" : ringtones,
      })

def RingtoneDetails(request, key):
  try:
    ringtone = models.Ringtone.get(key)
  except:
    raise Http404
  if request.method == 'POST':
    form = RingtoneForm(request.POST, instance=ringtone)
    if form.is_valid():
      form.save()
      return HttpResponseRedirect(reverse('ringtone_details',
        kwargs={'key' : key}))
  else:
    form = RingtoneForm(instance=ringtone)

  return render_to_response(
      'ringtone/ringtone_details.html',
      {
        "form" : form
      })


class UploadCSVForm(forms.Form):
  file = forms.FileField


@admin_required
def UploadCSVFile(request):
  if request.method == 'POST':
    form = UploadCSVForm(request.POST, request.FILES)
    if form.is_valid() and request.FILES.has_key('csv'):
      # Acquire the lock first.
      logging.info("Trying to lock")

      if not models.Lock.TryLock(_CSV_LOCK):
        return HttpResponse('Failed to acquire csv lock.')

      last_creation_time = models.LastCreationTime.Get()
      # Clean up all ringtones whose creation time is bigger than
      # last_creation_time because they were part of unsuccessful updates.
      logging.info("Last creation time: " + str(last_creation_time))

      for r in models.Ringtone.all().filter('creation_time >',
          last_creation_time):
        logging.info("Deleting: " + r.title + " (" + str(r.creation_time) + ")")
        r.delete()

      if _ProcessUploadedCSVFileLocked(request.FILES['csv']):
        return HttpResponse('ok')
      else:
        # Should we use a response code other than 200 here?
        return HttpResponse('Failed: see logs for more details.')
  else:
    form = UploadCSVForm()
  return render_to_response('ringtone/upload_csv.html', { 'form' : form })

# Lock must be held.
def _ProcessUploadedCSVFileLocked(f):
  try:
    reader = csv.DictReader(f)
    for row in reader:
      r = models.Ringtone.ParseFromCSVRow(row)
      r.save()

    # Update last creation time.
    models.LastCreationTime.SetNow()
    logging.info("New last creating time: " + str(models.LastCreationTime.Get()))
    return True
  except:
    logging.error(traceback.format_exc())
    return False
  finally:
    logging.info("Releasing csv lock")
    models.Lock.Unlock(_CSV_LOCK)


def DownloadRingtoneData(request):
  """
  Get all ringtone data in csv format. For testing purpose.
  """
  response = HttpResponse(mimetype='text/csv')
  response['Content-Disposition'] = 'attachment; filename=ringtones.csv'

  writer = csv.DictWriter(response, models.Ringtone.CSV_FILEDS)
  # Hack. Write the header first.
  d = {}
  for k in models.Ringtone.CSV_FILEDS:
    d[k] = k
  writer.writerow(d)
  for r in models.Ringtone.all():
    writer.writerow(r.DumpToCSVRow())
  return response


def DownloadRingtoneDataSince(request, since):
  """
  Download ringtone data whose creation timestamp is greater than or equal to
  "since". Data will be downloaded in csv format.
  """
  response = HttpResponse(mimetype='text/csv')
  response['Content-Disposition'] = 'attachment; filename=ringtones.csv'

  writer = csv.DictWriter(response, models.Ringtone.CSV_FILEDS)
  # Hack. Write the header first.
  d = {}
  for k in models.Ringtone.CSV_FILEDS:
    d[k] = k
  writer.writerow(d)
  if since:
    query = models.Ringtone.all().filter('creation_time >= ',
        datetime.datetime.strptime(since, "%Y-%m-%dT%H:%M:%S.%fZ"))
  else:
    query = models.Ringtone.all()
  for r in query:
    writer.writerow(r.DumpToCSVRow())
  return response


NUM_ROWS = 20
SOLR_URL = "http://204.236.135.108/?version=2.2&indent=on&wt=json&rows=20"
SEARCH_URL = ""

def Search(request):
  results = None
  prev_url = None
  next_url = None

  # Lazy initialization. Seems that reverse() can't be called at module level?
  global SEARCH_URL
  if not SEARCH_URL:
    SEARCH_URL = reverse("search")

  if request.method == 'GET' and request.GET.has_key('q'):
    # TODO: Clean data.
    q = request.GET['q']
    if q:
      q = urlquote(q)
      try:
        start = request.GET['start'] or 0
        start = int(start)
      except:
        start = 0
      url = SOLR_URL + "&start=" + str(start) + "&q=" + q
      reply = urlfetch.fetch(url)
      if reply.status_code == 200:
        results = json.loads(reply.content)
        if start > 0:
          prev_start = max(0, start - NUM_ROWS)
          prev_url = SEARCH_URL + "?start=" + str(prev_start) + "&q=" + q
        next_start = start + NUM_ROWS
        if next_start < results['response']['numFound']:
          next_url = SEARCH_URL + "?start=" + str(next_start) + "&q=" + q
          
  return render_to_response('ringtone/search.html',
      {'results' : results,
       'prev_url' : prev_url,
       'next_url' : next_url}) 
