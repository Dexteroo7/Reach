package reach.project.coreViews.saved_songs;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import java.lang.ref.WeakReference;

/**
 * Created by gauravsobti on 20/04/16.
 */
public class SaveSongInDatabaseTask extends AsyncTask<Void, Void, Boolean> {

    private final SavedSongsDataModel data;
    private final WeakReference<Context> contextWeakReference;
    //Context context;

    public SaveSongInDatabaseTask(Context context, SavedSongsDataModel data) {
        contextWeakReference = new WeakReference<Context>(context);
        //this.context = context;
        this.data = data;
    }

    @Override
    protected Boolean doInBackground(Void... params) {

        final Context context = contextWeakReference.get();
        if(context == null){
            return false;
        }

        final ContentValues cv = getContentValuesData();
        if(cv == null)
            return false;

        context.getContentResolver().insert(SavedSongsContract.SavedSongsEntry.CONTENT_URI,
                cv
        );

        return true;
    }

    // successful tell whether the database save operation was successful or not
    @Override
    protected void onPostExecute(Boolean successful) {
        if(successful){
            if(contextWeakReference.get()!=null)
            Toast.makeText(contextWeakReference.get(), "Song saved", Toast.LENGTH_SHORT).show();
        }
        else{
            if(contextWeakReference.get()!=null)
                Toast.makeText(contextWeakReference.get(), "Song couldn't be saved", Toast.LENGTH_SHORT).show();
        }
        super.onPostExecute(successful);
    }

    private ContentValues getContentValuesData() {
        if(data == null)
            return null;

        final ContentValues cv = new ContentValues();
        cv.put(SavedSongsContract.SavedSongsEntry.ARTIST_ALBUM_NAME,data.getArtist_album_name());
        cv.put(SavedSongsContract.SavedSongsEntry.DATE_ADDED,data.getDate_added());
        cv.put(SavedSongsContract.SavedSongsEntry.SENDER_ID,data.getSender_id());
        cv.put(SavedSongsContract.SavedSongsEntry.DISPLAY_NAME, data.getDisplay_name());
        cv.put(SavedSongsContract.SavedSongsEntry.SENDER_NAME, data.getSender_name());
        cv.put(SavedSongsContract.SavedSongsEntry.SONG_NAME, data.getSong_name() );
        cv.put(SavedSongsContract.SavedSongsEntry.TYPE, data.getType());
        cv.put(SavedSongsContract.SavedSongsEntry.YOUTUBE_ID, data.getYoutube_id() );
        //cv.put();


        return cv;
    }


}
