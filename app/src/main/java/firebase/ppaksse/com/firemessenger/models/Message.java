package firebase.ppaksse.com.firemessenger.models;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 */
@Data
public class Message {//메세지에대한 공통적인요소 정의(부모클래스)

    private String messageId;
    private User messageUser;
    private String chatId;
    private int unreadCount; //메세지에 대한 안읽은 메세지 수
    private Date messageDate;
    private MessageType messageType;
    private List<String> readUserList; //읽은사람


    public enum MessageType {  //메세지 타입
        TEXT, PHOTO, EXIT
    }



    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public User getMessageUser() {
        return messageUser;
    }

    public void setMessageUser(User messageUser) {
        this.messageUser = messageUser;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public Date getMessageDate() {
        return messageDate;
    }

    public void setMessageDate(Date messageDate) {
        this.messageDate = messageDate;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public List<String> getReadUserList() {
        return readUserList;
    }

    public void setReadUserList(List<String> readUserList) {
        this.readUserList = readUserList;
    }
}
