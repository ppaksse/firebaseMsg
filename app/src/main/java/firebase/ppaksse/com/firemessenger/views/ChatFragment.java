package firebase.ppaksse.com.firemessenger.views;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import firebase.ppaksse.com.firemessenger.R;
import firebase.ppaksse.com.firemessenger.adapters.ChatListAdapter;
import firebase.ppaksse.com.firemessenger.customviews.RecyclerViewItemClickListener;
import firebase.ppaksse.com.firemessenger.models.*;

import java.util.Date;
import java.util.Iterator;

public class ChatFragment extends Fragment {

    private FirebaseUser mFirebaseUser;

    private FirebaseDatabase mFirebaseDatase;

    private DatabaseReference mChatRef;
    private DatabaseReference mChatMessageRef;

    private DatabaseReference mChatMemberRef;

    @BindView(R.id.chatRecylerView)
    RecyclerView mChatRecyclerView;

    private ChatListAdapter mChatListAdapter;

    public static String JOINED_ROOM = "";

    public static final int JOIN_ROOM_REQUEST_CODE = 100;

    private Notification mNotification;

    private Context mContext;

    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View chatView = inflater.inflate(R.layout.fragment_chat, container, false);
        ButterKnife.bind(this, chatView);

        // 채팅방 리스너 부착
        // users/{나의 uid}/chats/
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirebaseDatase = FirebaseDatabase.getInstance();
        mChatRef = mFirebaseDatase.getReference("users").child(mFirebaseUser.getUid()).child("chats");
        mChatMemberRef = mFirebaseDatase.getReference("chat_members");
        mChatMessageRef = mFirebaseDatase.getReference("chat_messages");
        mChatListAdapter = new ChatListAdapter();
        mChatListAdapter.setFragment(this);
        mChatRecyclerView.setAdapter(mChatListAdapter);
        mChatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());
        mChatRecyclerView.addOnItemTouchListener( new RecyclerViewItemClickListener(getContext(), new RecyclerViewItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Chat chat = mChatListAdapter.getItem(position);
                Intent chatIntent = new Intent(getActivity(), ChatActivity.class);
                chatIntent.putExtra("chat_id", chat.getChatId());
                startActivityForResult(chatIntent, JOIN_ROOM_REQUEST_CODE);
            }
        }));
        mContext = getActivity();
        mNotification = new Notification(mContext);

        addChatListener();
        return chatView;
    }


    private void addChatListener() {  //채팅방 리스너 부착  (방 목록 수신 및 출력)
        mChatRef.addChildEventListener(new ChildEventListener() {//실시간 변하는 값을 계속 감지하므로 싱글리스너가 아님
            @Override
            public void onChildAdded(final DataSnapshot chatDataSnapshot, String s) {
                // ui 갱신 시켜주는 메서드로 방의 정보를 전달.
                // 방에 대한 정보를 얻어오고
                // 기존의 방제목과 방 멤버의 이름들을 가져와서 타이틀화 시켰을때 같지 않은 경우 방제목을 업데이트 시켜줍니다.
                drawUI(chatDataSnapshot, DrawType.ADD);
            }

            @Override
            public void onChildChanged(final DataSnapshot chatDataSnapshot, String s) {
                // 나의 내가 보낸 메시지가 아닌경우(다른사림이 보냄)와 마지막 메세지가 수정이 되었다면 -> 노티출력

                // 변경된 방의 정보를 수신
                // 변경된 포지션 확인. ( 채팅방 아이디로 기존의 포지션을 확인 합니다)
                // 그 포지션의 아이템중 unreadCount 변경이 되었다면 unreadCount 변경
                // lastMessage 입니다. last 메세지의 시각과 변경된 메세지의 last메세지 시간이 다르다면 -> 노티피케이션을 출력합니다.
                // 현재 액티비티가 ChatActivity 이고 chat_id 가 같다면 노티는 해주지 않습니다.

                // ui 갱신 시켜주는 메서드로 방의 정보를 전달.
                // onChildChanged가 호출되는 조건이 3가지(totalunread의 변경, title의 변경, lastmessage변경시에 호출이 됩니다.)
                // 방에 대한 정보를 얻어오고
                drawUI(chatDataSnapshot, DrawType.UPDATE);
                final Chat updatedChat = chatDataSnapshot.getValue(Chat.class);

                if (updatedChat.getLastMessage() != null ) {
                        if ( updatedChat.getLastMessage().getMessageType() == Message.MessageType.EXIT ) {
                        return;
                    }
                    if ( !updatedChat.getLastMessage().getMessageUser().getUid().equals(mFirebaseUser.getUid())) {//내가 아니어야 하고
                        if ( !updatedChat.getChatId().equals(JOINED_ROOM)) {//현재 방에 들어가 있지 않으면 노티 알림(채팅방에 들어가있으면 노티 안옴)
                            // 노티피케이션 알림
                            Intent chatIntent = new Intent(mContext, ChatActivity.class);
                            chatIntent.putExtra("chat_id", updatedChat.getChatId());
                            mNotification
                                    .setData(chatIntent)
                                    .setTitle(updatedChat.getLastMessage().getMessageUser().getName())
                                    .setText(updatedChat.getLastMessage().getMessageText())
                                    .notification();

                            Bundle bundle = new Bundle();////상대방이 나에게 노티를 보낸다(애널리스틱)
                            bundle.putString("friend", updatedChat.getLastMessage().getMessageUser().getEmail());
                            bundle.putString("me", mFirebaseUser.getEmail());
                            mFirebaseAnalytics.logEvent("notification", bundle);
                        }
                    }
                }

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                //방의 실시간 삭제
                Chat item = dataSnapshot.getValue(Chat.class);
                mChatListAdapter.removeItem(item);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private void drawUI(final DataSnapshot chatDataSnapshot, final DrawType drawType){

        final Chat chatRoom = chatDataSnapshot.getValue(Chat.class);
        mChatMemberRef.child(chatRoom.getChatId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long memberCount = dataSnapshot.getChildrenCount();
                Iterator<DataSnapshot> memberIterator = dataSnapshot.getChildren().iterator();  //넘어온 친구의 정보를 확인(방의 타이틀을 바꿔주기 위해)
                StringBuffer memberStringBuffer = new StringBuffer();

                /**
                 *  <추가 반영분>
                 *  방에 한명밖에 없는 경우 방을 사용하지 못하게 처리 합니다.
                 **/
                if ( memberCount <= 1 ) {
                    chatRoom.setTitle("대화상대가 없는 방입니다.");
                    chatDataSnapshot.getRef().child("title").setValue(chatRoom.getTitle());
                    chatDataSnapshot.getRef().child("disabled").setValue(true);
                    if ( drawType == DrawType.ADD) {
                        mChatListAdapter.addItem(chatRoom);
                    } else {
                        mChatListAdapter.updateItem(chatRoom);
                    }
                    return;
                }

                int loopCount = 1;
                while( memberIterator.hasNext()) {
                    User member = memberIterator.next().getValue(User.class);
                    if ( !mFirebaseUser.getUid().equals(member.getUid())) { //현재 유저와 현제 루프중인 채팅 아이디가 같지 않을때만
                        memberStringBuffer.append(member.getName()); //멤버타이틀에 멤버이름을 적음
                        if ( memberCount - loopCount > 1 ) {//마지막 멤버 다음에 쉼표 오면 안되므로,
                            memberStringBuffer.append(", ");
                        }
                    }
                    if ( loopCount == memberCount ) {
                        // users/uid/chats/{chat_id}/title   현재 chat_id 까지 들어와있는상태
                        String title = memberStringBuffer.toString(); //기존의 타이틀의 정보
                        if ( chatRoom.getTitle() == null ) {
                            chatDataSnapshot.getRef().child("title").setValue(title);
                        } else if (!chatRoom.getTitle().equals(title)){//새로 만들어진 타이틀 정보와 기존의 타이틀의 정보(title) 이 같지않다면
                            chatDataSnapshot.getRef().child("title").setValue(title);
                        }
                        chatRoom.setTitle(title);
                        if ( drawType == DrawType.ADD) {
                            mChatListAdapter.addItem(chatRoom);
                        } else {
                            mChatListAdapter.updateItem(chatRoom);
                        }
                    }
                    loopCount++;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void leaveChat(final Chat chat) {//방나가기
        final DatabaseReference messageRef = mFirebaseDatase.getReference("chat_messages").child(chat.getChatId());//방나갈때 하위항목이 아닌 동등한 병렬적위치의 항목으로 들어가기 하기 위해
        Snackbar.make(getView(), "선택된 대화방을 나가시겠습니까?", Snackbar.LENGTH_LONG).setAction("예", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 나의 대화방 목록에서 제거
                // users/{uid}/chats
                mChatRef.child(chat.getChatId()).removeValue(new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference chatRef) {

                        //exit 메세지 발송
                        // chat_messages > {chat_id} > {message_id} >  { - 내용...~}

                        final ExitMessage exitMessage = new ExitMessage();
                        String messageId = messageRef.push().getKey();

                        exitMessage.setMessageUser(new User(mFirebaseUser.getUid(), mFirebaseUser.getEmail(), mFirebaseUser.getDisplayName(), mFirebaseUser.getPhotoUrl().toString()));
                        exitMessage.setMessageDate(new Date());
                        exitMessage.setMessageId(messageId);
                        exitMessage.setChatId(chat.getChatId());
                        messageRef.child(messageId).setValue(exitMessage);

                        // 채팅 멤버 목록에서 제거
                        // chat_members/{chat_id}/{user_id} 제거
                        mChatMemberRef
                                .child(chat.getChatId())
                                .child(mFirebaseUser.getUid())
                                .removeValue(new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                // 메세지 unreadCount에서도 제거
                                // getReadUserList 내가 있다면 읽어진거니깐 pass
                                // 없다면 unreadCount - 1
                                // messages/{chat_id} 의
                                // 모든 메세지를 가져온다.
                                // 가져와서 루프를 통해서 내가 읽었는지 여부 판단.

                                Bundle bundle = new Bundle();//내가 어떤방에서 나간다(애널리스틱)
                                bundle.putString("me", mFirebaseUser.getEmail());
                                bundle.putString("chatId", chat.getChatId());
                                mFirebaseAnalytics.logEvent("leaveChat", bundle);



                                // 채팅방의 멤버정보를 받아와서
                                // 채팅방의 정보를 가져오고 (각각)
                                // 그정보의 라스트 메세지를 수정해줍니다.

                                mChatMemberRef
                                    .child(chat.getChatId())
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            Iterator<DataSnapshot> chatMemberIterator = dataSnapshot.getChildren().iterator();

                                            while ( chatMemberIterator.hasNext()) {//각 멤버의 방에 나간사람이 있으면 라스트 메세지를 수정해줌
                                                User chatMember = chatMemberIterator.next().getValue(User.class);

                                                // chats -> {uid} -> {chat_id} - { ... }
                                                mFirebaseDatase
                                                        .getReference("users")
                                                        .child(chatMember.getUid())
                                                        .child("chats")
                                                        .child(chat.getChatId())
                                                        .child("lastMessage")
                                                        .setValue(exitMessage);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                  });




                                mFirebaseDatase.getReference("messages").child(chat.getChatId()).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        Iterator<DataSnapshot> messageIterator = dataSnapshot.getChildren().iterator();

                                        while ( messageIterator.hasNext()) {
                                            DataSnapshot messageSnapshot = messageIterator.next();
                                            Message currentMessage = messageSnapshot.getValue(Message.class);
                                            if ( !currentMessage.getReadUserList().contains(mFirebaseUser.getUid())) {// getReadUserList 내가 없다면
                                                // message
                                                messageSnapshot.child("unreadCount").getRef().setValue(currentMessage.getUnreadCount() - 1);//나갔는데도 불구하고, 모두 읽었는데도 1이 떠있는것을 방지하기 위함
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                            }
                        });

                        /**
                         * <추가 반영분>
                         * 대화방의 타이틀이 변경됨을 알려주어 채팅방의 리스너가 감지하게 하여 방 이름을 업데이트 하게 해야합니다
                         * 방의 제목은 방의 업데이트, 추가되었을때의 리스너가 처리하기때문에 방 제목만 변경 시켜서 변경이 되었음만 알리면 됩니다
                         */
                        mChatMemberRef.child(chat.getChatId()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Iterator<DataSnapshot> memberIterator = dataSnapshot.getChildren().iterator();

                                while( memberIterator.hasNext()) {
                                    // 방 참여자의 UID를 가져오기 위하여 user 정보 조회
                                    User chatMember = memberIterator.next().getValue(User.class);
                                    // 해당 참여자의 방 정보의 업데이트를 위하여 방이름을 임의로 업데이트 진행
                                    mFirebaseDatase.getReference("users")
                                            .child(chatMember.getUid())
                                            .child("chats")
                                            .child(chat.getChatId())
                                            .child("title")
                                            .setValue("");
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                });
            }
        }).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ( requestCode == JOIN_ROOM_REQUEST_CODE ) {
            JOINED_ROOM = "";
        }
    }

    private enum DrawType  {
        ADD, UPDATE
    }
}
