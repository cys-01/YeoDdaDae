package com.bucheon.yeoddadae;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.NumberFormat;
import java.util.Locale;

public class YdPointRefundActivity extends AppCompatActivity {
    String loginId;
    long ydPoint;
    int refundPoint;
    FirestoreDatabase fd;

    ImageButton refundBackBtn;
    TextView refundHavePointContentTxt;
    EditText refundTargetPointContentEditTxt;
    TextView refundWonTxt;
    EditText refundBankContentEditTxt;
    EditText refundAccountNumberContentEditTxt;
    ImageButton refundBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yd_point_refund);

        refundBackBtn = findViewById(R.id.refundBackBtn);
        refundHavePointContentTxt = findViewById(R.id.refundHavePointContentTxt);
        refundTargetPointContentEditTxt = findViewById(R.id.refundTargetPointContentEditTxt);
        refundWonTxt = findViewById(R.id.refundWonTxt);
        refundBankContentEditTxt = findViewById(R.id.refundBankContentEditTxt);
        refundAccountNumberContentEditTxt = findViewById(R.id.refundAccountNumberContentEditTxt);
        refundBtn = findViewById(R.id.refundBtn);

        Intent inIntent = getIntent();
        loginId = inIntent.getStringExtra("loginId");

        fd = new FirestoreDatabase();

        refundBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        refundTargetPointContentEditTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (refundTargetPointContentEditTxt.getText().toString() == null
                        || refundTargetPointContentEditTxt.getText().toString().equals("")) {
                    refundWonTxt.setText("(0 원)");
                }
                else {
                    long won = Long.parseLong(refundTargetPointContentEditTxt.getText().toString());
                    String formattedWon = NumberFormat.getNumberInstance(Locale.KOREA).format(won);
                    refundWonTxt.setText("(" +  formattedWon+ " 원)");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        refundBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String targetPoint = refundTargetPointContentEditTxt.getText().toString();
                String bank = refundBankContentEditTxt.getText().toString();
                String accountNumber = refundAccountNumberContentEditTxt.getText().toString();

                if (targetPoint.equals("")) {
                    Toast.makeText(getApplicationContext(), "환급할 금액을 입력해주세요", Toast.LENGTH_SHORT).show();
                }
                else if (bank.equals("")) {
                    Toast.makeText(getApplicationContext(), "환급할 계좌의 은행을 입력해주세요", Toast.LENGTH_SHORT).show();
                }
                else if (accountNumber.equals("")) {
                    Toast.makeText(getApplicationContext(), "환급할 계좌번호를 입력해주세요", Toast.LENGTH_SHORT).show();
                }
                else if (accountNumber.length() < 10 || accountNumber.length() > 14) {
                    Toast.makeText(getApplicationContext(), "계좌번호는 10~14자 입니다", Toast.LENGTH_SHORT).show();
                }
                else {
                    refundPoint = Integer.parseInt(refundTargetPointContentEditTxt.getText().toString());

                    fd.refundYdPoint(loginId, refundPoint, bank, accountNumber, new OnFirestoreDataLoadedListener() {
                        @Override
                        public void onDataLoaded(Object data) {
                            Toast.makeText(getApplicationContext(), "환급 완료", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onDataLoadError(String errorMessage) {
                            Log.d(TAG, errorMessage);
                            if (errorMessage.equals("환급 포인트가 보유 포인트보다 큽니다")) {
                                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Toast.makeText(getApplicationContext(), "오류 발생", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        fd.loadYdPoint(loginId, new OnFirestoreDataLoadedListener() {
            @Override
            public void onDataLoaded(Object data) {
                ydPoint = (long) data;
                String formattedYdPoint = NumberFormat.getNumberInstance(Locale.KOREA).format(ydPoint);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refundHavePointContentTxt.setText(formattedYdPoint);
                    }
                });
            }

            @Override
            public void onDataLoadError(String errorMessage) {
                Log.d(TAG, errorMessage);
                Toast.makeText(getApplicationContext(), "오류 발생", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
