from appinfo import models
from django.http import HttpResponse
from django.http import HttpResponseNotFound

from django.utils import simplejson

def GetAppInfo(request, package):
  app = models.AppInfo.get_by_key_name(package)
  if app is None:
    return HttpResponseNotFound("Package not found")

  d = dict(version = app.version or "", 
           url = app.url or "",
           message = app.message or "",
           seq = app.seq)
        
  ret = simplejson.dumps(d)
  return HttpResponse(ret)


def SetAppInfo(request):
  package = request.REQUEST.get('package', '')
  version = request.REQUEST.get('version', '')
  url = request.REQUEST.get('url', '')
  message = request.REQUEST.get('message', '')
  seq = int(request.REQUEST.get('seq', ''))

  app = models.AppInfo(key_name = package,
      version = version,
      url = url,
      message = message,
      seq = seq)
  app.put()
  return HttpResponse("ok")



def LoadTestData(request):
  app = models.AppInfo(key_name = "test.package",
                       version = "1.0",
                       url = "http://test-url",
                       message = "test message")
  app.put()
  app = models.AppInfo(key_name = "test.package2",
                       version = "1.0")
  app.put()

  app = models.AppInfo(key_name = "com.happy.life",
                       version = "1.0",
                       url = "http://www.google.com",
                       message = "test message",
                       seq = 1)
  app.put()
  return HttpResponse("ok")
