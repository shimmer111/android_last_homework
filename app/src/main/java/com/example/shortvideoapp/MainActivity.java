package com.example.shortvideoapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.shortvideoapp.bean.Feed;
import com.example.shortvideoapp.bean.FeedResponse;
import com.example.shortvideoapp.bean.PostVideoResponse;
import com.example.shortvideoapp.network.IMiniDouyinService;
import com.example.shortvideoapp.utils.ResourceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Multipart;

public class MainActivity extends AppCompatActivity {
    private static final String TAG ="MainActivity";
    //show
    private RecyclerView mRv;
    private List<Feed> mFeeds = new ArrayList<>();
    //post
    private static final int PICK_IMAGE = 1;
    private static final int PICK_VIDEO = 2;
    public Uri mSelectedImage;
    private Uri mSelectedVideo;
    public Button mBtn;
    public Button mBtnRecord;
    private static final int REQUEST_VIDEO_CAPTURE = 1;

    private static final int REQUEST_EXTERNAL_CAMERA = 101;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRv = findViewById(R.id.rv);
        mBtn = findViewById(R.id.btnPost);
        mBtnRecord = findViewById(R.id.btnRecord);
        initRecyclerView();
        fetchFeed();
        initBtn();
        mBtnRecordClick();
    }

    private void mBtnRecordClick(){
        mBtnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_VIDEO_CAPTURE);
//                    ActivityCompat.requestPermissions(MainActivity.this,
//                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_EXTERNAL_CAMERA);
                    //todo 在这里申请相机、存储的权限
                } else {
                    startActivity(new Intent(MainActivity.this,RecordActivity.class));
                    //todo 打开相机拍摄
                }

            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_VIDEO_CAPTURE: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startActivity(new Intent(MainActivity.this,RecordActivity.class));
                }
                else{
                    Toast.makeText(MainActivity.this,"请手动打开相机权限",Toast.LENGTH_SHORT).show();
                }
                //todo 判断权限是否已经授予
                break;
            }
            default:
                break;
        }
    }

    //显示视频图片
    private void initRecyclerView(){
        mRv.setLayoutManager(new LinearLayoutManager(this));
        mRv.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                ImageView imageView = new ImageView(parent.getContext());
                imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                imageView.setAdjustViewBounds(true);
                return new MainActivity.MyViewHolder(imageView);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
                ImageView iv =(ImageView) holder.itemView;
                String url = mFeeds.get(position).getImage_url();
                if(position <= 2)Glide.with(iv.getContext()).load(url).into(iv);
            //    Glide.with(iv.getContext()).load(url).into(iv);
                iv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity.this, PlayActivity.class);
                        intent.putExtra("url",mFeeds.get(position).getVideo_url());
                        startActivity(intent);
                    }
                });
            }

            @Override
            public int getItemCount() {
                return mFeeds.size();
            }
        });
    }
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
    public void fetchFeed() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://test.androidcamp.bytedance.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Response<FeedResponse> response = null;
        retrofit.create(IMiniDouyinService.class).getFeedResponse().
                enqueue(new Callback<FeedResponse>() {
                    @Override
                    public void onResponse(Call<FeedResponse> call, Response<FeedResponse> response) {
                        mFeeds = response.body().getFeeds();
                        mRv.getAdapter().notifyDataSetChanged();
                        Toast.makeText(MainActivity.this, "fetchFeed Succed",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Call<FeedResponse> call, Throwable t) {
                        Toast.makeText(MainActivity.this, "fetchFeed FAILED",Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //post
    private void initBtn() {
        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String s = mBtn.getText().toString();
                if (getString(R.string.select_an_image).equals(s)) {
                    chooseImage();
                } else if (getString(R.string.select_a_video).equals(s)) {
                    chooseVideo();
                } else if (getString(R.string.post_it).equals(s)) {
                    if (mSelectedVideo != null && mSelectedImage != null) {
                        postVideo();
                    } else {
                        throw new IllegalArgumentException("error data uri, mSelectedVideo = " + mSelectedVideo + ", mSelectedImage = " + mSelectedImage);
                    }
                } else if ((getString(R.string.success_try_refresh).equals(s))) {
                    mBtn.setText(R.string.select_an_image);
                }
            }
        });
    }
    public void chooseImage() {//挑选图片activity
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"),
                PICK_IMAGE);
    }
    public void chooseVideo() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Video"),
                PICK_VIDEO);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && null != data) {

            if (requestCode == PICK_IMAGE) {
                mSelectedImage = data.getData();
                Log.d(TAG, "selectedImage = " + mSelectedImage);
                mBtn.setText(R.string.select_a_video);
            } else if (requestCode == PICK_VIDEO) {
                mSelectedVideo = data.getData();
                Log.d(TAG, "mSelectedVideo = " + mSelectedVideo);
                mBtn.setText(R.string.post_it);
            }
        }
    }
    private MultipartBody.Part getMultipartFromUri(String name, Uri uri) {
        // if NullPointerException thrown, try to allow storage permission in system settings
        File f = new File(ResourceUtils.getRealPath(MainActivity.this, uri));
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), f);
        return MultipartBody.Part.createFormData(name, f.getName(), requestFile);
    }
    private void postVideo() {
        mBtn.setText("POSTING...");
        mBtn.setEnabled(false);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://test.androidcamp.bytedance.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Response<PostVideoResponse> response = null;
        retrofit.create(IMiniDouyinService.class).createVideo("3120186666","⼩青",
                getMultipartFromUri("cover_image", mSelectedImage),getMultipartFromUri("video", mSelectedVideo)).
                enqueue(new Callback<PostVideoResponse>() {
                    @Override
                    public void onResponse(Call<PostVideoResponse> call, Response<PostVideoResponse> response) {
                        Toast.makeText(MainActivity.this, "postVideo SUCCESS",Toast.LENGTH_SHORT).show();
                        mBtn.setText(R.string.select_an_image);
                        mBtn.setEnabled(true);
                        Log.d(TAG, "onResponse: "+response.body());
                    }

                    @Override
                    public void onFailure(Call<PostVideoResponse> call, Throwable t) {
                        Log.d(TAG, "onFailure: "+ t.toString());

                        Toast.makeText(MainActivity.this, "postVideo FAILED",Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
