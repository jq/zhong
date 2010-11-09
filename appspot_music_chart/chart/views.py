from chart import billboard
from chart import models
from chart import Constants
from django.http import HttpResponse
from django.http import HttpResponseNotFound
from django.http import HttpResponseServerError
from django.utils import simplejson as json
import logging

# General function.
def _UpdateBillboardData(name, code, num):
  data = billboard.GetChartData(code, num)
  if not data or len(data.get("list", [])) < 20:
    return HttpResponseServerError("Error: not enough result")

  models.JsonData.Set(name, json.dumps(data))
  return HttpResponse("ok")

def UpdateBillboardHot100Singles(request):
  return _UpdateBillboardData(Constants.NAME_BILLBOARD_HOT_100_SINGLES, 
      Constants.CODE_BILLBOARD_HOT_100_SINGLES, 100)

def GetBillboardHot100Singles(request):
  return HttpResponse(models.JsonData.Get(
    Constants.NAME_BILLBOARD_HOT_100_SINGLES))


def UpdateBillboard200Albums(request):
  return _UpdateBillboardData(Constants.NAME_BILLBOARD_200_ALBUMS,
      Constants.CODE_BILLBOARD_200_ALBUMS, 200)

def GetBillboard200Albums(request):
  return HttpResponse(models.JsonData.Get(
    Constants.NAME_BILLBOARD_200_ALBUMS))

def UpdateBillboardHotRnBHipHopSongsSingles(request):
  return _UpdateBillboardData(
      Constants.NAME_BILLBOARD_HOT_RNB_HIP_HOP_SONGS_SINGLES,
      Constants.CODE_BILLBOARD_HOT_RNB_HIP_HOP_SONGS_SINGLES,
      100)

def GetBillboardHotRnBHipHopSongsSingles(request):
  return HttpResponse(models.JsonData.Get(
    Constants.NAME_BILLBOARD_HOT_RNB_HIP_HOP_SONGS_SINGLES))

def UpdateBillboardCountrySongsSingles(request):
  return _UpdateBillboardData(
    Constants.NAME_BILLBOARD_COUNTRY_SONGS_SINGLES,
    Constants.CODE_BILLBOARD_COUNTRY_SONGS_SINGLES,
    100)

def GetBillboardCountrySongsSingles(request):
  return HttpResponse(models.JsonData.Get(
    Constants.NAME_BILLBOARD_COUNTRY_SONGS_SINGLES))

def UpdateBillboardModernRockTracksSingles(request):
  return _UpdateBillboardData(
    Constants.NAME_BILLBOARD_MODERN_ROCK_TRACKS_SINGLES,
    Constants.CODE_BILLBOARD_MODERN_ROCK_TRACKS_SINGLES,
    100)

def GetBillboardModernRockTracksSingles(request):
  return HttpResponse(models.JsonData.Get(
    Constants.NAME_BILLBOARD_MODERN_ROCK_TRACKS_SINGLES))

def UpdateBillboardDanceClubPlaySingles(request):
  return _UpdateBillboardData(
      Constants.NAME_BILLBOARD_DANCE_CLUB_PLAY_SINGLES,
      Constants.CODE_BILLBOARD_DANCE_CLUB_PLAY_SINGLES,
      100)

def GetBillboardDanceClubPlaySingles(request):
  return HttpResponse(models.JsonData.Get(
    Constants.NAME_BILLBOARD_DANCE_CLUB_PLAY_SINGLES))

def UpdateBillboardHotRapTracksSingles(request):
  return _UpdateBillboardData(
      Constants.NAME_BILLBOARD_HOT_RAP_TRACKS_SINGLES,
      Constants.CODE_BILLBOARD_HOT_RAP_TRACKS_SINGLES,
      100)
  
def GetBillboardHotRapTracksSingles(request):
  return HttpResponse(models.JsonData.Get(
    Constants.NAME_BILLBOARD_HOT_RAP_TRACKS_SINGLES))


def UpdateBillboardPop100Singles(request):
  return _UpdateBillboardData(
      Constants.NAME_BILLBOARD_POP_100_SINGLES,
      Constants.CODE_BILLBOARD_POP_100_SINGLES,
      100)

def GetBillboardPop100Singles(request):
  return HttpResponse(models.JsonData.Get(
    Constants.NAME_BILLBOARD_POP_100_SINGLES))

def UpdateBillboardHotMainstreamRockTracksSingles(request):
  return _UpdateBillboardData(
      Constants.NAME_BILLBOARD_HOT_MAINSTREAM_ROCK_TRACKS_SINGLES,
      Constants.CODE_BILLBOARD_HOT_MAINSTREAM_ROCK_TRACKS_SINGLES,
      100)

def GetBillboardHotMainstreamRockTracksSingles(request):
  return HttpResponse(models.JsonData.Get(
    Constants.NAME_BILLBOARD_HOT_MAINSTREAM_ROCK_TRACKS_SINGLES))

def UpdateBillboardHotAdultTop40TracksSingles(request):
  return _UpdateBillboardData(
      Constants.NAME_BILLBOARD_HOT_ADULT_TOP_40_TRACKS_SINGLES,
      Constants.CODE_BILLBOARD_HOT_ADULT_TOP_40_TRACKS_SINGLES,
      100)

def GetBillboardHotAdultTop40TracksSingles(request):
  return HttpResponse(models.JsonData.Get(
    Constants.NAME_BILLBOARD_HOT_ADULT_TOP_40_TRACKS_SINGLES))

