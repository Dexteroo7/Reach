package reach.backend.user;

import java.io.IOException;
import java.util.HashSet;

/**
 * Created by Dexter on 25-03-2015.
 */
public class ContactsWrapper {

    private HashSet<String> contacts;
    private byte[] compressedData;
    private boolean isCompressed = false;

    public HashSet<String> getUnCompressedData() throws IOException {
        return null;
    }

    public byte[] getCompressedData() {
        return compressedData;
    }

    public void setCompressedData(byte[] compressedData) {
        this.compressedData = compressedData;
    }

    public boolean isCompressed() {
        return isCompressed;
    }

    public void setCompressed(boolean isCompressed) {
        this.isCompressed = isCompressed;
    }

    public HashSet<String> getContacts() {
        return contacts;
    }

    public void setContacts(HashSet<String> contacts) {
        this.contacts = contacts;
    }
}
