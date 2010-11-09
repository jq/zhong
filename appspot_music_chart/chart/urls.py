# NOTE: Must import *, since Django looks for things here, e.g. handler500.
from django.conf.urls.defaults import *

urlpatterns = patterns(
    'chart.views',
    (r'^update_billboard_hot_100_singles$', 'UpdateBillboardHot100Singles'),
    (r'^billboard_hot_100_singles$', 'GetBillboardHot100Singles'),
    (r'^update_billboard_200_albums$', 'UpdateBillboard200Albums'),
    (r'^billboard_200_albums$', 'GetBillboard200Albums'),
    (r'^update_billboard_hot_rnb_hip_hop_songs_singles$', 'UpdateBillboardHotRnBHipHopSongsSingles'),
    (r'^billboard_hot_rnb_hip_hop_songs_singles$', 'GetBillboardHotRnBHipHopSongsSingles'),
    (r'^update_billboard_country_songs_singles$', 'UpdateBillboardCountrySongsSingles'),
    (r'^billboard_country_songs_singles$', 'GetBillboardCountrySongsSingles'),
    (r'^update_billboard_modern_rock_tracks_singles$', 'UpdateBillboardModernRockTracksSingles'),
    (r'^billboard_modern_rock_tracks_singles$', 'GetBillboardModernRockTracksSingles'),
    (r'^update_billboard_dance_club_play_singles$', 'UpdateBillboardDanceClubPlaySingles'),
    (r'^billboard_dance_club_play_singles$', 'GetBillboardDanceClubPlaySingles'),
    (r'^update_billboard_hot_rap_tracks_singles$', 'UpdateBillboardHotRapTracksSingles'),
    (r'^billboard_hot_rap_tracks_singles$', 'GetBillboardHotRapTracksSingles'),
    (r'^update_billboard_pop_100_singles$', 'UpdateBillboardPop100Singles'),
    (r'^billboard_pop_100_singles$', 'GetBillboardPop100Singles'),
    (r'^update_billboard_hot_mainstream_rock_tracks_singles$', 'UpdateBillboardHotMainstreamRockTracksSingles'),
    (r'^billboard_hot_mainstream_rock_tracks_singles$', 'GetBillboardHotMainstreamRockTracksSingles'),
    (r'^update_billboard_hot_adult_top_40_tracks_singles$', 'UpdateBillboardHotAdultTop40TracksSingles'),
    (r'^billboard_hot_adult_top_40_tracks_singles$', 'GetBillboardHotAdultTop40TracksSingles'),
    )


