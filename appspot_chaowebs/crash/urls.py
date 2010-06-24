from django.conf.urls.defaults import *

urlpatterns = patterns(
    'crash.views',
    (r'^report$', 'Report'),
    (r'^show$', 'Show'),
    )
