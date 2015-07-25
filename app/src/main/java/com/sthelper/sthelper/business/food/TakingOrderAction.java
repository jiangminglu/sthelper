package com.sthelper.sthelper.business.food;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.sthelper.sthelper.R;
import com.sthelper.sthelper.SApp;
import com.sthelper.sthelper.api.BaseApi;
import com.sthelper.sthelper.api.FoodApi;
import com.sthelper.sthelper.bean.FoodStoreBean;
import com.sthelper.sthelper.bean.Goods;
import com.sthelper.sthelper.bean.GoodsInfo;
import com.sthelper.sthelper.bean.GoodsItemBean;
import com.sthelper.sthelper.business.BaseAction;
import com.sthelper.sthelper.business.CarAction;
import com.sthelper.sthelper.business.adapter.GoodsItemAdapter;
import com.sthelper.sthelper.util.ImageLoadUtil;
import com.sthelper.sthelper.view.SListView;

import org.apache.http.Header;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.sthelper.sthelper.R.id.none;
import static com.sthelper.sthelper.R.id.store_rating;

/**
 * 下订单点菜
 */
public class TakingOrderAction extends BaseAction {
    private FoodStoreBean bean;
    private ImageView storeImg;
    private RatingBar storeRate, yelloRate, blueRate;
    private TextView storeNameTv;
    private LinearLayout storeGoodsListContent;
    private SListView rightListView;
    private ArrayList<GoodsInfo> list;
    private GoodsItemAdapter adapter = null;
    private TextView totalPriceTv;

    private ArrayList<Goods> goodsList;
    private int type = 100;
    private Dialog detailDialog;

    private ArrayList<GoodsInfo> priceList = new ArrayList<GoodsInfo>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taking_order_action);

        type = getIntent().getIntExtra("type", 100);
        bean = getIntent().getParcelableExtra("bean");
        initActionBar(bean.shop_name);
        if (type == 100) {
            actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.app_default_actionbar_bg)));
        } else {
            actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.app_blue_actionbar_bg)));
        }
        init();
        loadData();
    }

    private void init() {

        goodsList = new ArrayList<Goods>();
        list = new ArrayList<GoodsInfo>();

        totalPriceTv = (TextView) findViewById(R.id.total_price);
        totalPriceTv.setText("0￥");
        storeImg = (ImageView) findViewById(R.id.store_pic);
        storeNameTv = (TextView) findViewById(R.id.store_name);
        yelloRate = (RatingBar) findViewById(store_rating);
        blueRate = (RatingBar) findViewById(R.id.store_rating_blue);


        if (type == 100) {
            totalPriceTv.setBackgroundResource(R.drawable.yellow_circle);
            blueRate.setVisibility(View.GONE);
            yelloRate.setVisibility(View.VISIBLE);
            storeRate = yelloRate;
            storeNameTv.setTextColor(Color.parseColor("#d85b00"));
        } else {
            totalPriceTv.setBackgroundResource(R.drawable.blue_circle);
            blueRate.setVisibility(View.VISIBLE);
            yelloRate.setVisibility(View.GONE);
            storeRate = blueRate;
            storeNameTv.setTextColor(getResources().getColor(R.color.app_blue_actionbar_bg));
        }

        storeGoodsListContent = (LinearLayout) findViewById(R.id.store_goods_content);
        rightListView = (SListView) findViewById(R.id.store_goods_item_listview);
        adapter = new GoodsItemAdapter(list, this, type);
        rightListView.setAdapter(adapter);
        rightListView.setOnItemClickListener(onItemClickListener);
        storeImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(mActivity, StoreInfoAction.class);
                startActivity(intent);
            }
        });
        totalPriceTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(mActivity, CarAction.class);
                intent.putParcelableArrayListExtra("list", priceList);
                intent.putExtra("type", type);
                startActivity(intent);
            }
        });


        storeNameTv.setText(bean.shop_name);
        ImageLoadUtil.getCommonImage(storeImg, SApp.IMG_URL + bean.photo);
        storeRate.setRating(bean.score);
    }

    private void initDialog(GoodsInfo info) {
        detailDialog = new Dialog(mActivity, R.style.full_dialog);
        View view = getLayoutInflater().inflate(R.layout.goods_item_detail_layout, null);
        detailDialog.setContentView(view);
        TextView itemNameTv = (TextView) view.findViewById(R.id.goods_item_name);
        TextView itemContentTv = (TextView) view.findViewById(R.id.goods_item_desc);
        ImageView img = (ImageView) view.findViewById(R.id.goods_item_photo_img);
        itemNameTv.setText(info.product_name);
        itemContentTv.setText(info.instructions);
        ImageLoadUtil.getCommonImage(img,SApp.IMG_URL+info.photo);

    }

    private void loadData() {
        processDialog.show();
        FoodApi api = new FoodApi();
        api.getShopGoodsList(bean.shop_id, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                processDialog.dismiss();
                try {
                    JsonNode node = BaseApi.mapper.readTree(response.toString());
                    if (0 == node.path("ret").asInt()) {
                        JsonNode result = node.path("result");
                        JsonNode goodsNode = result.path("goods");
                        JsonNode shopInfoNode = result.path("shopinfo");
                        for (JsonNode item : goodsNode) {
                            Goods goods = new Goods();
                            goods.cate_name = item.path("cate_name").asText();
                            goods.goodsinfo = new ArrayList<GoodsInfo>();
                            JsonNode goodsInfoNode = item.path("goodsinfo");
                            for (JsonNode bean : goodsInfoNode) {
                                GoodsInfo info = new GoodsInfo();
                                info.product_name = bean.path("product_name").asText();
                                info.area_id = bean.path("area_id").asInt();
                                info.business_id = bean.path("business_id").asInt();
                                info.cate_id = bean.path("cate_id").asInt();
                                info.product_id = bean.path("product_id").asInt();
                                info.price = bean.path("price").asDouble();
                                info.shop_id = bean.path("shop_id").asInt();
                                info.photo = bean.path("photo").asText();
                                info.instructions = bean.path("instructions").asText();
                                goods.goodsinfo.add(info);
                            }
                            goodsList.add(goods);
                        }
                        initLeftView();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                processDialog.dismiss();
            }
        });
    }


    /**
     * 刷新左边商品分类列表
     */
    private void initLeftView() {
        storeGoodsListContent.removeAllViews();
        for (int i = 0; i < goodsList.size(); i++) {
            Goods goods = goodsList.get(i);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            TextView textView = new TextView(mActivity);
            textView.setPadding(0, 24, 0, 24);
            textView.setText(goods.cate_name);
            textView.setGravity(Gravity.CENTER);
            textView.setTextSize(16);
            textView.setTextColor(Color.BLACK);
            textView.setLayoutParams(params);
            storeGoodsListContent.addView(textView);
            textView.setOnClickListener(onClickListener);
        }
        initRightList(goodsList.get(0).goodsinfo);
    }

    private void initRightList(List<GoodsInfo> goodsInfoArrayList) {
        list.removeAll(list);
        list.addAll(goodsInfoArrayList);
        adapter.notifyDataSetChanged();
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            int count = storeGoodsListContent.getChildCount();
            for (int i = 0; i < count; i++) {
                TextView item = (TextView) storeGoodsListContent.getChildAt(i);
                item.setBackgroundResource(R.drawable.goods_item_normal);
                item.setTextColor(Color.BLACK);
            }

            TextView textView = (TextView) view;
            textView.setBackgroundResource(R.drawable.goods_item_pressed);
            if (type == 100) {
                textView.setTextColor(Color.parseColor("#d9681d"));
            } else {
                textView.setTextColor(getResources().getColor(R.color.app_blue_actionbar_bg));
            }
        }
    };

    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            initDialog(list.get(i));
            detailDialog.show();
        }
    };

    /**
     * 加入到购物车
     *
     * @param position
     */
    public void add2Car(int position) {
        GoodsInfo info = list.get(position);

        boolean flag = false;
        GoodsInfo goodsInfoIndex = null;
        for (GoodsInfo bean : priceList) {
            if (bean.product_id == info.product_id) {
                flag = true;
                goodsInfoIndex = bean;
                break;
            }
        }
        if (flag) {
            goodsInfoIndex.num += 1;
        } else {
            priceList.add(info);
        }

        double price = 0;
        int num = 0;
        for (GoodsInfo item : priceList) {
            price += item.price * item.num;
            num += item.num;
        }
        totalPriceTv.setText("￥" + price);
        TextView numTv = (TextView) findViewById(R.id.goods_num);

        numTv.setText("已点" + num + "件");
    }
}

