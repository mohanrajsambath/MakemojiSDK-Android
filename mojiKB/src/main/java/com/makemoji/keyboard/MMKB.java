package com.makemoji.keyboard;


import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService;
import android.net.Uri;
import android.support.design.widget.TabLayout;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.makemoji.mojilib.CategoryPopulator;
import com.makemoji.mojilib.Moji;
import com.makemoji.mojilib.MojiGridAdapter;
import com.makemoji.mojilib.MojiSpan;
import com.makemoji.mojilib.OneGridPage;
import com.makemoji.mojilib.PagerPopulator;
import com.makemoji.mojilib.SpacesItemDecoration;
import com.makemoji.mojilib.Spanimator;
import com.makemoji.mojilib.TrendingPopulator;
import com.makemoji.mojilib.model.Category;
import com.makemoji.mojilib.model.MojiModel;
import com.squareup.picasso252.Picasso;
import com.squareup.picasso252.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;


/**
 * Created by DouglasW on 3/29/2016.
 */
public class MMKB extends InputMethodService implements TabLayout.OnTabSelectedListener,MojiGridAdapter.ClickAndStyler,
        PagerPopulator.PopulatorObserver{

    View inputView;
    String packageName;
    TabLayout tabLayout;
    RecyclerView rv;
    RecyclerView.ItemDecoration itemDecoration;
    PagerPopulator<MojiModel> populator;
    int mojisPerPage;
    MojiGridAdapter adapter;
    TextView heading, shareText;


    @Override public View onCreateInputView() {
        inputView =  getLayoutInflater().inflate(
                R.layout.kb_layout, null);
        tabLayout = (TabLayout)inputView.findViewById(R.id.tabs);
        rv = (RecyclerView) inputView.findViewById(R.id.kb_page_grid);
        rv.setLayoutManager(new GridLayoutManager(inputView.getContext(), OneGridPage.ROWS, LinearLayoutManager.HORIZONTAL, false));
        heading = (TextView) inputView.findViewById(R.id.kb_page_heading);
        shareText = (TextView) inputView.findViewById(R.id.share_kb_tv);
        List<TabLayout.Tab> tabs = KBCategory.getTabs(tabLayout);
        for (TabLayout.Tab tab: tabs) tabLayout.addTab(tab);
        tabLayout.setOnTabSelectedListener(this);
        tabs.get(0).select();
        inputView.findViewById(R.id.kb_backspace_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentInputConnection().deleteSurroundingText(1,0);
            }
        });
        inputView.findViewById(R.id.kb_abc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showInputMethodPicker();
            }
        });
        inputView.findViewById(R.id.share_kb_tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentInputConnection().setComposingText(" Check out the MakeMoji: Keyboard " +
                        "http://play.google.com/store/apps/details?id="+BuildConfig.APPLICATION_ID+ " ",1);
                getCurrentInputConnection().finishComposingText();
            }
        });
        return inputView;
    }


    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        packageName = attribute.packageName;

    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        heading.setText(tab.getContentDescription());
        if (populator!=null)populator.teardown();
        if ("trending".equals(tab.getContentDescription()))
            populator = new TrendingPopulator();
        else
            populator = new CategoryPopulator(new Category(tab.getContentDescription().toString(),null));
        populator.setup(this);
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }


    @Override
    public void onNewDataAvailable() {

        int h = rv.getHeight();
        int size = h / OneGridPage.ROWS;
        int vSpace = (h - (size * OneGridPage.ROWS)) / OneGridPage.ROWS;
        int hSpace = (rv.getWidth() - (size * 8)) / 16;


        mojisPerPage = Math.max(10, 8 * OneGridPage.ROWS);
        List<MojiModel> models =populator.populatePage(populator.getTotalCount(),0);
        adapter = new MojiGridAdapter(models,this,OneGridPage.ROWS,size);
        if (itemDecoration!=null) rv.removeItemDecoration(itemDecoration);
        itemDecoration = new SpacesItemDecoration(vSpace, hSpace);
        rv.addItemDecoration(itemDecoration);
        rv.setAdapter(adapter);

        Spanimator.onResume();

    }

    public Target getTarget(final MojiModel model) {
        return new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                FileOutputStream out = null;
                File path = new File(getFilesDir(),"images");
                path.mkdir();
                File cacheFile = new File(path,"share.png");
                try {
                    out = new FileOutputStream(cacheFile.getPath());
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Load failed", Toast.LENGTH_SHORT).show();
                    return;
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Load failed", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Uri uri = FileProvider.getUriForFile(getContext(),"com.makemoji.keyboard.fileprovider",cacheFile);
                    PackageManager pm = getPackageManager();
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setPackage(packageName);
                    i.putExtra(Moji.EXTRA_MM, true);
                    i.putExtra(Intent.EXTRA_STREAM,uri);
                    i.setData(uri);
                    i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    i.putExtra(Moji.EXTRA_JSON, MojiModel.toJson(model).toString());
                    i.setType("image/*");
                    List<ResolveInfo> ris = pm.queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY);
                    if (ris.isEmpty()) {
                        Toast.makeText(getContext(), "App does not support sharing images. URL copied to clip board", Toast.LENGTH_LONG).show();
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("MakeMoji emoji", model.image_url);
                        clipboard.setPrimaryClip(clip);
                        return;
                    }
                    i.setPackage(ris.get(0).activityInfo.packageName);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(i);

                }
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {

                Toast.makeText(getContext(), "Load failed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };
    }
    Target t;
    @Override
    public void addMojiModel(MojiModel model, BitmapDrawable d) {
        t = getTarget(model);
        int size = MojiSpan.getDefaultSpanDimension(MojiSpan.BASE_TEXT_PX_SCALED);
        Moji.picasso.load(model.image_url).resize(size,size).into(t);
    }


    @Override
    public Context getContext() {
        return Moji.context;
    }

    @Override
    public int getPhraseBgColor() {
        return getResources().getColor(R.color._mm_default_phrase_bg_color);
    }
}