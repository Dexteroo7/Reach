package reach.project.utils;// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: Documents/proto/musiclist.proto
import com.squareup.wire.Message;
import com.squareup.wire.ProtoField;

import static com.squareup.wire.Message.Datatype.BOOL;
import static com.squareup.wire.Message.Datatype.INT32;
import static com.squareup.wire.Message.Datatype.INT64;
import static com.squareup.wire.Message.Datatype.STRING;

public final class Song extends Message {
  private static final long serialVersionUID = 0L;

  public static final Long DEFAULT_SONGID = -1L;
  public static final Long DEFAULT_SIZE = 0L;
  public static final Boolean DEFAULT_VISIBILITY = true;
  public static final Integer DEFAULT_YEAR = 0;
  public static final Long DEFAULT_DATEADDED = 0L;
  public static final Long DEFAULT_DURATION = 0L;
  public static final String DEFAULT_GENRE = "hello_world";
  public static final String DEFAULT_DISPLAYNAME = "hello_world";
  public static final String DEFAULT_ACTUALNAME = "hello_world";
  public static final String DEFAULT_ARTIST = "hello_world";
  public static final String DEFAULT_ALBUM = "hello_world";
  public static final String DEFAULT_ALBUMARTURL = "hello_world";
  public static final String DEFAULT_FORMATTEDDATAADDED = "hello_world";
  public static final String DEFAULT_FILEHASH = "hello_world";
  public static final String DEFAULT_PATH = "hello_world";

  @ProtoField(tag = 1, type = INT64)
  public final Long songId;

  @ProtoField(tag = 2, type = INT64)
  public final Long size;

  @ProtoField(tag = 3, type = BOOL)
  public final Boolean visibility;

  @ProtoField(tag = 4, type = INT32)
  public final Integer year;

  @ProtoField(tag = 5, type = INT64)
  public final Long dateAdded;

  @ProtoField(tag = 6, type = INT64)
  public final Long duration;

  @ProtoField(tag = 7, type = STRING)
  public final String genre;

  @ProtoField(tag = 8, type = STRING)
  public final String displayName;

  @ProtoField(tag = 9, type = STRING)
  public final String actualName;

  @ProtoField(tag = 10, type = STRING)
  public final String artist;

  @ProtoField(tag = 11, type = STRING)
  public final String album;

  @ProtoField(tag = 12, type = STRING)
  public final String albumArtUrl;

  @ProtoField(tag = 13, type = STRING)
  public final String formattedDataAdded;

  @ProtoField(tag = 14, type = STRING)
  public final String fileHash;

  @ProtoField(tag = 15, type = STRING)
  public final String path;

  public Song(Long songId, Long size, Boolean visibility, Integer year, Long dateAdded, Long duration, String genre, String displayName, String actualName, String artist, String album, String albumArtUrl, String formattedDataAdded, String fileHash, String path) {
    this.songId = songId;
    this.size = size;
    this.visibility = visibility;
    this.year = year;
    this.dateAdded = dateAdded;
    this.duration = duration;
    this.genre = genre;
    this.displayName = displayName;
    this.actualName = actualName;
    this.artist = artist;
    this.album = album;
    this.albumArtUrl = albumArtUrl;
    this.formattedDataAdded = formattedDataAdded;
    this.fileHash = fileHash;
    this.path = path;
  }

  private Song(Builder builder) {
    this(builder.songId, builder.size, builder.visibility, builder.year, builder.dateAdded, builder.duration, builder.genre, builder.displayName, builder.actualName, builder.artist, builder.album, builder.albumArtUrl, builder.formattedDataAdded, builder.fileHash, builder.path);
    setBuilder(builder);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof Song)) return false;
    Song o = (Song) other;
    return equals(songId, o.songId)
        && equals(size, o.size)
        && equals(visibility, o.visibility)
        && equals(year, o.year)
        && equals(dateAdded, o.dateAdded)
        && equals(duration, o.duration)
        && equals(genre, o.genre)
        && equals(displayName, o.displayName)
        && equals(actualName, o.actualName)
        && equals(artist, o.artist)
        && equals(album, o.album)
        && equals(albumArtUrl, o.albumArtUrl)
        && equals(formattedDataAdded, o.formattedDataAdded)
        && equals(fileHash, o.fileHash)
        && equals(path, o.path);
  }

  @Override
  public int hashCode() {
    int result = hashCode;
    if (result == 0) {
      result = songId != null ? songId.hashCode() : 0;
      result = result * 37 + (size != null ? size.hashCode() : 0);
      result = result * 37 + (visibility != null ? visibility.hashCode() : 0);
      result = result * 37 + (year != null ? year.hashCode() : 0);
      result = result * 37 + (dateAdded != null ? dateAdded.hashCode() : 0);
      result = result * 37 + (duration != null ? duration.hashCode() : 0);
      result = result * 37 + (genre != null ? genre.hashCode() : 0);
      result = result * 37 + (displayName != null ? displayName.hashCode() : 0);
      result = result * 37 + (actualName != null ? actualName.hashCode() : 0);
      result = result * 37 + (artist != null ? artist.hashCode() : 0);
      result = result * 37 + (album != null ? album.hashCode() : 0);
      result = result * 37 + (albumArtUrl != null ? albumArtUrl.hashCode() : 0);
      result = result * 37 + (formattedDataAdded != null ? formattedDataAdded.hashCode() : 0);
      result = result * 37 + (fileHash != null ? fileHash.hashCode() : 0);
      result = result * 37 + (path != null ? path.hashCode() : 0);
      hashCode = result;
    }
    return result;
  }

  public static final class Builder extends Message.Builder<Song> {

    public Long songId;
    public Long size;
    public Boolean visibility;
    public Integer year;
    public Long dateAdded;
    public Long duration;
    public String genre;
    public String displayName;
    public String actualName;
    public String artist;
    public String album;
    public String albumArtUrl;
    public String formattedDataAdded;
    public String fileHash;
    public String path;

    public Builder() {
    }

    public Builder(Song message) {
      super(message);
      if (message == null) return;
      this.songId = message.songId;
      this.size = message.size;
      this.visibility = message.visibility;
      this.year = message.year;
      this.dateAdded = message.dateAdded;
      this.duration = message.duration;
      this.genre = message.genre;
      this.displayName = message.displayName;
      this.actualName = message.actualName;
      this.artist = message.artist;
      this.album = message.album;
      this.albumArtUrl = message.albumArtUrl;
      this.formattedDataAdded = message.formattedDataAdded;
      this.fileHash = message.fileHash;
      this.path = message.path;
    }

    public Builder songId(Long songId) {
      this.songId = songId;
      return this;
    }

    public Builder size(Long size) {
      this.size = size;
      return this;
    }

    public Builder visibility(Boolean visibility) {
      this.visibility = visibility;
      return this;
    }

    public Builder year(Integer year) {
      this.year = year;
      return this;
    }

    public Builder dateAdded(Long dateAdded) {
      this.dateAdded = dateAdded;
      return this;
    }

    public Builder duration(Long duration) {
      this.duration = duration;
      return this;
    }

    public Builder genre(String genre) {
      this.genre = genre;
      return this;
    }

    public Builder displayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder actualName(String actualName) {
      this.actualName = actualName;
      return this;
    }

    public Builder artist(String artist) {
      this.artist = artist;
      return this;
    }

    public Builder album(String album) {
      this.album = album;
      return this;
    }

    public Builder albumArtUrl(String albumArtUrl) {
      this.albumArtUrl = albumArtUrl;
      return this;
    }

    public Builder formattedDataAdded(String formattedDataAdded) {
      this.formattedDataAdded = formattedDataAdded;
      return this;
    }

    public Builder fileHash(String fileHash) {
      this.fileHash = fileHash;
      return this;
    }

    public Builder path(String path) {
      this.path = path;
      return this;
    }

    @Override
    public Song build() {
      return new Song(this);
    }
  }
}
