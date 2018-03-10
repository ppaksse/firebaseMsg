package firebase.ppaksse.com.firemessenger.models;

import lombok.Data;

@Data
public class TextMessage extends Message{
    private String messageText;

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }
}
