package com.bucheon.yeoddadae;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.skt.Tmap.TMapCircle;
import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;
import com.skt.Tmap.poi_item.TMapPOIItem;
import com.skt.tmap.engine.navigation.SDKManager;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class FindParkActivity extends AppCompatActivity implements TMapGpsManager.onLocationChangedCallback, TMapView.OnClickListenerCallback {
    private static final int PERMISSION_REQUEST_CODE = 1;

    boolean firstOnLocationChangeCalled = false; // onLocationChange가 처음 불림 여부

    int recievedSort;
    int nowSort;

    int clickParkType;

    ListView parkListView;
    Button findParkBackBtn;
    Button zoomOutBtn;
    Button zoomInBtn;
    Button gpsBtn;
    HorizontalScrollView parkSortHorizontalScrollView;
    Button sortByDistanceBtn;
    Button sortByRateBtn;
    Button sortByParkPriceBtn;
    Button cancelNaviBtn;
    Button toStartNaviBtn;
    Button searchStartBtn;
    ConstraintLayout searchConstraintLayout;
    EditText searchEdTxt;
    Button searchBtn;
    Button searchBackBtn;
    ListView searchListView;

    AlertDialog loadingAlert;

    Bitmap tmapMyLocationIcon;
    Bitmap tmapMarkerIcon;
    Bitmap tmapStartMarkerIcon;
    Bitmap tmapSelectedMarkerIcon;
    Bitmap tmapShareParkMarkerIcon;

    // 경복궁
    double lat = 37.578611; // 위도
    double lon = 126.977222; // 경도

    private static String CLIENT_ID = "";
    private static String API_KEY = "iqTSQ2hMuj8E7t2sy3WYA5m73LuX4iUD5iHgwRGf";
    private static String USER_KEY = ""; // USER KEY 입력 필수 아님 : Copy License 기준 서비스 운영시 필요
    private static String DEVICE_KEY = "";

    TMapPoint nowPoint = new TMapPoint(lat, lon);
    TMapPoint naviEndPoint;
    String naviEndPointName;
    TMapGpsManager gpsManager;
    TMapView tMapView;
    TMapCircle tMapCircle;
    TMapData tMapData;
    TMapMarkerItem selectedMarker;
    ParkAdapter parkAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_park);

        // 로딩 시작
        loadingStart();

        // 메인액티비티에서 정렬기준 받기
        Intent inIntent = getIntent();
        recievedSort = inIntent.getIntExtra("SortBy", 0);
        if (recievedSort == 0) {
            Log.d(TAG, "sttSort가 0임 (오류)");
            finish();
        }

        // 뷰 정의
        parkListView = findViewById(R.id.parkListView);
        findParkBackBtn = findViewById(R.id.findParkBackBtn);
        zoomOutBtn = findViewById(R.id.zoomOutBtn);
        zoomInBtn = findViewById(R.id.zoomInBtn);
        gpsBtn = findViewById(R.id.gpsBtn);
        parkSortHorizontalScrollView = findViewById(R.id.parkSortHorizontalScrollView);
        sortByDistanceBtn = findViewById(R.id.sortByDistanceBtn);
        sortByRateBtn = findViewById(R.id.sortByRateBtn);
        sortByParkPriceBtn = findViewById(R.id.sortByParkPriceBtn);
        cancelNaviBtn = findViewById(R.id.cancelNaviBtn);
        toStartNaviBtn = findViewById(R.id.toStartNaviBtn);
        searchStartBtn = findViewById(R.id.searchStartBtn);
        searchConstraintLayout = findViewById(R.id.searchConstraintLayout);
        searchEdTxt = findViewById(R.id.searchEdTxt);
        searchBtn = findViewById(R.id.searchBtn);
        searchBackBtn = findViewById(R.id.searchBackBtn);
        searchListView = findViewById(R.id.searchListView);

        // Bitmap 정의
        tmapMyLocationIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.temp_tmap_my_location);
        tmapMarkerIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.temp_tmap_marker);
        tmapSelectedMarkerIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.temp_tmap_selected_marker);
        tmapStartMarkerIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.temp_tmap_start_marker);
        tmapShareParkMarkerIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.temp_tmap_share_park_marker);

        // 로딩 완료까지 뷰 없애기
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parkListView.setVisibility(View.GONE);
                parkSortHorizontalScrollView.setVisibility(View.GONE);
            }
        });
        // 권한 확인 후 초기화
        checkPermission();
    }

    private void checkPermission() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "권한 있음");
            init();
        }
        else {
            Log.d(TAG, "권한 없음. 요청");
            String[] permissionArr = {android.Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
            requestPermissions(permissionArr, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "권한요청에서 허가");
            init();
        }
        else {
            Log.d (TAG, "권한요청에서 거부or문제");
            finish();
        }
    }

    private void init() { // 초기화
        // 최초 위치 한 번 받기
        Location currentLocation = SDKManager.getInstance().getCurrentPosition();
        lat = currentLocation.getLatitude();
        lon = currentLocation.getLongitude();
        nowPoint = new TMapPoint(lat, lon);

        // TMapGpsManager 설정
        gpsManager = new TMapGpsManager(this);
        gpsManager.setMinTime(1000); // ms단위
        gpsManager.setMinDistance(1); // m단위
        gpsManager.setProvider(gpsManager.GPS_PROVIDER);
        gpsManager.OpenGps();
        /* 가상머신 말고 실제 기기로 실내에서 사용 시 필요
        gpsManager.setProvider(gpsManager.NETWORK_PROVIDER);
        gpsManager.OpenGps();
        */

        // TMapView 생성 및 보이기
        tMapView = new TMapView(this);
        LinearLayout linearLayoutTmap = (LinearLayout)findViewById(R.id.linearLayoutTmap);
        linearLayoutTmap.addView( tMapView );

        // TMapView 초기설정
        tMapView.setCenterPoint(lon, lat);
        tMapView.setLocationPoint(lon, lat);
        tMapView.setIcon(tmapMyLocationIcon);
        tMapView.setIconVisibility(true);
        tMapView.setSightVisible(true);

        // TMapCircle 초기설정
        tMapCircle = new TMapCircle();
        tMapCircle.setRadius(3000);
        tMapCircle.setCircleWidth(0);
        tMapCircle.setAreaColor(Color.rgb(0, 0, 0));
        tMapCircle.setAreaAlpha(25);

        // TMapCircle 추가
        tMapView.addTMapCircle("Circle", tMapCircle);

        findPark(recievedSort);

        // 버튼 클릭 이벤트 리스너들
        findParkBackBtn.setOnClickListener(new View.OnClickListener() { // 액티비티 종료
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        gpsBtn.setOnClickListener(new View.OnClickListener() { // GPS 현재 위치로 맵 중심점 이동
            @Override
            public void onClick(View v) {
                tMapView.setCenterPoint(lon, lat);
                tMapView.setZoomLevel(16);
            }
        });

        zoomOutBtn.setOnClickListener(new View.OnClickListener() { // 맵 축소
            @Override
            public void onClick(View v) {
                if (tMapView.getZoomLevel() >= 8 ) {
                    tMapView.MapZoomOut();
                    Log.d(TAG, "축소, 현재 ZoomLevel : " + tMapView.getZoomLevel());
                }
            }
        });

        zoomInBtn.setOnClickListener(new View.OnClickListener() { // 맵 확대
            @Override
            public void onClick(View v) {
                if (tMapView.getZoomLevel() <= 17 ) {
                    tMapView.MapZoomIn();
                    Log.d(TAG, "확대, 현재 ZoomLevel : " + tMapView.getZoomLevel());
                }
            }
        });

        sortByDistanceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingStart();
                findPark(1);
            }
        });

        sortByRateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingStart();
                findPark(2);
            }
        });

        sortByParkPriceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingStart();
                findPark(3);
            }
        });

        parkListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 클릭한 ParkItem을 가져옴
                ParkItem clickedPark = (ParkItem) parent.getItemAtPosition(position);

                Log.d(TAG, "리스트뷰에서 주유소 아이템 클릭함 : " + clickedPark.getName() + ", " + clickedPark.getRadius() + ", " + clickedPark.getParkPrice()
                        + ", " + clickedPark.getPhone() + ", " + clickedPark.getAddition()
                        + ", " + clickedPark.getStarRate() + ", " + clickedPark.getLat() + ", " + clickedPark.getLon());

                // parkListView에 clickedPark만 있도록
                ParkAdapter tempParkAdapter = new ParkAdapter();
                tempParkAdapter.addItem(clickedPark);
                parkListView.setAdapter(tempParkAdapter);

                // 도착점 설정
                naviEndPoint = new TMapPoint (clickedPark.getLat(), clickedPark.getLon());
                naviEndPointName = clickedPark.getName();

                // 길 찾기 및 선 표시
                TMapData tmapdata = new TMapData();
                tmapdata.findPathData(nowPoint, naviEndPoint, new TMapData.FindPathDataListenerCallback() {
                    @Override
                    public void onFindPathData(TMapPolyLine polyLine) {
                        if (selectedMarker != null) {
                            if (clickedPark.getType() == 3) {
                                selectedMarker.setIcon(tmapShareParkMarkerIcon);
                            }
                            else {
                                selectedMarker.setIcon(tmapMarkerIcon);
                            }
                        }

                        tMapView.setTMapPathIcon(tmapStartMarkerIcon, null);
                        TMapMarkerItem endMarker = tMapView.getMarkerItemFromID(clickedPark.getName());
                        endMarker.setIcon(tmapSelectedMarkerIcon);

                        selectedMarker = endMarker;

                        tMapView.addTMapPath(polyLine);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                                float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, displayMetrics);
                                parkListView.getLayoutParams().height = (int) px;
                                parkListView.requestLayout();

                                TextView parkOrder = findViewById(R.id.parkOrder);
                                parkOrder.setVisibility(View.GONE);

                                TextView parkName = findViewById(R.id.parkName);
                                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) parkName.getLayoutParams();
                                params.leftToLeft = R.id.parkItemConstraintLayout;
                                parkName.setLayoutParams(params);

                                parkSortHorizontalScrollView.setVisibility(View.GONE);
                                searchStartBtn.setVisibility(View.GONE);
                                cancelNaviBtn.setVisibility(View.VISIBLE);
                                toStartNaviBtn.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });
            }
        });

        cancelNaviBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tMapView.removeTMapPath();

                        parkSortHorizontalScrollView.setVisibility(View.VISIBLE);
                        searchStartBtn.setVisibility(View.VISIBLE);
                        cancelNaviBtn.setVisibility(View.GONE);
                        toStartNaviBtn.setVisibility(View.GONE);

                        findPark(nowSort);
                    }
                });
            }
        });

        toStartNaviBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent naviIntent = new Intent(getApplicationContext(), NavigationActivity.class);
                naviIntent.putExtra("endPointLat", naviEndPoint.getLatitude());
                naviIntent.putExtra("endPointLon", naviEndPoint.getLongitude());
                naviIntent.putExtra("endPointName", naviEndPointName);
                startActivity(naviIntent);
            }
        });

        searchStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        searchConstraintLayout.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!searchEdTxt.getText().toString().equals("")) {
                    tMapData = new TMapData();
                    tMapData.findAddressPOI(searchEdTxt.getText().toString(), new TMapData.FindAddressPOIListenerCallback() {
                        @Override
                        public void onFindAddressPOI(ArrayList<TMapPOIItem> arrayList) {
                            if (arrayList == null) {
                                return;
                            }
                            SearchParkAdapter spa = new SearchParkAdapter();

                            for (int i = 0; i < arrayList.size(); i++) {
                                TMapPOIItem item = arrayList.get(i);
                                spa.addItem(new ParkItem(123, item.name, item.radius, null, null, null, 0, item.frontLat, item.frontLon));
                            }

                            tMapData.findTitlePOI(searchEdTxt.getText().toString(), new TMapData.FindTitlePOIListenerCallback() {
                                @Override
                                public void onFindTitlePOI(ArrayList<TMapPOIItem> arrayList) {
                                    if (arrayList == null) {
                                        return;
                                    }

                                    for (int i = 0; i < arrayList.size(); i++) {
                                        TMapPOIItem item = arrayList.get(i);
                                        spa.addItem(new ParkItem(456, item.name, item.radius, null, item.telNo, null, 0, item.frontLat, item.frontLon));

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                searchListView.setAdapter(spa);
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });
        searchBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        searchConstraintLayout.setVisibility(View.GONE);
                    }
                });
            }
        });

        searchListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 클릭한 ParkItem을 가져옴
                ParkItem clickedPark = (ParkItem) parent.getItemAtPosition(position);

                Log.d(TAG, "검색 리스트뷰에서 아이템 클릭함 : " + clickedPark.getType() + ", " + clickedPark.getName() + ", " + clickedPark.getRadius() + ", " + clickedPark.getParkPrice()
                        + ", " + clickedPark.getPhone() + ", " + clickedPark.getAddition()
                        + ", " + clickedPark.getStarRate() + ", " + clickedPark.getLat() + ", " + clickedPark.getLon());

                // 키보드 내리기
                InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (manager != null && getCurrentFocus() != null && getCurrentFocus().getWindowToken() != null) {
                    manager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }

                tMapView.setCenterPoint(clickedPark.getLon(), clickedPark.getLat());
                findPark(recievedSort);

                if (clickedPark.getType() == 123) {
                    // 주소
                }
                else if (clickedPark.getType() == 456){
                    // 장소
                    TMapPoint tpoint = new TMapPoint(clickedPark.getLat(), clickedPark.getLon());
                    TMapMarkerItem tItem = new TMapMarkerItem();
                    tItem.setTMapPoint(tpoint);
                    tItem.setName(clickedPark.getName());
                    tItem.setVisible(TMapMarkerItem.VISIBLE);
                    tItem.setIcon(tmapShareParkMarkerIcon);
                    tItem.setPosition(0.5f,1.0f); // 마커의 중심점을 하단, 중앙으로 설정
                    tMapView.addMarkerItem(clickedPark.getName(), tItem);
                }
                searchConstraintLayout.setVisibility(View.GONE);
            }
        });

        // 뷰 보이기
        parkListView.setVisibility(View.VISIBLE);
        parkSortHorizontalScrollView.setVisibility(View.VISIBLE);
    }

    public void findPark(int sortBy) { // sortBy는 정렬기준 (1:거리순, 2:평점순, 3:휘발유가순, 4: 경유가순, 5:LPG가순
        Log.d (TAG, "findPark 시작");
        tMapView.removeAllMarkerItem();

        TMapPoint centerPoint = tMapView.getCenterPoint();

        parkAdapter = new ParkAdapter();

        FirestoreDatabase fd = new FirestoreDatabase();
        fd.findSharePark(centerPoint.getLatitude(), centerPoint.getLongitude(), 3, new OnFirestoreDataLoadedListener() {
            @Override
            public void onDataLoaded(Object data) {
                ArrayList<ParkItem> dataList = (ArrayList<ParkItem>) data;

                for (ParkItem item : dataList) {
                    parkAdapter.addItem(item);
                    TMapPoint tpoint = new TMapPoint(item.getLat(), item.getLon());
                    TMapMarkerItem tItem = new TMapMarkerItem();
                    tItem.setTMapPoint(tpoint);
                    tItem.setName(item.getName());
                    tItem.setVisible(TMapMarkerItem.VISIBLE);
                    tItem.setIcon(tmapShareParkMarkerIcon);
                    tItem.setPosition(0.5f,1.0f); // 마커의 중심점을 하단, 중앙으로 설정
                    tMapView.addMarkerItem(item.getName(), tItem);
                }
            }

            @Override
            public void onDataLoadError(String errorMessage) {
                Log.e("FirestoreDataError", errorMessage);
            }
        });


        tMapData = new TMapData();
        tMapData.findAroundNamePOI(centerPoint, "주차장", 3, 200, new TMapData.FindAroundNamePOIListenerCallback ()
        {
            @Override
            public void onFindAroundNamePOI(ArrayList<TMapPOIItem> arrayList) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (arrayList.size() == 0) {
                            parkListView.getLayoutParams().height = 0;
                            parkListView.requestLayout();
                        }
                        else if(arrayList.size() == 1) {
                            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                            float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, displayMetrics);
                            parkListView.getLayoutParams().height = (int) px;
                            parkListView.requestLayout();
                        }
                        else if (arrayList.size() == 2) {
                            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                            float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, displayMetrics);
                            parkListView.getLayoutParams().height = (int) px;
                            parkListView.requestLayout();
                        }
                        else {
                            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                            float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 240, displayMetrics);
                            parkListView.getLayoutParams().height = (int) px;
                            parkListView.requestLayout();
                        }

                        int originalBackgroundColor = Color.rgb(128,128,128);
                        int selectedBackgroundColor = Color.rgb(0,0,255);

                        for (int i = 0; i < arrayList.size(); i++) {
                            TMapPOIItem item = arrayList.get(i);

                            parkAdapter.addItem(new ParkItem(0, item.name, item.radius, item.fee, item.telNo, item.additionalInfo, 0, item.frontLat, item.frontLon));
                            TMapPoint tpoint = new TMapPoint(Double.parseDouble(item.frontLat), Double.parseDouble(item.frontLon));
                            TMapMarkerItem tItem = new TMapMarkerItem();
                            tItem.setTMapPoint(tpoint);
                            tItem.setName(item.name);
                            tItem.setVisible(TMapMarkerItem.VISIBLE);
                            tItem.setIcon(tmapMarkerIcon);
                            tItem.setPosition(0.5f,1.0f); // 마커의 중심점을 하단, 중앙으로 설정
                            tMapView.addMarkerItem(item.name, tItem);
                        }

                        switch (sortBy) {
                            case 1 :
                                sortByDistanceBtn.setBackgroundColor(selectedBackgroundColor);
                                sortByRateBtn.setBackgroundColor(originalBackgroundColor);
                                sortByParkPriceBtn.setBackgroundColor(originalBackgroundColor);
                                parkAdapter.sortByDistance();
                                break;
                            case 2 :
                                sortByDistanceBtn.setBackgroundColor(originalBackgroundColor);
                                sortByRateBtn.setBackgroundColor(selectedBackgroundColor);
                                sortByParkPriceBtn.setBackgroundColor(originalBackgroundColor);
                                parkAdapter.sortByRate();
                                break;
                            case 3 :
                                sortByDistanceBtn.setBackgroundColor(originalBackgroundColor);
                                sortByRateBtn.setBackgroundColor(originalBackgroundColor);
                                sortByParkPriceBtn.setBackgroundColor(selectedBackgroundColor);
                                parkAdapter.sortByParkPrice();
                                break;
                        }
                    }
                });
            }
        });
        parkListView.setAdapter(parkAdapter);

        parkListView.setVisibility(View.VISIBLE);
        parkSortHorizontalScrollView.setVisibility(View.VISIBLE);

        nowSort = sortBy;

        tMapView.removeAllTMapCircle();
        tMapCircle.setCenterPoint( nowPoint );
        tMapView.addTMapCircle("Circle", tMapCircle);

        loadingStop();
    }

    // TMapView 터치이벤트
    @Override
    public boolean onPressEvent(ArrayList<TMapMarkerItem> arrayList, ArrayList<TMapPOIItem> arrayList1, TMapPoint tMapPoint, PointF pointF) {
        if (arrayList.size() > 0) { // TMapMarkerItem이 클릭됬다면
            TMapMarkerItem endMarker = arrayList.get(0); // endMarker = 클릭한 TMapMarkerItem

            ParkItem clickedPark = parkAdapter.findItem(endMarker.getName());
            naviEndPoint = endMarker.getTMapPoint();
            naviEndPointName = clickedPark.getName();
            TMapData tmapdata = new TMapData();

            Log.d(TAG, "TMapView에서 주유소 마커 클릭함 : " + endMarker.getName() + ", " + naviEndPoint.getLatitude() + ", " + naviEndPoint.getLongitude());

            // parkListView에 clickedPark만 있도록
            ParkAdapter tempParkAdapter = new ParkAdapter();
            tempParkAdapter.addItem(clickedPark);
            parkListView.setAdapter(tempParkAdapter);

            // 길 찾기 및 선 표시
            tmapdata.findPathData(nowPoint, naviEndPoint, new TMapData.FindPathDataListenerCallback() {
                @Override
                public void onFindPathData(TMapPolyLine polyLine) {
                    if (selectedMarker != null) {
                        if (clickParkType == 3) {
                            selectedMarker.setIcon(tmapShareParkMarkerIcon);
                        }
                        else {
                            selectedMarker.setIcon(tmapMarkerIcon);
                        }
                    }

                    clickParkType = clickedPark.getType();

                    tMapView.setTMapPathIcon(tmapStartMarkerIcon, null);
                    endMarker.setIcon(tmapSelectedMarkerIcon);

                    selectedMarker = endMarker;

                    tMapView.addTMapPath(polyLine);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                            float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, displayMetrics);
                            parkListView.getLayoutParams().height = (int) px;
                            parkListView.requestLayout();

                            TextView parkOrder = findViewById(R.id.parkOrder);
                            parkOrder.setVisibility(View.GONE);

                            TextView parkName = findViewById(R.id.parkName);
                            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) parkName.getLayoutParams();
                            params.leftToLeft = R.id.parkItemConstraintLayout;
                            parkName.setLayoutParams(params);

                            parkSortHorizontalScrollView.setVisibility(View.GONE);
                            searchStartBtn.setVisibility(View.GONE);
                            cancelNaviBtn.setVisibility(View.VISIBLE);
                            toStartNaviBtn.setVisibility(View.VISIBLE);
                        }
                    });
                }
            });
        }
        return false;
    }

    @Override
    public boolean onPressUpEvent(ArrayList<TMapMarkerItem> arrayList, ArrayList<TMapPOIItem> arrayList1, TMapPoint tMapPoint, PointF pointF) {
        return false;
    }

    @Override
    public void onLocationChange(Location location){
        Log.d(TAG, "onLocationChange 호출됨");

        lat = location.getLatitude();
        Log.d(TAG, "onLocationChange 호출됨 : lat(경도) = " + lat);

        lon = location.getLongitude();
        Log.d(TAG, "onLocationChange 호출됨 : lon(위도) = " + lon);

        nowPoint = gpsManager.getLocation();
        tMapView.setLocationPoint(lon, lat);

        if (!firstOnLocationChangeCalled) {
            loadingStart();
            tMapView.setCenterPoint(lon, lat);
            findPark(recievedSort);
            firstOnLocationChangeCalled = true;
        }
    }

    public void loadingStart() {
        Log.d(TAG, "로딩 시작");

        // 로딩 AlertDialog 지정
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("로딩 중").setCancelable(false).setNegativeButton("액티비티 종료", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        loadingAlert = builder.create();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        loadingAlert.show();
    }

    public void loadingStop() {
        Log.d(TAG, "로딩 끝");

        loadingAlert.dismiss();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        loadingAlert.dismiss();
    }
}