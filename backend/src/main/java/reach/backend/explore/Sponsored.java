package reach.backend.explore;

import com.google.appengine.api.datastore.Blob;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.Date;

/**
 * Created by dexter on 21/01/16.
 */

@Entity
public class Sponsored {

    @Id
    Long id = 0L;
    Blob sponsoredContent;
    Date when;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Blob getSponsoredContent() {
        return sponsoredContent;
    }

    public void setSponsoredContent(Blob sponsoredContent) {
        this.sponsoredContent = sponsoredContent;
    }

    public Date getWhen() {
        return when;
    }

    public void setWhen(Date when) {
        this.when = when;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sponsored)) return false;

        Sponsored sponsored = (Sponsored) o;

        return !(id != null ? !id.equals(sponsored.id) : sponsored.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Sponsored{" +
                "id=" + id +
                ", sponsoredContent=" + sponsoredContent +
                ", when=" + when +
                '}';
    }
}
