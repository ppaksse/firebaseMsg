package firebase.ppaksse.com.firemessenger.views;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.*;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.*;
import firebase.ppaksse.com.firemessenger.R;
import firebase.ppaksse.com.firemessenger.models.User;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    private View mProgressView;

    private SignInButton mSignInbtn;

    private GoogleApiClient mGoogleAPIClient;

    private GoogleSignInOptions mGoogleSignInoptions;

    private FirebaseAuth mAuth;

    private static int GOOGLE_LOGIN_OPEN = 100;


    private FirebaseAnalytics mFirebaseAnalytics; //이벤트 기록등 처리를 위해

    private FirebaseDatabase mDatabase;

    private DatabaseReference mUserRef;

    static {
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);//안드로이드 오프라인 기능 설정(첫번째 엑티비티에 써줌)
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mProgressView = (ProgressBar) findViewById(R.id.login_progress);
        mSignInbtn = (SignInButton) findViewById(R.id.google_sign_in_btn);
        mAuth = FirebaseAuth.getInstance();
        if ( mAuth.getCurrentUser() != null ) {//자동로그인 기능 추가
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }
        mDatabase = FirebaseDatabase.getInstance();
        mUserRef = mDatabase.getReference("users");//데이터베이스의 users 하위항목에 저장한다는 의미

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        //구글 사인인옵션, idtoken email등을 로그인할때 요청하는것 설정
        GoogleSignInOptions mGoogleSignInoptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // [END config_signin]

        mGoogleAPIClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        // 실패 시 처리 하는 부분.
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, mGoogleSignInoptions)
                .build();

        mSignInbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });
    }

    private void signIn() { //구글로그인창이 보일수 있는것은(계정을 선택할수있는창) 이부분 때문에
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleAPIClient);
        startActivityForResult(signInIntent, GOOGLE_LOGIN_OPEN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {//계정선택 뒤 다음화면 넘어왔을 떄
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GOOGLE_LOGIN_OPEN) {//요청코드가 같다면 데이터에서 사인인 result를 가져와서 성공하면, 실제로 파이어베이스로그인 위해서 acoout정보가져옴
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                // Google Sign In failed, update UI appropriately
                // ...
            }
        }
    }


    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {//구글프로바이더의 자격증명을 통해서 파이어베이스에 로그인함

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Snackbar.make(mProgressView, "call onFailuer", Snackbar.LENGTH_LONG).show();
                mDatabase.getReference("error/").setValue(e);
                mDatabase.getReference("error/message").setValue(e.getMessage());
                FirebaseCrash.report(e);
            }
        })
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {//로그인이 끝나면, onComplete 절이 호출되는것

                if (task.isComplete()) {//컴플릿은 성공과 실패를 포함
                    if (task.isSuccessful()) {//성공하면 friebaseUser를 가져옴
                        FirebaseUser firebaseUser = task.getResult().getUser();

                        final User user = new User();
                        user.setEmail(firebaseUser.getEmail());
                        user.setName(firebaseUser.getDisplayName());
                        user.setUid(firebaseUser.getUid());
                        if ( firebaseUser.getPhotoUrl() != null )
                            user.setProfileUrl(firebaseUser.getPhotoUrl().toString());

                        mUserRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {//users 하위의 userId에 들어가므로 child
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if ( !dataSnapshot.exists() ) {//데이터가 존재하지 않으면(친구목록이 없으면)
                                    mUserRef.child(user.getUid()).setValue(user, new DatabaseReference.CompletionListener() {//구글에서 데이터를 가져와야함
                                        @Override
                                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                                            if ( databaseError == null ) {//에러가 없는경우만 진행
                                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                                finish();
                                            }
                                        }
                                    });
                                } else {//데이터가 존재하면(기존의 대화방,친구목록등) 바로 메인띄움(기존의 데이터가 손상이 되지 한도록)
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                }

                                Bundle eventBundle = new Bundle();
                                eventBundle.putString("email", user.getEmail());
                                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, eventBundle); //로그인 이벤트를 기록하는 것임
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                    } else {
                        Snackbar.make(mProgressView, "로그인에 실패하였습니다.", Snackbar.LENGTH_LONG).show();
                    }
                } else {
                    Snackbar.make(mProgressView, "로그인에 실패하였습니다.", Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }
}

