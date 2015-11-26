package reach.backend.ActiveCountLog;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.OnSave;

/**
 * Created by dexter on 21/10/15.
 */

@Entity
@Index
public class ActiveCountLog {

    @Id
    private Long id;

    private int currentActive = 0;
    private int currentTotal = 0;
    private long currentTime = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getCurrentActive() {
        return currentActive;
    }

    public void setCurrentActive(int currentActive) {
        this.currentActive = currentActive;
    }

    public int getCurrentTotal() {
        return currentTotal;
    }

    public void setCurrentTotal(int currentTotal) {
        this.currentTotal = currentTotal;
    }

    @OnSave
    void onSave() {
        this.currentTime = System.currentTimeMillis();
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }
}
