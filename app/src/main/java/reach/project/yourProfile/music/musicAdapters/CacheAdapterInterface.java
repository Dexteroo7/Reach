package reach.project.yourProfile.music.musicAdapters;

import com.squareup.wire.Message;

import reach.project.music.Song;

/**
 * Created by dexter on 18/11/15.
 */
public interface CacheAdapterInterface<T extends Message> {

    int getItemCount();

    long getItemId(T item);

    T getItem(int position);

    void handOverSongClick(Song song);
}