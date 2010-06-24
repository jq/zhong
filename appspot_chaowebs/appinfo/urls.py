from django.conf.urls.defaults import *

urlpatterns = patterns(
  'appinfo.views',
  (r'^loadtest$', 'LoadTestData'),
  (r'^add$', 'SetAppInfo'),
  (r'^(?P<package>[a-zA-z0-9_.]+)$', 'GetAppInfo'),
)
