package firebase.ppaksse.com.firemessenger.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.bumptech.glide.Glide;
import firebase.ppaksse.com.firemessenger.R;
import firebase.ppaksse.com.firemessenger.customviews.RoundedImageView;
import firebase.ppaksse.com.firemessenger.models.User;

import java.util.ArrayList;

public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.FriendHolder> {


    public static final int UNSELECTION_MODE = 1; //친구 한명 선택할때 모드
    public static final int SELECTION_MODE = 2;  //친구 여러명 선택할때 모드

    private int selectionMode = UNSELECTION_MODE;  //셀렉션모드 최초의 값은 친구한명선택모드로 놓음

    private ArrayList<User> friendList;

    public FriendListAdapter(){
        friendList = new ArrayList<>();
    }

    public void addItem(User friend) { //리얼타임데이터베이스에 onchild가 added 되었을때 리스트어답터에 아이템을 추가해야하므로
        friendList.add(friend);
        notifyDataSetChanged();
    }

    public void setSelectionMode(int selectionMode) {
        this.selectionMode = selectionMode;
        notifyDataSetChanged();
    }

    public int getSelectionMode() {
        return this.selectionMode;
    }

    public int getSelectionUsersCount() {
        int selectedCount = 0;
        for ( User user : friendList) {
            if ( user.isSelection() ) {
                selectedCount++;
            }
        }
        return selectedCount;
    }

    public String [] getSelectedUids() { //선택된 유저의 정보를 리턴할수 있는 함수
        String [] selecteUids = new String[getSelectionUsersCount()];
        int i = 0;
        for ( User user : friendList) {
            if ( user.isSelection() ) {
                selecteUids[i++] = user.getUid();
            }
        }
        return selecteUids;
    }

    public User getItem(int position) {
        return this.friendList.get(position);
    }

    @Override
    public FriendHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_friend_item, parent, false);
        FriendHolder friendHolder = new FriendHolder(view);
        return friendHolder;
    }

    @Override
    public void onBindViewHolder(FriendHolder holder, int position) {
        User friend = getItem(position);
        holder.mEmailView.setText(friend.getEmail());
        holder.mNameView.setText(friend.getName());
        if ( getSelectionMode() == UNSELECTION_MODE ) { //언슬랙션모드 이면 체크박스 없는걸로
            holder.friendSelectedView.setVisibility(View.GONE);
        } else {        //그렇치 안흥면 체크박스 보이도록
            holder.friendSelectedView.setVisibility(View.VISIBLE);
        }

        if ( friend.getProfileUrl() != null ) {
            Glide.with(holder.itemView)
                .load(friend.getProfileUrl())
                .into(holder.mProfileView);
        }
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    public static class FriendHolder extends RecyclerView.ViewHolder {//뷰홀더란 친구목록의 한개의 아이템을 말함

        @BindView(R.id.checkbox)
        CheckBox friendSelectedView;

        @BindView(R.id.thumb)   //프로필사진
        RoundedImageView mProfileView;

        @BindView(R.id.name) //이름
        TextView mNameView;

        @BindView(R.id.email)  //에메일로 구성됨(fragment_friend_item)
        TextView mEmailView;

        private FriendHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);
        }
    }
}
