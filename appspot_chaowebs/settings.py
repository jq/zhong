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

"""Minimal Django settings."""

import os

APPEND_SLASH = False
DEBUG = os.environ['SERVER_SOFTWARE'].startswith('Dev')
INSTALLED_APPS = (
    'testproject'
)
MIDDLEWARE_CLASSES = (
    #'firepython.middleware.FirePythonDjango',
    #'appstats.recording.AppStatsDjangoMiddleware',
    'django.middleware.common.CommonMiddleware',
    'django.middleware.http.ConditionalGetMiddleware',
)
ROOT_URLCONF = 'urls'
TEMPLATE_CONTEXT_PROCESSORS = (
    'django.core.context_processors.request',
)
TEMPLATE_DEBUG = DEBUG
TEMPLATE_DIRS = (
    os.path.join(os.path.dirname(__file__), 'templates'),
    )
TEMPLATE_LOADERS = (
    'django.template.loaders.filesystem.load_template_source',
    )
FILE_UPLOAD_HANDLERS = (
    'django.core.files.uploadhandler.MemoryFileUploadHandler',
)
FILE_UPLOAD_MAX_MEMORY_SIZE = 1048576  # 1 MB

RIETVELD_INCOMING_MAIL_ADDRESS = ('reply@%s.appspotmail.com'
                                  % os.getenv('APPLICATION_ID'))

