package reach.backend.transactions;

/**
 * Created by dexter on 05/10/15.
 */
public class Pair {

    final int uploads;
    final int downloads;

    public Pair(int uploads, int downloads) {
        this.uploads = uploads;
        this.downloads = downloads;
    }

    public int getUploads() {
        return uploads;
    }

    public int getDownloads() {
        return downloads;
    }
}
