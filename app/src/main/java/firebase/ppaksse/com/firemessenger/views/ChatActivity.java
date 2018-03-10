package firebase.ppaksse.com.firemessenger.views;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import firebase.ppaksse.com.firemessenger.R;
import firebase.ppaksse.com.firemessenger.adapters.MessageListAdapter;
import firebase.ppaksse.com.firemessenger.models.*;

import java.util.*;

public class ChatActivity extends AppCompatActivity {

    private String mChatId;

    @BindView(R.id.senderBtn)
    ImageView mSenderButton;

    @BindView(R.id.edtContent)
    EditText mMessageText;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.chat_rec_view)
    RecyclerView mChatRecyclerView;

    private MessageListAdapter messageListAdapter;
    private FirebaseDatabase mFirebaseDb;
    private DatabaseReference mChatRef;
    private DatabaseReference mChatMemeberRef;
    private DatabaseReference mChatMessageRef;
    private DatabaseReference mUserRef;
    private FirebaseUser mFirebaseUser;
    private static final int TAKE_PHOTO_REQUEST_CODE = 201;
    private StorageReference mImageStorageRef;
    private FirebaseAnalytics mFirebaseAnalytics;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_chat);
        ButterKnife.bind(this);
        mChatId = getIntent().getStringExtra("chat_id");
        mFirebaseDb = FirebaseDatabase.getInstance();
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mUserRef = mFirebaseDb.getReference("users");
        mToolbar.setTitleTextColor(Color.WHITE);
        if (mChatId != null) {//기존 채팅방이라면 바로 밑에 mChatId 가 있고 initTotatlunreadCount 호출함
            mChatRef = mFirebaseDb.getReference("users").child(mFirebaseUser.getUid()).child("chats").child(mChatId);
            mChatMessageRef = mFirebaseDb.getReference("chat_messages").child(mChatId);
            mChatMemeberRef = mFirebaseDb.getReference("chat_members").child(mChatId);
            ChatFragment.JOINED_ROOM = mChatId;
            initTotalunreadCount();
        } else {
            mChatRef = mFirebaseDb.getReference("users").child(mFirebaseUser.getUid()).child("chats");
        }
        messageListAdapter = new MessageListAdapter();
        mChatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mChatRecyclerView.setAdapter(messageListAdapter);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
    }

    @Override
    protected void onPause() {//액티비티가 비활성화 됐을 때
        super.onPause();
        if (mChatId != null) {
            removeMessageListener();
        }
    }

    @Override
    protected void onResume() {//액티비티가 재개 되어을 때
        super.onResume();
        if (mChatId != null) {

            // 총 메세지의 카운터를 가져온다.(메세제 포커스  처리)
            // onchildadded 호출한 변수의 값이 총메세지의 값과 크거나 같다면, 포커스를 맨아래로 내려줍니다.
            mChatMessageRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    long totalMessageCount = dataSnapshot.getChildrenCount();
                    mMessageEventListener.setTotalMessageCount(totalMessageCount);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            messageListAdapter.clearItem();
            addChatListener();//사용자가 나가거나 했을때 바로 방 이름이 바뀔수 있도록..
            addMessageListener();
        }
    }

    private void initTotalunreadCount() {//채팅 메세지 수신시 토탈언리드카운드는 0
        mChatRef.child("totalUnreadCount").setValue(0);
    }

    MessageEventListener mMessageEventListener = new MessageEventListener();

    private void addChatListener() {//채팅창 툴바에 타이틀 입력
        mChatRef.child("title").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String title = dataSnapshot.getValue(String.class);
                if (title != null) {//타이틀이 널이아닐때
                    mToolbar.setTitle(title);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private void addMessageListener() {
        mChatMessageRef.addChildEventListener(mMessageEventListener);
    }

    private void removeMessageListener() {
        mChatMessageRef.removeEventListener(mMessageEventListener);
    }


    private class MessageEventListener implements ChildEventListener {

        private long totalMessageCount;

        private long callCount = 1;

        public void setTotalMessageCount(long totalMessageCount) {
            this.totalMessageCount = totalMessageCount;
        }

        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {

            // 신규메세지
            Message item = dataSnapshot.getValue(Message.class);

            // 읽음 처리
            // chat_messages > {chat_id} > {message_id} > readUserList
            // 내가 존재 하는지를 확인
            // 존재한다면 아무처리 하지 않음

            // 존재 하지 않는다면
            // chat_messages > {chat_id} > {message_id} >  unreadCount -= 1
            // readUserList에 내 uid 추가

            List<String> readUserUIDList = item.getReadUserList();
            if (readUserUIDList != null) {
                if (!readUserUIDList.contains(mFirebaseUser.getUid())) { //존재하지 않는다면
                    // chat_messages > {chat_id} > {message_id} >  unreadCount -= 1

                    // messageRef.setValue();
                    dataSnapshot.getRef().runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(MutableData mutableData) {
                            Message mutableMessage = mutableData.getValue(Message.class);
                            // readUserList에 내 uid 추가
                            // unreadCount -= 1

                            List<String> mutabledReadUserList = mutableMessage.getReadUserList();
                            mutabledReadUserList.add(mFirebaseUser.getUid());
                            int mutableUnreadCount = mutableMessage.getUnreadCount() - 1;

                            if (mutableMessage.getMessageType() == Message.MessageType.PHOTO) {
                                PhotoMessage mutablePhotoMessage = mutableData.getValue(PhotoMessage.class);
                                mutablePhotoMessage.setReadUserList(mutabledReadUserList);
                                mutablePhotoMessage.setUnreadCount(mutableUnreadCount);
                                mutableData.setValue(mutablePhotoMessage);
                            } else {
                                TextMessage mutableTextMessage = mutableData.getValue(TextMessage.class);
                                mutableTextMessage.setReadUserList(mutabledReadUserList);
                                mutableTextMessage.setUnreadCount(mutableUnreadCount);
                                mutableData.setValue(mutableTextMessage);
                            }
                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                            //0.5 초 정도 후에 언리드카운트의 값을 초기화.
                            // Timer // TimeTask
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    initTotalunreadCount();
                                }
                            }, 500);
                        }
                    });
                }
            }

            // ui
            if (item.getMessageType() == Message.MessageType.TEXT) {
                TextMessage textMessage = dataSnapshot.getValue(TextMessage.class);
                messageListAdapter.addItem(textMessage);
            } else if (item.getMessageType() == Message.MessageType.PHOTO) {
                PhotoMessage photoMessage = dataSnapshot.getValue(PhotoMessage.class);
                messageListAdapter.addItem(photoMessage);
            } else if (item.getMessageType() == Message.MessageType.EXIT) {
                messageListAdapter.addItem(item); //여기서 item은 메세지를 말함
            }

            if (callCount >= totalMessageCount) {
                // 스크롤을 맨 마지막으로 내린다.
                mChatRecyclerView.scrollToPosition(messageListAdapter.getItemCount() - 1);
            }
            callCount++;
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            // 변경된 메세지 ( unreadCount)
            // 아답터쪽에 변경된 메세지데이터를 전달하고
            // 메시지 아이디 번호로 해당 메세지의 위치를 알아내서
            // 알아낸 위치값을 이용해서 메세지 리스트의 값을 변경할 예정입니다.
            Message item = dataSnapshot.getValue(Message.class);

            if (item.getMessageType() == Message.MessageType.TEXT) {
                TextMessage textMessage = dataSnapshot.getValue(TextMessage.class);
                messageListAdapter.updateItem(textMessage);
            } else if (item.getMessageType() == Message.MessageType.PHOTO) {
                PhotoMessage photoMessage = dataSnapshot.getValue(PhotoMessage.class);
                messageListAdapter.updateItem(photoMessage);
            }
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    }


    @OnClick(R.id.senderBtn)
    public void onSendEvent(View v) {

        if (mChatId != null) {//기존 방이라면
            sendMessage();
        } else {  //새로운 방을 만들어야 한다면
            createChat();
        }
    }

    @OnClick(R.id.photoSend)
    public void onPhotoSendEvent(View v) {
        // 안드로이드 파일창 오픈 (갤러리 오픈)
        // requestcode = 201
        //TAKE_PHOTO_REQUEST_CODE

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, TAKE_PHOTO_REQUEST_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TAKE_PHOTO_REQUEST_CODE) {
            if (data != null) {

                // 업로드 이미지를 처리 합니다.
                // 이미지 업로드가 완료된 경우
                // 실제 web 에 업로드 된 주소를 받아서 photoUrl로 저장
                // 그다음 포토메세지 발송
                uploadImage(data.getData());

            }
        }
    }

    private String mPhotoUrl = null;
    private Message.MessageType mMessageType = Message.MessageType.TEXT;

    private void uploadImage(Uri data) {
        if (mImageStorageRef == null) {
            mImageStorageRef = FirebaseStorage.getInstance().getReference("/chats/").child(mChatId);
        }
        mImageStorageRef.putFile(data).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    mPhotoUrl = task.getResult().getDownloadUrl().toString();
                    mMessageType = Message.MessageType.PHOTO; //최초에는 설정을 text 로 함, 그뒤 텍스트 였던것을 photo로 바꿔줌
                    sendMessage();
                }
            }
        });
        //firebase Storage
    }


    private Message message = new Message();

    private void sendMessage() {
        // 메세지 키 생성
        mChatMessageRef = mFirebaseDb.getReference("chat_messages").child(mChatId);
        // chat_message>{chat_id}>{message_id} > messageInfo
        String messageId = mChatMessageRef.push().getKey();

        String messageText = mMessageText.getText().toString();

        final Bundle bundle = new Bundle(); //내가 어떤 방에 메세지를 보낸다(애널리스틱)
        bundle.putString("me", mFirebaseUser.getEmail());
        bundle.putString("roomId", mChatId);

        if (mMessageType == Message.MessageType.TEXT) {
            if (messageText.isEmpty()) {//메세지창 비어있으면 안무것도 안쓰면, 가만히 있음(전송안됨)
                return;
            }
            message = new TextMessage();
            ((TextMessage) message).setMessageText(messageText.trim());
            bundle.putString("messageType", Message.MessageType.TEXT.toString());//내가 어떤 방에 메세지를 보낸다(애널리스틱)
        } else if (mMessageType == Message.MessageType.PHOTO) {
            message = new PhotoMessage();
            ((PhotoMessage) message).setPhotoUrl(mPhotoUrl);
            bundle.putString("messageType", Message.MessageType.PHOTO.toString());//내가 어떤 방에 메세지를 보낸다(애널리스틱)
        }

        message.setMessageDate(new Date());
        message.setChatId(mChatId);
        message.setMessageId(messageId);
        message.setMessageType(mMessageType);
        message.setMessageUser(new User(mFirebaseUser.getUid(), mFirebaseUser.getEmail(), mFirebaseUser.getDisplayName(), mFirebaseUser.getPhotoUrl().toString()));
        message.setReadUserList(Arrays.asList(new String[]{mFirebaseUser.getUid()})); //나는 읽었다는 것을 나타냄

        String[] uids = getIntent().getStringArrayExtra("uids");
        if (uids != null) {
            message.setUnreadCount(uids.length - 1); //나는읽었다는 것을 알수 있음
        }
        mMessageText.setText("");//메세지가 나갈거니까(전송) 미리 공백으로 비워줌
        mMessageType = Message.MessageType.TEXT; //메세지 전송후에는 메세지 타입을 기본설정값인 다시 TEXT로 바꿔줌
        mChatMemeberRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                //unreadCount 셋팅하기 위한 대화 상대의 수를 가져 옵니다. mChatMembers의 밑에 userId 있으므로 getchildren으로 가져옴
                long memberCount = dataSnapshot.getChildrenCount();
                message.setUnreadCount((int) memberCount - 1);
                mChatMessageRef.child(message.getMessageId()).setValue(message, new DatabaseReference.CompletionListener() { //메세지 저장
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                        mFirebaseAnalytics.logEvent("sendMessage", bundle);//내가 어떤 방에 메세지를 보낸다(애널리스틱)
                        Iterator<DataSnapshot> memberIterator = dataSnapshot.getChildren().iterator();
                        while (memberIterator.hasNext()) { //다음커서가 존재한다면
                            User chatMember = memberIterator.next().getValue(User.class); //유저의 정보를 꺼내와서
                            mUserRef
                                    .child(chatMember.getUid())
                                    .child("chats")
                                    .child(mChatId)
                                    .child("lastMessage")
                                    .setValue(message); //라스트메세지가 메세지로 대체됨(users 하위 chatid 밑에 lastMessage 항목 만드록 거기에 message넣음)

                            if (!chatMember.getUid().equals(mFirebaseUser.getUid())) {//루프돌아가는 유저가 내가 아닌경우라면
                                // 공유되는 증가카운트의 경우 transaction을 이용하여 처리합니다.
                                mUserRef
                                        .child(chatMember.getUid())
                                        .child("chats")
                                        .child(mChatId)
                                        .child("totalUnreadCount")
                                        .runTransaction(new Transaction.Handler() {
                                            @Override
                                            public Transaction.Result doTransaction(MutableData mutableData) {
                                                long totalUnreadCount = mutableData.getValue(long.class) == null ? 0 : mutableData.getValue(long.class);
                                                mutableData.setValue(totalUnreadCount + 1);
                                                return Transaction.success(mutableData);
                                            }

                                            @Override
                                            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                                            }
                                        });
                            }
                        }
                    }
                });


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private boolean isSentMessage = false;

    private void createChat() {
        // <방생성>

        // 0. 방 정보 설정 <-- 기존 방이어야 가능함.

        // 1. 대화 상대에 내가 선택한 사람 추가

        // 2. 각 상대별 chats에 방추가

        // 3. 메세지 정보 중 읽은 사람에 내 정보를 추가

        // 4. 4.  첫 메세지 전송

        final Chat chat = new Chat();

        mChatId = mChatRef.push().getKey();
        mChatRef = mChatRef.child(mChatId);
        mChatMemeberRef = mFirebaseDb.getReference("chat_members").child(mChatId);
        chat.setChatId(mChatId);
        chat.setCreateDate(new Date());
        String uid = getIntent().getStringExtra("uid");   //싱클채팅
        String[] uids = getIntent().getStringArrayExtra("uids");  //멀티채팅
        if (uid != null) {
            // 1:1 방임
            uids = new String[]{uid};
        }

        List<String> uidList = new ArrayList<>(Arrays.asList(uids));
        uidList.add(mFirebaseUser.getUid());

        for (String userId : uidList) { //방생성과 채팅멤버 생성은 유저리스트로...또한 for문이므로 메세지도 방생성과 채팅멤버생성 만큼 3명이면(3번보내지므로) 아래처럼 if문으로
            // uid > userInfo
            mUserRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(final DataSnapshot dataSnapshot) {
                    User member = dataSnapshot.getValue(User.class); //파이어베이스db에서 넘어온정보를 User형으로 받아옴

                    mChatMemeberRef.child(member.getUid()) // mchatmembers에 userID는 줌으로써
                            .setValue(member, new DatabaseReference.CompletionListener() {//데이터가 성공적으로 저장이 되었을 때, 방정보를 설정
                                @Override
                                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                    // USERS>uid>chats>{chat_id}>chatinfo (방정보를 설정)
                                    dataSnapshot.getRef().child("chats").child(mChatId).setValue(chat); //getRef 가지가 uid 까지를 말함
                                    if (!isSentMessage) { //메세지가 보내지지않은경우(false) 인경우만 한번메세지 보내고, 한번보내면 못보내게 true로 해줌
                                        sendMessage();
                                        addChatListener();
                                        addMessageListener();
                                        isSentMessage = true;

                                        Bundle bundle = new Bundle();//파이어베이스에 애널리스특 등록(내가 이 방을 생성했다)
                                        bundle.putString("me", mFirebaseUser.getEmail());
                                        bundle.putString("roomId", mChatId);
                                        mFirebaseAnalytics.logEvent("createChat", bundle);
                                        ChatFragment.JOINED_ROOM = mChatId;//채팅방 생성시 joined_room에 방 아이디 설정(채팅방에 들어가있으면 노티 안옴)

                                    }

                                }
                            });

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        // users > {uid} > chats > {chat_uid}
    }

}

