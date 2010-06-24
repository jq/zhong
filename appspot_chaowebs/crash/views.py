
from crash import models

from django.http import HttpResponse
from django.http import HttpResponseNotFound
from django.http import HttpResponseServerError

import os.path
import logging
import traceback

def Report(request):
  package = request.REQUEST.get('package', '')
  version = request.REQUEST.get('version', '')
  stacktrace = request.REQUEST.get('stacktrace', '')
  entry = models.CrashEntry(package=package, version=version, stacktrace=stacktrace)
  entry.put()
  return HttpResponse("ok")


def Show(request):
  package = request.REQUEST.get('package', '')
  version = request.REQUEST.get('version', '')
  query = models.CrashEntry.all()
  if package:
    query = query.filter('package = ', package)
  if version:
    query = query.filter('version = ', version)

  output="""
  <html>
  <title>Crash Report</title>
  <body>
  """
  entries = []
  for e in query.order('-time').fetch(100):
    entries.append("%s %s %s<br>%s<br>" % (
      e.time.strftime("%b %d %Y %H:%M:%S"),
      e.package,
      e.version,
      e.stacktrace.replace('\n', '<br>')))

  output += "<br>".join(entries)
  output += "</body></html>"
  return HttpResponse(output)
