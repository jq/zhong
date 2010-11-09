from django.http import HttpResponse, HttpResponseRedirect
from django.http import HttpResponseForbidden, HttpResponseNotFound

from google.appengine.api import users

import logging

_DEBUG = True

def debug(msg):
  if _DEBUG:
    logging.info('\033[95m[DEBUG]\033[0m' + msg)

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
