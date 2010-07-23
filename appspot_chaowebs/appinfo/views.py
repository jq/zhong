from appinfo import models
from django.http import HttpResponse
from django.http import HttpResponseNotFound

from django.http import HttpResponseRedirect
from django.http import HttpResponseForbidden
from django.template import RequestContext
from django.utils import simplejson
from django.shortcuts import render_to_response

from google.appengine.ext.db import djangoforms
from google.appengine.api import users

import logging

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


def login_required(func):
  """Decorator that redirects to the login page if you're not logged in."""

  def login_wrapper(request, *args, **kwds):
    if request.user is None:
      return HttpResponseRedirect(
          users.create_login_url(request.get_full_path().encode('utf-8')))
    return func(request, *args, **kwds)

  return login_wrapper


def admin_required(func): 
  """Decorator that insists that you're logged in as administratior.""" 
 
  def admin_wrapper(request, *args, **kwds): 
    if request.user is None: 
      return HttpResponseRedirect( 
          users.create_login_url(request.get_full_path().encode('utf-8'))) 
    if not request.user_is_admin: 
      return HttpResponseForbidden('You must be admin in for this function') 
    return func(request, *args, **kwds) 
 
  return admin_wrapper 


class AppInfoForm(djangoforms.ModelForm):
  class Meta:
    model = models.AppInfo

@admin_required
def EditAppInfo(request, package):
  if request.method == 'POST':
    form = AppInfoForm(request.POST)
    if form.is_valid():
      app = form.save(commit = False) 
      saved_app = models.AppInfo(key_name = package,
          **dict([(prop, getattr(app, prop)) for prop in app.properties()]))
      saved_app.save()
  else:
    app = models.AppInfo.get_by_key_name(package)
    form = AppInfoForm(instance = app)
  
  return render_to_response("app_info.html",
      {'form' : form},
      context_instance=RequestContext(request))
