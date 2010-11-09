#!/bin/bash

URLS="
http://music-chart.appspot.com/chart/update_billboard_hot_100_singles
http://music-chart.appspot.com/chart/update_billboard_200_albums
http://music-chart.appspot.com/chart/update_billboard_hot_rnb_hip_hop_songs_singles
http://music-chart.appspot.com/chart/update_billboard_country_songs_singles
http://music-chart.appspot.com/chart/update_billboard_modern_rock_tracks_singles
http://music-chart.appspot.com/chart/update_billboard_dance_club_play_singles
http://music-chart.appspot.com/chart/update_billboard_hot_rap_tracks_singles
http://music-chart.appspot.com/chart/update_billboard_pop_100_singles
http://music-chart.appspot.com/chart/update_billboard_hot_mainstream_rock_tracks_singles
http://music-chart.appspot.com/chart/update_billboard_hot_adult_top_40_tracks_singles
"

for u in $URLS; do
  echo Updating $u
  curl -s $u
  echo
done
