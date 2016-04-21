package reach.project.coreViews.saved_songs;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;

/**
 * Created by gauravsobti on 20/04/16.
 */

//String param[0] is youtube_id and param[1] is type of saved data
public class DeleteSongInDatabaseTask extends AsyncTask<Void, Void, Boolean> {
    private final WeakReference<Context> contextWeakReference;
    private static final String TAG = DeleteSongInDatabaseTask.class.getSimpleName();
    String youtube_id, type;

    public DeleteSongInDatabaseTask(Context context, String youtube_id, String type ) {
        contextWeakReference = new WeakReference<Context>(context);
        this.youtube_id = youtube_id;
        this.type = type;
        //this.context = context;
    }

    @Override
    protected Boolean doInBackground(Void... params) {

        final Context context = contextWeakReference.get();
        if(context == null){
            return false;
        }
        final int rowsDeleted = context.getContentResolver().delete(SavedSongsContract.SavedSongsEntry.CONTENT_URI,
                SavedSongsContract.SavedSongsEntry.YOUTUBE_ID + " LIKE ? ",
                new String[]{youtube_id}
                );

        if(rowsDeleted>0)
            return true;
        else
            return false;
    }

    // successful tell whether the database save operation was successful or not
    @Override
    protected void onPostExecute(Boolean successful) {
        if(successful){
            if(contextWeakReference.get()!=null)
                Toast.makeText(contextWeakReference.get(), "Song removed from " + type , Toast.LENGTH_SHORT).show();
        }
        else{
            if(contextWeakReference.get()!=null)
                Toast.makeText(contextWeakReference.get(), "Song couldn't be removed from " + type, Toast.LENGTH_SHORT).show();
        }
        super.onPostExecute(successful);
    }


}
