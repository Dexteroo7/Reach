package reach.backend.ObjectWrappers;

import java.util.List;

/**
 * Created by dexter on 22/12/15.
 */
public class PushContainerJSON {

    public String firstContentName;
    public String container;
    public String customMessage;
    public int size;
    public List<Long> receiverId;
    public long senderId;

    PushContainerJSON () {

    }

    public PushContainerJSON(String firstContentName, String container, String customMessage, int size, List<Long> receiverId, long senderId) {
        this.firstContentName = firstContentName;
        this.container = container;
        this.customMessage = customMessage;
        this.size = size;
        this.receiverId = receiverId;
        this.senderId = senderId;
    }

    public String getFirstContentName() {
        return firstContentName;
    }

    public void setFirstContentName(String firstContentName) {
        this.firstContentName = firstContentName;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getCustomMessage() {
        return customMessage;
    }

    public void setCustomMessage(String customMessage) {
        this.customMessage = customMessage;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public List<Long> getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(List<Long> receiverId) {
        this.receiverId = receiverId;
    }

    public long getSenderId() {
        return senderId;
    }

    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }
}
