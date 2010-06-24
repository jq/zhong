from django.conf.urls.defaults import *


urlpatterns = patterns(
  'msearch.views',
  (r'^music.so$', 'Search'),
  (r'^stats$', 'Stats')
)
