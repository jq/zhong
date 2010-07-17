
grep -rI vnd.android *

AndroidManifest.xml:                <data android:mimeType="vnd.android.cursor.dir/artistalbum"/>
AndroidManifest.xml:                <data android:mimeType="vnd.android.cursor.dir/album"/>
AndroidManifest.xml:                <data android:mimeType="vnd.android.cursor.dir/nowplaying"/>
AndroidManifest.xml:                <data android:mimeType="vnd.android.cursor.dir/track"/>
AndroidManifest.xml:                <data android:mimeType="vnd.android.cursor.dir/playlist"/>
AndroidManifest.xml:                <data android:mimeType="vnd.android.cursor.dir/playlist"/>
AndroidManifest.xml:                <data android:mimeType="vnd.android.cursor.dir/video"/>
AndroidManifest.xml:                <data android:mimeType="vnd.android.cursor.dir/audio"/>
src/com/droidcool/music/AlbumBrowserActivity.java:        intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
src/com/droidcool/music/ArtistAlbumBrowserActivity.java:        intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
src/com/droidcool/music/MediaPlaybackActivity.java:                    .setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track")
src/com/droidcool/music/MusicUtils.java:                intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/artistalbum");
src/com/droidcool/music/MusicUtils.java:                intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/album");
src/com/droidcool/music/MusicUtils.java:                intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
src/com/droidcool/music/PlaylistBrowserActivity.java:            shortcut.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/playlist");
src/com/droidcool/music/PlaylistBrowserActivity.java:            intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
src/com/droidcool/music/PlaylistBrowserActivity.java:            intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
src/com/droidcool/music/PlaylistBrowserActivity.java:            intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
src/com/droidcool/music/QueryBrowserActivity.java:                i.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
src/com/droidcool/music/QueryBrowserActivity.java:                i.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/album");
src/com/droidcool/music/QueryBrowserActivity.java:            intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/album");
src/com/droidcool/music/QueryBrowserActivity.java:            intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");

find . -name "*.xml" -o -name "*.java" | xargs perl -pi -e 's/vnd.android/vnd.droidcool/g'
