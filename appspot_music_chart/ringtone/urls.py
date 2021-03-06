# Copyright 2008 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""URL mappings for the codereview package."""

# NOTE: Must import *, since Django looks for things here, e.g. handler500.
from django.conf.urls.defaults import *

urlpatterns = patterns(
    'ringtone.views',
    url(r'^list/$', 'ListRingtones', name='list_ringtones'),
    url(r'^details/(?P<key>.+)$', 'RingtoneDetails', name='ringtone_details'),
    url(r'^upload/$', 'UploadCSVFile', name='upload_csv'),
    url(r'^download/(?P<since>.*)$', 'DownloadRingtoneDataSince'),
    url(r'^search/$', 'Search', name='search'),
    )
