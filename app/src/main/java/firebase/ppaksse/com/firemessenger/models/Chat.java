package firebase.ppaksse.com.firemessenger.models;

import lombok.Data;

import java.util.Date;

/**
 */
@Data
public class Chat {

    private String chatId;
    private String title;
    private Date createDate;
    private TextMessage lastMessage;  //방의 마지막 메세지
    private boolean disabled; //방의 만료됬는지 여부
    private int totalUnreadCount; //전체적인 토탈 안읽은 메세지 수

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public TextMessage getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(TextMessage lastMessage) {
        this.lastMessage = lastMessage;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public int getTotalUnreadCount() {
        return totalUnreadCount;
    }

    public void setTotalUnreadCount(int totalUnreadCount) {
        this.totalUnreadCount = totalUnreadCount;
    }
}


