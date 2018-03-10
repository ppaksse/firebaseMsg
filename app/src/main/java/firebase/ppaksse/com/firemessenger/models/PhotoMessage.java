package firebase.ppaksse.com.firemessenger.models;

import lombok.Data;

/**
 */
@Data
public class PhotoMessage extends Message {
    private String photoUrl;

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
