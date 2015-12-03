package reach.project.coreViews.yourProfile.music;// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: proto/smartsong.proto

import com.squareup.wire.Message;
import com.squareup.wire.ProtoField;

import java.util.Collections;
import java.util.List;

import reach.project.music.Song;

import static com.squareup.wire.Message.Datatype.STRING;
import static com.squareup.wire.Message.Label.REPEATED;

public final class SmartSong extends Message {
    private static final long serialVersionUID = 0L;

    public static final String DEFAULT_TITLE = "hello_world";
    public static final List<Song> DEFAULT_SONGLIST = Collections.emptyList();

    @ProtoField(tag = 1, type = STRING)
    public final String title;

    @ProtoField(tag = 2, label = REPEATED, messageType = Song.class)
    public final List<Song> songList;

    public SmartSong(String title, List<Song> songList) {
        this.title = title;
        this.songList = immutableCopyOf(songList);
    }

    private SmartSong(Builder builder) {
        this(builder.title, builder.songList);
        setBuilder(builder);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof SmartSong)) return false;
        SmartSong o = (SmartSong) other;
        return equals(title, o.title)
                && equals(songList, o.songList);
    }

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            result = title != null ? title.hashCode() : 0;
            result = result * 37 + (songList != null ? songList.hashCode() : 1);
            hashCode = result;
        }
        return result;
    }

    public static final class Builder extends Message.Builder<SmartSong> {

        public String title;
        public List<Song> songList;

        public Builder() {
        }

        public Builder(SmartSong message) {
            super(message);
            if (message == null) return;
            this.title = message.title;
            this.songList = copyOf(message.songList);
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder songList(List<Song> songList) {
            this.songList = checkForNulls(songList);
            return this;
        }

        @Override
        public SmartSong build() {
            return new SmartSong(this);
        }
    }
}
