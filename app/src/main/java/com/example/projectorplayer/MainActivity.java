package com.example.projectorplayer;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    ExoPlayer player; PlayerView view; LinearLayout home; TextView status;
    String configUrl, directUrl;
    @Override public void onCreate(Bundle b) { super.onCreate(b);
        player = new ExoPlayer.Builder(this).build();
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(14,18,28));
        home = new LinearLayout(this); home.setOrientation(LinearLayout.VERTICAL); home.setPadding(35,20,35,20);
        TextView title=new TextView(this); title.setText("MY PROJECTOR PLAYER"); title.setTextSize(26); title.setTextColor(Color.WHITE); home.addView(title);
        status=new TextView(this); status.setText("Ready. Set your JSON URL or a direct video URL."); status.setTextColor(Color.LTGRAY); status.setTextSize(16); home.addView(status);
        addButton("▶ PLAY LATEST", ()->fetchAndPlay());
        addButton("⚙ SET REMOTE JSON URL", ()->editUrl(true));
        addButton("🔗 SET DIRECT VIDEO URL", ()->editUrl(false));
        addButton("🎧 AUDIO TRACK", ()->chooseTrack(C.TRACK_TYPE_AUDIO));
        addButton("CC SUBTITLE", ()->chooseTrack(C.TRACK_TYPE_TEXT));
        root.addView(home);
        view=new PlayerView(this); view.setPlayer(player); view.setUseController(true);
        root.addView(view,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        configUrl=getPreferences(0).getString("config",""); directUrl=getPreferences(0).getString("direct","");
        player.addListener(new Player.Listener() { @Override public void onPlayerError(PlaybackException e) { status.setText("Playback error: "+e.getMessage()); home.setVisibility(View.VISIBLE); } });
    }
    void addButton(String label,Runnable action){ Button b=new Button(this); b.setText(label); b.setAllCaps(false); home.addView(b); b.setOnClickListener(v->action.run()); }
    void editUrl(boolean remote){ EditText input=new EditText(this); input.setSingleLine(); input.setText(remote?configUrl:directUrl); input.setHint("http://...");
        new AlertDialog.Builder(this).setTitle(remote?"Remote JSON URL":"Direct video URL").setView(input).setPositiveButton("SAVE",(d,w)->{
            String s=input.getText().toString().trim(); if(remote){configUrl=s;getPreferences(0).edit().putString("config",s).apply();}else{directUrl=s;getPreferences(0).edit().putString("direct",s).apply();}status.setText("Saved. Press PLAY LATEST.");
        }).setNegativeButton("CANCEL",null).show(); }
    void fetchAndPlay(){
        if(configUrl.isEmpty()){ if(!directUrl.isEmpty())play(directUrl); else status.setText("Please set a URL first."); return; }
        status.setText("Fetching latest link...");
        new Thread(()->{ try {
            HttpURLConnection c=(HttpURLConnection)new URL(configUrl).openConnection(); c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setUseCaches(false);
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>65536)throw new IOException("JSON too large");}
                String u=new JSONObject(out.toString("UTF-8")).getString("url");runOnUiThread(()->play(u));
            } finally {c.disconnect();}
        } catch(Exception e){runOnUiThread(()->status.setText("Cannot fetch JSON: "+e.getMessage()));} }).start();
    }
    void play(String url){ if(!(url.startsWith("http://")||url.startsWith("https://"))){status.setText("Only HTTP/HTTPS supported");return;}
        status.setText("Playing: "+url);player.setMediaItem(MediaItem.fromUri(url));player.prepare();player.play();home.setVisibility(View.GONE);
    }
    void chooseTrack(int type){
        Tracks tracks=player.getCurrentTracks(); ArrayList<TrackSelectionOverride> overrides=new ArrayList<>();ArrayList<String> labels=new ArrayList<>();
        ArrayList<Tracks.Group> groups=new ArrayList<>();ArrayList<Integer> indices=new ArrayList<>();
        for(Tracks.Group group:tracks.getGroups()) if(group.getType()==type){ for(int i=0;i<group.length;i++) if(group.isTrackSupported(i)){
            Format f=group.getTrackFormat(i);String lang=f.language==null?"Unknown":f.language;
            labels.add(lang+" — "+(f.label==null?"Track "+(i+1):f.label));groups.add(group);indices.add(i);
        }}
        if(labels.isEmpty()){new AlertDialog.Builder(this).setMessage("No supported tracks available. Start a video first.").setPositiveButton("OK",null).show();return;}
        if(type==C.TRACK_TYPE_TEXT){labels.add(0,"Subtitles OFF");}
        new AlertDialog.Builder(this).setTitle(type==C.TRACK_TYPE_AUDIO?"Audio track":"Subtitle track").setItems(labels.toArray(new String[0]),(d,which)->{
            if(type==C.TRACK_TYPE_TEXT && which==0){player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setTrackTypeDisabled(type,true).build());return;}
            int ix=type==C.TRACK_TYPE_TEXT?which-1:which;
            Tracks.Group g=groups.get(ix);int track=indices.get(ix);
            player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setTrackTypeDisabled(type,false).clearOverridesOfType(type).addOverride(new TrackSelectionOverride(g.getMediaTrackGroup(),track)).build());
        }).show();
    }
    @Override public void onBackPressed(){if(home.getVisibility()==View.GONE){home.setVisibility(View.VISIBLE);player.pause();}else super.onBackPressed();}
    @Override protected void onDestroy(){player.release();super.onDestroy();}
}
