package reach.project.music;// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: /Users/ashish/Documents/proto/musiclist.proto
import com.squareup.wire.Message;
import com.squareup.wire.ProtoField;
import java.util.Collections;
import java.util.List;

import static com.squareup.wire.Message.Label.REPEATED;

public final class ListOfAlbumArtData extends Message {
  private static final long serialVersionUID = 0L;

  public static final List<AlbumArtData> DEFAULT_ALBUMARTDATA = Collections.emptyList();

  @ProtoField(tag = 1, label = REPEATED, messageType = AlbumArtData.class)
  public final List<AlbumArtData> albumArtData;

  public ListOfAlbumArtData(List<AlbumArtData> albumArtData) {
    this.albumArtData = immutableCopyOf(albumArtData);
  }

  private ListOfAlbumArtData(Builder builder) {
    this(builder.albumArtData);
    setBuilder(builder);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof ListOfAlbumArtData)) return false;
    return equals(albumArtData, ((ListOfAlbumArtData) other).albumArtData);
  }

  @Override
  public int hashCode() {
    int result = hashCode;
    return result != 0 ? result : (hashCode = albumArtData != null ? albumArtData.hashCode() : 1);
  }

  public static final class Builder extends Message.Builder<ListOfAlbumArtData> {

    public List<AlbumArtData> albumArtData;

    public Builder() {
    }

    public Builder(ListOfAlbumArtData message) {
      super(message);
      if (message == null) return;
      this.albumArtData = copyOf(message.albumArtData);
    }

    public Builder albumArtData(List<AlbumArtData> albumArtData) {
      this.albumArtData = checkForNulls(albumArtData);
      return this;
    }

    @Override
    public ListOfAlbumArtData build() {
      return new ListOfAlbumArtData(this);
    }
  }
}