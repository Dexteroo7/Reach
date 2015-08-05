package reach.project.viewHelpers;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.ContactsContract;

/**
 * Created by Dexter on 03-03-2015.
 */
public class Contact {

    private final String userName,phoneNumber;
    private final long userID;
    private final Uri photoUri;

    private boolean inviteSent = false;

    public Contact(String userName, String phoneNumber, long userID) {
        this.userName = userName;
        this.phoneNumber = phoneNumber;
        this.userID = userID;
        this.photoUri =  Uri.withAppendedPath(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, userID),
                ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
    }

    public boolean isInviteSent() {
        return inviteSent;
    }

    public void setInviteSent(boolean inviteSent) {
        this.inviteSent = inviteSent;
    }

    public String getUserName() {
        return userName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public long getUserID() {
        return userID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Contact contact = (Contact) o;
        return phoneNumber.equals(contact.phoneNumber) && userName.equals(contact.userName);

    }

    @Override
    public int hashCode() {
        int result = userName.hashCode();
        result = 31 * result + phoneNumber.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Contact{" +
                "userName='" + userName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                '}';
    }

    public Uri getPhotoUri() {
        return photoUri;
    }
}