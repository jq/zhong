from django.conf.urls.defaults import *

urlpatterns = patterns(
  'appinfo.views',
  (r'^loadtest$', 'LoadTestData'),
  #(r'^add$', 'SetAppInfo'),
  (r'^edit/(?P<package>[a-zA-Z0-9_.]+)$', 'EditAppInfo'),
  (r'^(?P<package>[a-zA-Z0-9_.]+)$', 'GetAppInfo'),
)
