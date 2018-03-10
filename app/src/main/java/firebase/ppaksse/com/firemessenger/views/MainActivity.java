package firebase.ppaksse.com.firemessenger.views;

import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import butterknife.BindView;
import butterknife.ButterKnife;
import firebase.ppaksse.com.firemessenger.R;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tabs)
    TabLayout mTabLayout;

    @BindView(R.id.fab)
    FloatingActionButton mFab;

    @BindView(R.id.viewpager)
    ViewPager mViewPager;

    ViewPagerAdapter mPageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this); //BindView를 쉽게 이걸함으로써 간단히 가져옴

        mTabLayout.setupWithViewPager(mViewPager);//탭 레이아웃에 뷰페이저를 연결해줌(붙임)
        setUpViewPager();

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment currentFragment = mPageAdapter.getItem(mViewPager.getCurrentItem());
                if (currentFragment instanceof FriendFragment) { //현재프래그먼트가 친구프래그먼트이면 친구프래그먼트의 토글서치바 호출
                    ((FriendFragment) currentFragment).toggleSearchBar();
                } else {
                    // 그렇치 않으면(현재 채팅 프래그먼트이면) 친구 탭으로 이동
                    mViewPager.setCurrentItem(2, true);
                    // 체크박스가 보일수 있도록 처리
                    FriendFragment friendFragment = (FriendFragment) mPageAdapter.getItem(1); //포지션은 0이 채팅, 1인 프랜드프래그먼트임
                    friendFragment.toggleSelectionMode();

                }
            }
        });
    }


    private void setUpViewPager(){
        mPageAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        mPageAdapter.addFragment(new ChatFragment(), "채팅");
        mPageAdapter.addFragment(new FriendFragment(), "친구");
        mViewPager.setAdapter(mPageAdapter);
    }


    private class ViewPagerAdapter extends FragmentPagerAdapter {

        private List<Fragment> fragmentList = new ArrayList<>(); //뷰페이저안에 들어올 프래그먼트를 담을 리스트
        private List<String> fragmentTitleList = new ArrayList<>();//뷰페이저안에 들어올 프래그먼트 제목을 담을 리스트

        public ViewPagerAdapter(FragmentManager fragmentManager){
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        } //poition값 0은 채팅 1은 침구리스트

        @Override
        public CharSequence getPageTitle(int position) {//타이틀 제목을 출력
            return fragmentTitleList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            fragmentList.add(fragment);
            fragmentTitleList.add(title);
        }

    }

}
