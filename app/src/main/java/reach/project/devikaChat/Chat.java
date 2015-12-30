//package reach.project.devikaChat;
//
///**
// * @author dexter
// * @since 6/21/13
// */
//public class Chat {
//
//    public static final int NOT_ADMIN = 0;
//    public static final int ADMIN = 1;
//
//    public static final int PENDING = 0;
//    public static final int SENT_TO_SERVER = 1;
//    public static final int UN_READ = 2;
//    public static final int READ = 3;
//
//    private String chatId = "hello_world";
//    private String message = "hello_world";
//    private long timestamp = 0;
//
//    private int status = PENDING;
//    private int admin = NOT_ADMIN;
//
//    //IMP Required default constructor for Firebase object mapping
//    @SuppressWarnings("unused")
//    public Chat() {
//    }
//
//    public String getMessage() {
//        return message;
//    }
//
//    public void setMessage(String message) {
//        this.message = message;
//    }
//
//    public long getTimestamp() {
//        return timestamp;
//    }
//
//    public void setTimestamp(long timestamp) {
//        this.timestamp = timestamp;
//    }
//
//    public int getAdmin() {
//        return admin;
//    }
//
//    public void setAdmin(int admin) {
//        this.admin = admin;
//    }
//
//    public int getStatus() {
//        return status;
//    }
//
//    public void setStatus(int status) {
//        this.status = status;
//    }
//
//    public String getChatId() {
//        return chatId;
//    }
//
//    public void setChatId(String chatId) {
//        this.chatId = chatId;
//    }
//}