package com.makemoji.mojilib.model;


import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.support.annotation.WorkerThread;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.makemoji.mojilib.Moji;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Scott Baar on 1/9/2016.
 */
public class MojiModel {
    public int id;
 //   public String user_id;
 //   public String origin_id;
    public  String name;
    public String image_url;
    public String link_url;
 //   public String legacy;
//    public String deleted;
    public String created;
  //  public String access;
 //   public String username;
    public String flashtag;
 //   public int shares;
   // public int remoji;
  //  public int likes;
    public int gif;
    public String character;
    public int video;
    public String video_url;

    public WeakReference<Bitmap> bitmapRef;


    @SerializedName("native")
    public int _native;
    @SerializedName("40x40_url")
    public String fourtyX40Url;
    public int phrase;
    public List<MojiModel> emoji = new ArrayList<>();

    public MojiModel(){}
    public MojiModel(String name, String image_url){
        this.name = name;
        this.image_url = image_url;
        if (image_url!=null && image_url.toLowerCase().endsWith(".gif"))gif=1;
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof MojiModel)) return false;
        MojiModel m = (MojiModel) o;
        if (m.character!=null && !m.character.equals(character))return false;
        else if (character!=null && !character.equals(m.character))return false;
        if (m.image_url==null)return image_url==null;
        return  (m.image_url).equals(image_url);
    }
    public static JSONObject toJson(MojiModel m){
        if ( m==null || m.image_url==null||m.name==null)return null;//invalid object
        try{
            return new JSONObject(Moji.gson.toJson(m));
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;

    }
    public static MojiModel fromJson(JSONObject jo){
       return Moji.gson.fromJson(jo.toString(),MojiModel.class);
    }
    public static JSONArray toJsonArray(Collection<MojiModel> models){
        JSONArray ja = new JSONArray();
        if (models==null ||models.isEmpty())return ja;
        for (MojiModel m : models){
            JSONObject jo = toJson(m);
            if (jo!=null)ja.put(jo);
        }
        return ja;
    }
    public static List<MojiModel> fromJSONArray(JSONArray ja){
        List<MojiModel> list = new ArrayList<>();
        for (int i = 0; i <ja.length();i++){
            try {
                list.add(fromJson(ja.getJSONObject(i)));
            }
            catch (Exception e){e.printStackTrace();}
        }
        return list;
    }
    @Override
    public String toString(){
        return ""+name +(gif==1?" gif":"");
    }
    @WorkerThread
    public static void saveList(List<MojiModel> list,String name){
        SharedPreferences sp = Moji.context.getSharedPreferences("_mm_cached_lists4",0);
        sp.edit().putString(""+name,toJsonArray(list).toString()).apply();
    }
    @WorkerThread
    public static List<MojiModel> getList(String name){
        SharedPreferences sp = Moji.context.getSharedPreferences("_mm_cached_lists4",0);
        List<MojiModel> models;
        try{
            models = fromJSONArray(new JSONArray(sp.getString(name,"[]")));
        }catch (Exception e){
            models = new ArrayList<>();
            e.printStackTrace();
        }
        return models;
    }
    public boolean isNative(){return _native==1;}
    public boolean isPhrase(){return phrase==1;}
    public boolean isVideo(){return video==1;}
}
