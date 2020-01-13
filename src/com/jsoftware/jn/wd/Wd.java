package com.jsoftware.jn.wd;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import com.jsoftware.j.android.AndroidJInterface;
import com.jsoftware.j.android.JConsoleApp;
import com.jsoftware.jn.base.Util;
import com.jsoftware.jn.base.Utils;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.Character;
import java.lang.Math;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Wd
{

  static final String bintype="ghlmpsuvz";
  static public int nextId=1;

  JWdActivity activity;
  public String locale="";
  private Cmd cmd;
  private Child cc;
  Form form;
  Form evtform;
  private Font fontdef;
  private Font FontExtent;
  JIsigraph isigraph=null;
  JOpengl opengl=null;

  private Handler systimerHandler;
  private Runnable systimerRunnable;
  private long systimerInterval;
  List<Form>Forms;

  int FormSeq;
  int rc;
  private String lasterror="";
  public byte[] result;
  public int[] resultshape;
  public int[] intresult;
  public int[] intresultshape;
  public int gltarget=0;

  private String ccmd="";

  private int verbose;
  private String cmdstrparms="";
  private final String[] defChildStyle=new String[] {"flush"};
  final float dmdensity;

  private Context context;
  private JMb dialog;

  public Wd()
  {
//     DisplayMetrics displaymetrics = new DisplayMetrics();
//     pform.activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
//     screenHeight = displaymetrics.heightPixels;
//     screenWidth = displaymetrics.widthPixels;

    context=JConsoleApp.theApp.getApplicationContext();
    dmdensity=context.getResources().getDisplayMetrics().density;
    Forms=new ArrayList<Form>();
    dialog=new JMb(null);
    systimerRunnable = new Runnable() {
      @Override
      public void run() {
        if (0<systimerInterval) {
          systimerHandler.postDelayed(this, systimerInterval);
          JConsoleApp.theApp.jInterface.callJ("(i.0 0)\"_ sys_timer_z_$0", false);
        }
      }
    };

    _wdreset();
  }

// ---------------------------------------------------------------------
  public int uigl2(int t, int[] buf)
  {
    int rc=0;
    int cnt=buf.length;
    if (2345==t) {
// glwaitgl
      return rc = glwaitgl();
    } else if (2346==t) {
// glwaitnative
      return rc = glwaitnative();
    } else if (2035==t||2344==t) {
// glsel glsel2
      byte[] textbuf = new byte[cnt];
      for (int i=0; i<cnt; i++) textbuf[i] = (byte)buf[i];
      try {
        String g = (new String(textbuf, Charset.forName("UTF-8"))).trim();
        return rc = glsel(g);
      } catch (Exception exc) {
        Log.d(JConsoleApp.LogTag,"wd exception "+exc);
        glerror(t, exc.toString());
        return rc = 1;
      }
    } else if (2094==t) {
// glfontextent
      String textstring = Util.int2String(buf, 0, cnt);
      if (0==textstring.length())
        FontExtent=null;
      else {
        Font fnt = new Font(textstring);
        if (fnt.error) {
          glerror(t, "command failed");
          return rc = 1;
        } else
          FontExtent=fnt;
      }
      return rc = 0;
    } else if (2057==t) {
// glqextent
      String textstring = Util.int2String(buf, 0, cnt);
      int[] wh = Glcmds.qextent(textstring);
      intresult=new int[] {wh[0], wh[1],};
      intresultshape=new int[] {4,-1,-1};
      return rc = -1;
    } else if (2083==t) {
// glqextentw
      int[] glresult;
      String textstring = Util.int2String(buf, 0, cnt);
      int[] ws = Glcmds.qextentw(textstring.split("\n"));
      if (0<ws.length) {
        glresult=new int[ws.length];
        for (int j=0; j<ws.length-1; j++)
          glresult[j]=ws[j];
      } else {
        glresult=new int[0];
      }
      intresult=glresult;
      intresultshape=new int[] {4,-1,-1};
      return rc = -1;
    }
    int[] buf1=new int[2+buf.length];
    buf1[0]=2+buf.length;
    buf1[1]=t;
    System.arraycopy(buf, 0, buf1, 2, buf.length);

    if (gltarget==1) {
      if (null==JConsoleApp.theWd.opengl) {
        glerror(t, "no target selected");
        return rc = 1;
      }
      return rc = JConsoleApp.theWd.opengl.glcmds.uiglcmds(buf1);
    } else if (gltarget==0) {
      if (null==JConsoleApp.theWd.isigraph) {
        glerror(t, "no target selected");
        return rc = 1;
      }
      return rc = JConsoleApp.theWd.isigraph.glcmds.uiglcmds(buf1);
    } else {
      glerror(t, "no target selected");
      return rc = 1;
    }
  }

// ---------------------------------------------------------------------
  int glwaitgl()
  {
    if (null==opengl) return 0;
    opengl.glwaitgl();
    return 0;
  }

// ---------------------------------------------------------------------
  int glwaitnative()
  {
    if (null==opengl) return 0;
    opengl.glwaitnative();
    return 0;
  }

// ---------------------------------------------------------------------
  int glsel(String p)
  {
    Log.d(JConsoleApp.LogTag,"glsel p: "+p);
    if (p.isEmpty()) return 1;
    if (0==Forms.size()) return 1;
    if (Util.isInteger(p)) {
      long n= Util.c_strtol(p);
      for (Form f : Forms) {
        if (!f.closed) {
          for (Child c : f.children) {
            if (c.widget==null)
              Log.d(JConsoleApp.LogTag,"glsel a1 "+c.id+" "+c.type);
            else
              Log.d(JConsoleApp.LogTag,"glsel a1 "+c.id+" "+c.type+" "+c.widget.getId());
            if ((c.type.equals("isigraph")||c.type.equals("isidraw")||c.type.equals("opengl")||c.type.equals("isiprint")) &&
                ((c.widget==null &&  n==c.handle) || (c.widget!=null &&  n==c.widget.getId()))) {
              if (c.type=="isigraph"||c.type=="isidraw") {
                this.isigraph=(JIsigraph)c;
                this.gltarget=0;
              } else if (c.type=="opengl") {
                this.opengl=(JOpengl)c;
                this.gltarget=1;
              } else if (c.type=="isiprint") {
//                this.isiprint=(JIsiprint)c;
                this.gltarget=2;
              }
//                form=f;
//                form.child=c;
              Log.d(JConsoleApp.LogTag,"glsel "+form.id+" "+c.id+" "+c.widget.getId());
              return 0;
            }
          }
        }
      }
    } else {
// MUST search current form first
      if ((null!=form) && (!form.closed)) {
        for (Child c : form.children) {
          if ((c.type.equals("isigraph")||c.type.equals("isidraw")||c.type.equals("opengl")||c.type.equals("isiprint")) &&
              c.id.equals(p)) {
            if (c.type=="isigraph"||c.type=="isidraw") {
              this.isigraph=(JIsigraph)c;
              this.gltarget=0;
            } else if (c.type=="opengl") {
              this.opengl=(JOpengl)c;
              this.gltarget=1;
            } else if (c.type=="isiprint") {
//                this.isiprint=(JIsiprint)c;
              this.gltarget=2;
            }
//              form.child=c;
            Log.d(JConsoleApp.LogTag,"glsel "+form.id+" "+c.id+" "+c.widget.getId());
            return 0;
          }
        }
      }
      for (Form f : Forms) {
        if (!f.closed) {
          for (Child c : f.children) {
            if ((c.type.equals("isigraph")||c.type.equals("isidraw")||c.type.equals("opengl")||c.type.equals("isiprint")) &&
                c.id.equals(p)) {
              if (c.type=="isigraph"||c.type=="isidraw") {
                this.isigraph=(JIsigraph)c;
                this.gltarget=0;
              } else if (c.type=="opengl") {
                this.opengl=(JOpengl)c;
                this.gltarget=1;
              } else if (c.type=="isiprint") {
//                this.isiprint=(JIsiprint)c;
                this.gltarget=2;
              }
//              form.child=c;
              Log.d(JConsoleApp.LogTag,"glsel "+form.id+" "+c.id+" "+c.widget.getId());
              return 0;
            }
          }
        }
      }
    }
    Log.d(JConsoleApp.LogTag,"glsel failed");
    glerror(2035, "glsel failed");
    return 1;
  }

// ---------------------------------------------------------------------
  public int wd(int t, int[] inta, Object[] inarr, Object[] res, String loc)
  {
    final int tt = t;
    final int[] intas = inta;
    final Object[] inarrs = inarr;
    final Object[] ress = {null,new int[]{2,-1,-1}};
    final String sloc = loc;
    final Integer[] rcs = {0};
    if (t>=4000) {
      int rc = uiwd(tt, intas, inarrs, ress, sloc, rcs);
    } else {
      final CountDownLatch latch = new CountDownLatch(1);
      JConsoleApp.theApp.activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          int rc = uiwd(tt, intas, inarrs, ress, sloc, rcs);
          latch.countDown();
        }
      });
      try {
        latch.await();
      } catch (InterruptedException e) {
        error(Log.getStackTraceString(e));
      }
    }
    res[0]=ress[0];
    res[1]=ress[1];
    return rcs[0].intValue();
  }

// ---------------------------------------------------------------------
  public int uiwd(int t, int[] inta, Object[] inarr, Object[] res, String loc, Integer[] rcs)
  {
    rc=0;
    locale = loc;
    if (t!=0) Log.d(JConsoleApp.LogTag,"wd type: "+t);
    if (t==0) {
      result=null;
      resultshape=new int[] {2,-1,-1};
      String s;
      if (inta[0]==2) {
        try {
          s = new String((byte[])inarr[0], Charset.forName("UTF-8"));
        } catch (Exception exc) {
          Log.d(JConsoleApp.LogTag,"wd exception "+exc);
          glerror(t, exc.toString());
          return rcs[0] = 1;
        }
      } else {
        error("argument error");
        return rcs[0] = 1;
      }
      Log.d(JConsoleApp.LogTag,"wd cmd: "+s);
      cmd=new Cmd();
      cmd.init(s);
      _wd();
      if (rc<0) {
        res[0]=result;
        res[1]=resultshape;
      }
      return rcs[0] = rc;
    } else if (t>=1000 && t<=2999) {
      intresult=null;
      intresultshape=new int[] {4,-1,-1};
      int[] buf;
      if (inta[0]==4) {
        buf = (int[])inarr[0];
      } else if (inta[0]==2) {
        byte[] bytea = (byte[]) inarr[0];
        buf =  new int[bytea.length];
        for (int i=0; i<bytea.length; i++) buf[i]=bytea[i];
      } else {
        glerror(t, "argument error");
        return rcs[0] = 1;
      }
      rc = uigl2(t, buf);
      if (rc<0) {
        res[0]=intresult;
        res[1]=intresultshape;
      }
      return rcs[0] = rc;
    } else if (t==3000) {
      if (inarr.length!=1 ||inta[0]!=2) {
        glerror(t, "argument error");
        return rcs[0] = 1;
      }
      try {
        String fname = new String((byte[])inarr[0], Charset.forName("UTF-8"));
        rc = JBitmap.glreadimg(t, fname, res);
      } catch (Exception exc) {
        Log.d(JConsoleApp.LogTag,"wd exception "+exc);
        glerror(t, exc.toString());
        return rcs[0] = rc;
      }
      if (0<rc) glerror(t, "command failed");
      return rcs[0] = rc;
    } else if (t==3001) {
      if (inarr.length!=1 ||inta[0]!=2) {
        glerror(t, "argument error");
        return rcs[0] = 1;
      }
      byte[] buf = (byte[])inarr[0];
      try {
        rc = JBitmap.glgetimg(t, buf, res);
      } catch (Exception exc) {
        Log.d(JConsoleApp.LogTag,"wd exception "+exc);
        glerror(t, exc.toString());
        return rcs[0] = rc;
      }
      if (0<rc) glerror(t, "command failed");
      return rcs[0] = rc;
    } else if (t==3002) {
      if (inarr.length<2 || inarr.length>4 || inta[0]!=4 || inta[3]!=2) {
        glerror(t, "argument error");
        return rcs[0] = 1;
      }
      if (inarr.length>2 && inta[6]!=2) {
        glerror(t, "argument error");
        return rcs[0] = 1;
      }
      if (inarr.length>3 && inta[9]!=4) {
        glerror(t, "argument error");
        return rcs[0] = 1;
      }
      int[] buf = (int[])inarr[0];
      String imgfmt = "";
      int[] q=new int[] {-1};
      try {
        String fname = new String((byte[])inarr[1], Charset.forName("UTF-8"));
        if (inarr.length>2) imgfmt = new String((byte[])inarr[2], Charset.forName("UTF-8"));
        if (inarr.length>3) q=(int[])inarr[3];
        rc = JBitmap.glwriteimg(t, buf, inta[2], inta[1], fname, imgfmt, (q.length>0)?q[0]:-1);
      } catch (Exception exc) {
        Log.d(JConsoleApp.LogTag,"wd exception "+exc);
        glerror(t, exc.toString());
        return rcs[0] = 1;
      }
      if (0<rc) glerror(t, "command failed");
      return rcs[0] = rc;
    } else if (t==3003) {
      if (inarr.length<2 || inarr.length>3 || inta[0]!=4 || inta[3]!=2) {
        glerror(t, "argument error");
        return rcs[0] = 1;
      }
      if (inarr.length>2 && inta[6]!=4) {
        glerror(t, "argument error");
        return rcs[0] = 1;
      }
      int[] buf = (int[])inarr[0];
      int[] q=new int[] {-1};
      try {
        String imgfmt = new String((byte[])inarr[1], Charset.forName("UTF-8"));
        if (inarr.length>2) q=(int[])inarr[2];
        rc = JBitmap.glputimg(t, buf, inta[2], inta[1], imgfmt, (q.length>0)?q[0]:-1, res);
      } catch (Exception exc) {
        Log.d(JConsoleApp.LogTag,"wd exception "+exc);
        glerror(t, exc.toString());
        return rcs[0] = 1;
      }
      if (0<rc) glerror(t, "command failed");
      return rcs[0] = rc;
    } else if (t==4000) {
      if (!(inarr.length>0&&inarr.length<=3)) {
        glerror(t, "length error");
        return rcs[0] = 1;
      }
      try {
        if (inarr.length==1&&inta[0]==2) {
          String url = new String((byte[])inarr[0], Charset.forName("UTF-8"));
          rc = JHttpReq.httpreq("get", url, null, res);
        } else if (inarr.length>1&&inta[0]==2&&inta[3]==2) {
          String req = new String((byte[])inarr[0], Charset.forName("UTF-8"));
          String url = new String((byte[])inarr[1], Charset.forName("UTF-8"));
          if(!(req.equals("get")||req.equals("post")||req.equals("put"))) {
            glerror(t, "argument error");
            return rcs[0] = 1;
          }
          if(req.equals("get"))
            rc = JHttpReq.httpreq(req, url, null, res);
          else {
            if(!(inarr.length==3&&inta[6]==2)) {
              glerror(t, "argument error");
              return rcs[0] = 1;
            }
            rc = JHttpReq.httpreq(req, url, (byte[])inarr[2], res);
          }
        } else {
          glerror(t, "argument error");
          return rcs[0] = 1;
        }
      } catch (Exception exc) {
        Log.d(JConsoleApp.LogTag,"wd exception "+exc);
        glerror(t, exc.toString());
        return rcs[0] = rc;
      }
      if (0<rc) glerror(t, "command failed");
      return rcs[0] = rc;
    } else {
      Log.d(JConsoleApp.LogTag,"not implemented");
      glerror(t, "not implemented");
      return rcs[0] = 1;
    }
  }

// ---------------------------------------------------------------------
// subroutines may set int rc and _wd returns if non-zero
  private void _wd()
  {
    String c;
    while ((rc==0) && cmd.more()) {
      c=cmd.getid();
      if (c.isEmpty()) continue;
      ccmd=c;
      if ((0!=verbose) && !c.equals("qer")) {
        cmd.markpos();
        cmdstrparms=c + " " + cmd.getparms();
        cmd.rewindpos();
        if (2==verbose || 3==verbose) {
          StringBuilder indent= new StringBuilder();
          if ((null!=form) && (null!=form.pane)) indent.append(new String(new char[2*Math.max(0,form.pane.layouts.size()-1)]).replace("\0", " "));
//          if (2==verbose && (null!=tedit) && Utils.ShowIde) tedit.append_smoutput("wd command: " + indent.toString()+cmdstrparms);
          if (3==verbose) Util.qDebug ( "wd command: " + indent.toString()+cmdstrparms);
        }
      }
      if (c.equals("activity"))
        wdactivity();
      else if (c.equals("dm"))
        wddm();
      else if (c.equals("q"))
        wdq();
      else if (c.equals("beep"))
        wdbeep();
      else if (c.equals("bin"))
        wdbin(false);
      else if (c.equals("binx"))
        wdbin(true);
      else if (c.equals("cc"))
        wdcc();
      else if (c.equals("clipcopy"))
        wdclipcopy();
      else if (c.equals("clippaste"))
        wdclippaste();
      else if (c.equals("cmd"))
        wdcmd();
      else if (c.equals("cn"))
        wdcn();
//    else if (c.equals("defprint"))
//      wddefprint();
//    else if (c.equals("dirmatch"))
//      wddirmatch();
      else if (c.equals("end"))
        wdend();
      else if (c.equals("fontdef"))
        wdfontdef();
      else if (c.equals("fontfile"))
        wdfontfile();
      else if (c.equals("get"))
        wdget();
      else if (c.equals("getj"))
        wdgetj();
      else if (c.equals("getp"))
        wdgetp();
      else if (c.equals("grid"))
        wdgrid();
      else if (c.equals("ide"))
        wdide();
      else if (c.equals("immexj"))
        wdimmexj();
      else if (c.startsWith("line"))
        wdline(c);
      else if (c.startsWith("mb"))
        wdmb();
      else if (c.startsWith("menu"))
        wdmenu(c);
      else if (c.equals("msgs"))
        wdmsgs();
      else if (c.equals("openj"))
        wdopenj();
      else if (c.equals("NB."))
        wdnb();
      else if (c.charAt(0)=='p')
        wdp(c);
      else if (c.charAt(0)=='q')
        wdqueries(c);
      else if (c.equals("rem"))
        wdrem();
      else if (c.equals("reset"))
        wdreset();
      else if (c.equals("set"))
        wdset();
      else if (c.equals("setj"))
        wdsetj();
      else if (c.equals("setp"))
        wdsetp();
      else if (c.startsWith("set"))
        wdsetx(c);
      else if (c.startsWith("sm"))
        wdsm(c);
      else if (c.equals("textview"))
        wdtextview();
      else if (c.equals("timer"))
        wdtimer();
      else if (c.equals("verbose"))
        wdverbose();
      else if (c.equals("version"))
        wdversion();
      else if (c.equals("wt"))
        wdwt();
      else if (c.equals("wh"))
        wdwh();
      else if (c.equals("wwh"))
        wdwwh();
      else if (c.equals("minwh"))
        wdminwh();
      else if (false) {
        wdnotyet();
      } else
        error("command not found");
    }
  }

// ---------------------------------------------------------------------
  private void wdactivity()
  {
    String bb="default";
    String fullscreen="no";
    String p=cmd.getparms();
    String[] opt=Cmd.qsplit(p);
    String[] n=Util.qsless(p.split(" "),new String[] {""});   //,String::SkipEmptyParts);
    if (0==n.length) {
      error("activity requires a locale.");
      return;
    } else if (1<n.length) {
      if (Util.sacontains(opt,"fs")) fullscreen="yes";
      if (Util.sacontains(opt,"ask")) bb="ask";
      if (Util.sacontains(opt,"never")) bb="never";
    }
    Log.d(JConsoleApp.LogTag,"activity "+p);
    Intent intent = new Intent();
    intent.setClass(context, JWdActivity.class);
    intent.putExtra("jlocale", n[0]);
    intent.putExtra("fullscreen", fullscreen);
    intent.putExtra("backbuttonAct", bb);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
    Log.d(JConsoleApp.LogTag,"startActivity");
    context.startActivity(intent);
  }

// ---------------------------------------------------------------------
  void wdactivateform()
  {
    if (null!=form) {
      form.setVisibility(View.VISIBLE);
    } else if (0==Forms.size()) {
      if (Utils.ShowIde) {
      }
    }
  }

// ---------------------------------------------------------------------
  private void wdbeep()
  {
    cmd.getparms();
//  QApplication::beep();
  }

// ---------------------------------------------------------------------
  private void wdbin(boolean withid)
  {
    String n="";
    if (withid)
      n=cmd.getid();
    String p=Util.remquotes(cmd.getparms());
    if (noform()) return;
    form.pane.bin(p,n);
  }

// ---------------------------------------------------------------------
  private void wdcc()
  {
    if (noform()) {
      cmd.getparms();
      return;
    }
    String c,n,p;
    n=cmd.getid();
    c=cmd.getid();
    p=cmd.getparms();
    form.pane.addchild(n,c,p);
  }

// ---------------------------------------------------------------------
  private void wdclipcopy()
  {
    String p=cmd.getparms();
    Util.clipcopy(context,p);
  }

// ---------------------------------------------------------------------
  private void wdclippaste()
  {
    String p=cmd.getparms();
    int len=-1;
    String m=Util.clipread(context);
    if (0!=m.length()) {
      rc=-1;
      result= Util.s2ba(m.replace("\u00a0"," "));
    } else if (p.equals("1"))
      error("clipboard is empty");
    else {
      rc=-1;
      result= new byte[0];
    }
  }

// ---------------------------------------------------------------------
  private void wdcmd()
  {
    String n=cmd.getid();
    String p=cmd.getid();
    String v=cmd.getparms();
    int type=setchild(n);
    if (0!=type)
      cc.cmd(p,v);
    else
      error("bad child id: " + n);
  }

// ---------------------------------------------------------------------
  private void wdcn()
  {
    String p=cmd.getparms();
    if (noform()) return;
    cc=form.child;
    if (nochild()) return;
    if (cc.type.equals("togglebutton")||cc.type.equals("switch"))
      cc.set("texts",p);
    else
      cc.set("caption",Util.remquotes(p));
  }

// ---------------------------------------------------------------------
// private void wddirmatch()
// {
//   String p=cmd.getparms();
//   String[] f=Cmd.qsplit(p);
//   if (f.size()!=2) {
//     error("dirmatch requires 2 directories");
//     return;
//   }
//   dirmatch(f[0].Util.c_str(),f[1].Util.c_str());
// }

// ---------------------------------------------------------------------
  private void wdend()
  {
    cmd.end();
  }

// ---------------------------------------------------------------------
  private void wdfontdef()
  {
    String p=cmd.getparms();
    if (!p.isEmpty()) {
      fontdef=null;
    } else {
      Font fnt = new Font(p);
      if (fnt.error) {
        error("fonddef command failed: "+ p);
      } else
        fontdef=fnt;
    }
  }

// ---------------------------------------------------------------------
  private void wdfontfile()
  {
    String p=Util.remquotes(cmd.getparms());
//  int id=QFontDatabase::addApplicationFont(p);
    int id=0;
    result=Util.s2ba(Util.i2s(id));
    rc=-1;
  }

// ---------------------------------------------------------------------
  private void wdget()
  {
    String n=cmd.getid();
    String p=cmd.getid();
    String v=cmd.getparms();
    rc=-1;
    if (n.equals("_")) n=formchildid();
    int type=setchild(n);
    if (0!=type)
      result=cc.get(p,v);
    else
      error("bad child id: " + n);
  }

// ---------------------------------------------------------------------
  private void wdgetj()
  {
    String p=cmd.getid();
    String v=cmd.getparms();
    rc=-1;
    result=JConsoleApp.theApp.getpref(p,v);
    return;
  }

// ---------------------------------------------------------------------
  private void wdgetp()
  {
    String p=cmd.getid();
    String v=cmd.getparms();
    rc=-1;
    if (noform()) return;
    result=form.get(p,v);
    return;
  }

// ---------------------------------------------------------------------
  private void wdgrid()
  {
    if (noform()) {
      cmd.getparms();
      return;
    }
    String n=cmd.getid();
    String v=cmd.getparms();
    form.pane.grid(n,v);
  }

// ---------------------------------------------------------------------
  private void wdide()
  {
    String p=Util.remquotes(cmd.getparms());
//   if (p.equals("hide"))
//     showide(false);
//   else if (p.equals("show"))
//     showide(true);
//   else
//     error("unrecognized command: ide " + p);
  }

// ---------------------------------------------------------------------
  private void wdimmexj()
  {
    String p=Util.remquotes(cmd.getparms());
    JConsoleApp.theApp.jInterface.callJ("(i.0 0)\"_ " + p, false);
  }

// ---------------------------------------------------------------------
  void wdline(String c)
  {
    String p=cmd.getparms();
    if (noform()) return;
    if (!form.pane.line(c,p))
      error("unrecognized command: " + c + " " + p);
  }

// ---------------------------------------------------------------------
  private void wdmb()
  {
    String c=cmd.getid();
    String p=cmd.getparms(true);
    if (null!=form)
      form.dialog.mb(c,p);
    else
      dialog.mb(c,p);
  }

// ---------------------------------------------------------------------
  private void wdmenu(String s)
  {
    int rc=0;
    if (noform()) {
      cmd.getparms();
      return;
    }
    if (null==form.menubar)
      form.addmenu();
    String c,p;
    if (s.equals("menu")) {
      c=cmd.getid();
      p=cmd.getparms();
      rc=form.menubar.menu(c,p);
    } else if (s.equals("menupop")) {
      p=Util.remquotes(cmd.getparms());
      rc=form.menubar.menupop(p);
    } else if (s.equals("menupopz")) {
      p=cmd.getparms();
      rc=form.menubar.menupopz();
    } else if (s.equals("menusep")) {
      p=cmd.getparms();
      rc=form.menubar.menusep();
    } else {
      p=cmd.getparms();
      error("menu command not found");
    }
    if (0!=rc) error("menu command failed");
  }

// ---------------------------------------------------------------------
  private void wdmsgs()
  {
    String p=cmd.getparms();
    if (!p.isEmpty()) {
      error("extra parameters: " + p);
      return;
    }
//  QApplication::processEvents(QEventLoop::AllEvents);
  }

// ---------------------------------------------------------------------
  private void wdnb()
  {
    cmd.getline();
  }

// ---------------------------------------------------------------------
// not yet
  private void wdnotyet()
  {
    cmd.getparms();
  }

// ---------------------------------------------------------------------
  private void wdopenj()
  {
    File f = new File(Util.remquotes(cmd.getparms()));
    Intent intent = new Intent();
    intent.setClass(context, com.jsoftware.j.android.EditActivity.class);
    intent.setData(Uri.parse("file://" + f.getAbsolutePath()));
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
//      theApp.setCurrentDirectory(f.getParentFile());

    context.startActivity(intent);
  }

// ---------------------------------------------------------------------
  private void wdp(String c)
  {
    if (c.equals("pactive"))
      wdpactive();
    else if (c.equals("pas"))
      wdpas();
    else if (c.equals("pc"))
      wdpc();
    else if (c.equals("pclose"))
      wdpclose();
    else if (c.equals("pcenter"))
      wdpcenter();
    else if (c.equals("picon"))
      wdpicon();
    else if (c.equals("pmove"))
      wdpmove();
    else if (c.equals("pn"))
      wdpn();
    else if (c.equals("psel"))
      wdpsel();
    else if (c.equals("pshow"))
      wdpshow();
    else if (c.equals("ptimer"))
      wdptimer();
    else if (c.equals("ptop"))
      wdptop();
    else
      error("parent command not found: " + c);
  }

// ---------------------------------------------------------------------
  private void wdpactive()
  {
    String p=cmd.getparms();
    if (!p.isEmpty()) {
      error("extra parameters: " + p);
      return;
    }
    if (noform()) return;
    if(form!=Forms.get(Forms.size()-1)) return;
    form.activateWindow();
    form.raise();
  }

// ---------------------------------------------------------------------
  private void wdpas()
  {
    String p=cmd.getparms();
    if (noform()) return;
    String[] n=Util.qsless(p.split(" "),new String[] {""});   //,String::SkipEmptyParts);
    int l,t,r,b;
    if (n.length==2) {
      l=r=Util.c_strtoi(n[0]);
      t=b=Util.c_strtoi(n[1]);
      form.setpadding(l,t,r,b);
    } else if (n.length==4) {
      l=Util.c_strtoi(n[0]);
      t=Util.c_strtoi(n[1]);
      r=Util.c_strtoi(n[2]);
      b=Util.c_strtoi(n[3]);
      form.setpadding(l,t,r,b);
    } else
      error("pas requires 2 or 4 numbers: " + p);
  }

// ---------------------------------------------------------------------
  private void wdpc()
  {
    String c=cmd.getid();
    String p=cmd.getparms();
// View must be parentless to be top-level window
    String[] m=Util.qsless(p.split(" "),new String[] {""});   //,String::SkipEmptyParts);
    if (null==activity || null!=activity.form) {
      error("pc requires a new activity " + p);
      return;
    }
    Form f=new Form(c,p,activity,Util.sacontains(m,"owner")?form:null);
    if (rc==1) {
      f.dispose();
      return;
    }
    evtform=form=f;
    Forms.add(form);
  }

// ---------------------------------------------------------------------
  private void wdpcenter()
  {
    String p=cmd.getparms();
    if (!p.isEmpty()) {
      error("extra parameters: " + p);
      return;
    }
    if (noform()) return;
  }

// ---------------------------------------------------------------------
  private void wdpclose()
  {
    String p=cmd.getparms();
    if (!p.isEmpty()) {
      error("extra parameters: " + p);
      return;
    }
    if (noform()) return;
    if (form.closed) return;
    form.closed=true;
    form.close();
  }

// ---------------------------------------------------------------------
  private void wdpicon()
  {
    String p=Util.remquotes(cmd.getparms());
    if (noform()) return;
    form.setpicon(p);
  }

// ---------------------------------------------------------------------
  private void wdpmove()
  {
    String p=cmd.getparms();
    if (noform()) return;
    String[] n=Util.qsless(p.split(" "),new String[] {""});   //,String::SkipEmptyParts);
    if (n.length!=4)
      error("pmove requires 4 numbers: " + p);
    else {
      if (Util.c_strtoi(n[2])!=-1 && Util.c_strtoi(n[3])!=-1)
        form.resize(Util.c_strtoi(n[2]),Util.c_strtoi(n[3]));
    }
  }

// ---------------------------------------------------------------------
  private void wdpn()
  {
    String p=Util.remquotes(cmd.getparms());
    if (noform()) return;
    form.setpn(p);
  }

// ---------------------------------------------------------------------
  private void wdpsel()
  {
    String p=cmd.getparms();
    if (p.isEmpty()) {
      form=null;
      return;
    }
    if (Util.isInteger(p)) {
      long n= Util.c_strtol(p);
      for (Form f : Forms) {
        if ((!f.closed) && n==f.getId()) {
          form=f;
          return;
        }
      }
    } else {
      for (Form f : Forms) {
        if ((!f.closed) && f.id.equals(p)) {
          form=f;
          return;
        }
      }
    }
    error("command failed: psel");
  }

// ---------------------------------------------------------------------
  private void wdpshow()
  {
    String p=Util.remquotes(cmd.getparms());
    if (noform()) return;
    form.showit(p);
  }

// ---------------------------------------------------------------------
  private void wdptimer()
  {
    String p=Util.remquotes(cmd.getparms());
    if (noform()) return;
    form.settimer(p);
  }

// ---------------------------------------------------------------------
  private void wdptop()
  {
    String p=Util.remquotes(cmd.getparms());
    if (noform()) return;
    if(form!=Forms.get(Forms.size()-1)) return;
//   Qt::WindowFlags f=form.windowFlags();
//   if (p.equals("1"))
//     f|=Qt::WindowStaysOnTopHint;
//   else
//     f=f&~Qt::WindowStaysOnTopHint;
//   form.setWindowFlags(f);
    form.show();
  }

// ---------------------------------------------------------------------
  private void wddm()
  {
    String p=cmd.getparms();
    if (!p.isEmpty()) {
      error("extra parameters: " + p);
      return;
    }
    rc=-1;
    DisplayMetrics dm=context.getResources().getDisplayMetrics();
    result=Util.s2ba(Util.d2s((double)dm.density)+" "+
                     Util.d2s((double)dm.scaledDensity)+" "+
                     Util.i2s(dm.densityDpi)+" "+
                     Util.i2s(dm.widthPixels)+" "+
                     Util.i2s(dm.heightPixels)+" "+
                     Util.d2s((double)dm.xdpi)+" "+
                     Util.d2s((double)dm.ydpi));
    return;
  }

// ---------------------------------------------------------------------
  private void wdq()
  {
    String p=cmd.getparms();
    if (!p.isEmpty()) {
      error("extra parameters: " + p);
      return;
    }
    wdstate(evtform,1);
  }

// ---------------------------------------------------------------------
  private void wdqtstate(String p)
  {
    rc=-2;
//  result=qtstate(p);
  }

// ---------------------------------------------------------------------
  private void wdqueries(String s)
  {
    String p=cmd.getparms();

    if (!(p.isEmpty()) && (s.equals("qd") || s.equals("qverbose") ||s.equals("qopenglmod") || s.equals("qscreen") || s.equals("qwd") || s.equals("qosver") || s.equals("qprinters") || s.equals("qpx") || s.equals("qhwndp") || s.equals("qform"))) {
      error("extra parameters: " + p);
      return;
    }

    if (s.equals("qd")) {
      wdstate(form,0);
      return;
    }

    if (s.equals("qtstate")) {
      wdqtstate(p);
      return;
    }

    rc=-1;
    if (s.equals("qer")) {
      if (0!=verbose)
        result=Util.s2ba(lasterror + "\012" + cmdstrparms);
      else result=Util.s2ba(lasterror);
//      if (2==verbose && (null!=tedit) && Utils.ShowIde) tedit.append_smoutput("wd **error: " + lasterror);
      if (3==verbose) Util.qDebug("wd **error: " + lasterror);
      return;
    }
// queries that form not needed
    if (s.equals("qverbose")) {
      result=Util.s2ba(Util.i2s(verbose));
      return;
    } else if (s.equals("qopenglmod")) {
      error("command not found");
      return;
    } else if (s.equals("qscreen")) {
      Point point = new Point();
      float dpix,dpiy;
      int w,h;
      DisplayMetrics dm = new DisplayMetrics();
      Display display = JConsoleApp.theApp.activity.getWindowManager().getDefaultDisplay();
      if (Build.VERSION.SDK_INT >= 17) {
        //get real metrics
        display.getRealMetrics(dm);
        w = dm.widthPixels;
        h = dm.heightPixels;
      } else if (Build.VERSION.SDK_INT >= 14) {
        display.getMetrics(dm);
        //reflection for this weird in-between time
        try {
          Method mGetRawH = Display.class.getMethod("getRawHeight");
          Method mGetRawW = Display.class.getMethod("getRawWidth");
          w = (Integer) mGetRawW.invoke(display);
          h = (Integer) mGetRawH.invoke(display);
        } catch (Exception e) {
          //this may not be 100% accurate, but it's all we've got
          w = display.getWidth();
          h = display.getHeight();
        }
      } else {
        display.getMetrics(dm);
        //This should be close, as lower API devices should not have window navigation bars
        w = display.getWidth();
        h = display.getHeight();
      }
      dpix = dm.xdpi;
      dpiy = dm.ydpi;
      int mmx=(int)(25.4*(float)w/dpix);
      int mmy=(int)(25.4*(float)h/dpiy);
      int dia=(int)Math.sqrt(dpix*dpix+dpiy*dpiy);
      result=Util.s2ba(Util.i2s(mmx) + " " + Util.i2s(mmy) + " " + Util.i2s(w) + " " + Util.i2s(h) + " " + Util.i2s((int)dpix) + " " + Util.i2s((int)dpiy) + " " + Util.i2s(1) + " 1 " + Util.i2s(24) + " " + Util.i2s((int)dpix) + " " + Util.i2s((int)dpiy) + " " + Util.i2s(dia));
      return;
    } else if (s.equals("qwd")) {
      result=Util.s2ba("android");
      return;
    } else if (s.equals("qosver")) {
      result=Util.s2ba(Util.i2s(Build.VERSION.SDK_INT));
      return;
    } else if (s.equals("qprinters")) {
      String q="";
      result=Util.s2ba(q);
      return;
    } else if (s.equals("qpx")) {
      if (0==Forms.size()) result=new byte[0];
      else {
        StringBuilder q=new StringBuilder();
        for (Form f : Forms)
          q.append(f.id + "\t" + f.hsform() + "\t" + f.locale + "\t\t" + Util.i2s(f.seq) + "\t\012");
        result=Util.s2ba(q.toString());
      }
      return;
    } else if (s.equals("qfile")) {
      boolean done=false;
      String path=Util.remquotes(p);
      try {
        byte buff[] = new byte[8092];
        InputStream in = JConsoleApp.theApp.activity.getAssets().open(path);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        int n;
        while ((n = in.read(buff)) != -1) {
          out.write(buff, 0, n);
        }
        done=true;
        out.close();
        in.close();
        if (!done) result=out.toByteArray();
      } catch (IOException exc) {
        error(Log.getStackTraceString(exc));
      };
      return;
    }
// queries that form is needed
    if (noform()) return;
    if (s.equals("qhwndp"))
      result=Util.s2ba(form.hsform());
    else if (s.equals("qform"))
      result=form.qform();
// queries expecting parameter
    else if (p.isEmpty())
      error("missing parameters");
    else if (s.equals("qhwndc")) {
      Child cc;
      if (p.equals("_")) p=formchildid();
      if (null!=(cc=form.id2child(p)))
        result=Util.s2ba(Util.i2s((null!=cc.widget)?cc.widget.getId():0));
      else
        error("no child selected: " + p);
      if (rc==-1)
        Log.d(JConsoleApp.LogTag,"child hwnd "+cc.id+" "+Util.i2s((null!=cc.widget)?cc.widget.getId():0));
      else
        Log.d(JConsoleApp.LogTag,"child hwnd error "+p);
      if (rc!=1) form.child=cc;
    } else if (s.equals("qchildxywh")) {
      Child cc;
      if (p.equals("_")) p=formchildid();
      if (null!=(cc=form.id2child(p)) && null!=cc.widget) {
//       QPoint pos=cc.widget.mapTo(cc.widget.window(),cc.widget.pos());
//       QSize size=cc.widget.size();
//       result=Util.s2ba(Util.i2s(pos.x())+" "+Util.i2s(pos.y())+" "+Util.i2s(size.width())+" "+Util.i2s(size.height()));
        result=Util.s2ba("0 0 0 0");
      } else
        error("TODO");
      if (rc!=1) form.child=cc;
    } else
      error("command not found");
  }

// ---------------------------------------------------------------------
  private void wdrem()
  {
    cmd.getparms();
  }

// ---------------------------------------------------------------------
  private void wdreset()
  {
    String p=cmd.getparms();
    if (!p.isEmpty()) {
      error("extra parameters: " + p);
      return;
    }
    _wdreset();
  }
// ---------------------------------------------------------------------
  private void _wdreset()
  {
    systimerInterval=0;
    if (null!=systimerHandler) systimerHandler.removeCallbacks(systimerRunnable);

    for (Form f : Forms) {
      f.closed=true;
      f.close();
    }
    Forms.clear();
    form=null;
    evtform=null;
    if (null!=fontdef) fontdef=null;
    fontdef=null;
    if (null!=FontExtent) FontExtent=null;
    FontExtent=null;
    isigraph=null;
    opengl=null;
    lasterror="";
    result=null;
    verbose=0;
    cmdstrparms="";
  }

// ---------------------------------------------------------------------
  private void wdset()
  {
    String n=cmd.getid();
    String p=cmd.getid();
    String v=cmd.getparms();
    wdset1(n,p,v);
  }

// ---------------------------------------------------------------------
  private void wdsetj()
  {
    String p=cmd.getid();
    String v=cmd.getparms();
    JConsoleApp.theApp.setpref(p,v);
    return;
  }

// ---------------------------------------------------------------------
  private void wdsetp()
  {
    String p=cmd.getid();
    String v=cmd.getparms();
    if (noform()) return;
    Util.noevents(1);
    form.set(p,v);
    Util.noevents(0);
    return;
  }

// ---------------------------------------------------------------------
  private void wdsetx(String c)
  {
    String n=cmd.getid();
    String p=c.substring(3);
    String v=cmd.getparms();
    wdset1(n,p,v);
  }

// ---------------------------------------------------------------------
  private void wdset1(String n,String p,String v)
  {
    if (noform()) return;
    Util.noevents(1);
    if (n.equals("_")) n=formchildid();
    int type=setchild(n);
    switch (type) {
    case 1 :
      if (p.equals("stretch"))
        form.pane.setstretch(cc,v);
      else {
        if (v.isEmpty() && Util.isint(p)) {
          v=p;
          p="value";
        }
        cc.set(p,v);
      }
      break;
    case 2 :
      cc.set(n+" "+p,v);
      break;
    default :
      error("bad child id: " + n);
    }
    if (rc!=1) form.child=cc;
    Util.noevents(0);
  }

// ---------------------------------------------------------------------
  private void wdsm(String s)
  {
    String c,p;
    if (s.equals("sm"))
      c=cmd.getid();
    else if (s.equals("smact"))
      c="act";
    else
      c=s;
//  result=Util.s2ba(sm(c));
    if (rc==1)
      error(Util.ba2s(result));
  }

// ---------------------------------------------------------------------
  private void wdstate(Form f,int event)
  {
    if (null!=f)
      result=f.state(event);
    rc=-2;
  }

// ---------------------------------------------------------------------
  private void wdtextview()
  {
    String p, title="",header="",text;
    char d;
    int n;
    p=Util.boxj2utf8(cmd.getparms());
    if (p.isEmpty()) return;
    d=p.charAt(0);
    p=p.substring(1);
    n=p.indexOf(d);
    if (-1!=n)
      title=p.substring(0,n);
    p=p.substring(n+1);
    n=p.indexOf(d);
    if (-1!=n)
      header=p.substring(0,n);
    text=p.substring(n+1);
    Intent intent = new Intent();
    intent.setClass(context, JTextViewActivity.class);
    intent.putExtra("title", title);
    intent.putExtra("header", header);
    intent.putExtra("text", text);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
    context.startActivity(intent);
  }

// ---------------------------------------------------------------------
  private void wdtimer()
  {
    String p=Util.remquotes(cmd.getparms());
    systimerInterval=Util.c_strtol(p);
    if (0<systimerInterval) {
      if (null==systimerHandler) systimerHandler = new Handler();
      systimerHandler.postDelayed(systimerRunnable, systimerInterval);
    }
  }

// ---------------------------------------------------------------------
  private void wdverbose()
  {
    String p=Util.remquotes(cmd.getparms());
    String[] n=Util.qsless(p.split(" "),new String[] {""});   //,String::SkipEmptyParts);
    if (0==n.length)
      error("verbose requires 1 number: " + p);
    else {
      int i=Util.c_strtoi(n[0]);
      if (!(i>=0 && i<=3))
        error("verbose should be 0,1,2 or 3: " + p);
      else verbose=i;
    }
  }

// ---------------------------------------------------------------------
  private String wdosrelease()
  {
    /*
        Field[] fields = Build.VERSION_CODES.class.getFields();
        String codeName = "";
        for (Field field : fields) {
          try {
            if (field.getInt(Build.VERSION_CODES.class) == Build.VERSION.SDK_INT) {
              codeName = " "+field.getName();
            }
          } catch (IllegalAccessException e) {
            e.printStackTrace();
          }
        }
        return Build.VERSION.RELEASE+codeName;
    */
    return Build.VERSION.RELEASE;
  }
// ---------------------------------------------------------------------
  private void wdversion()
  {
    String p=Util.remquotes(cmd.getparms());
    if (!p.isEmpty()) {
      error("extra parameters: " + p);
      return;
    }
    result=Util.s2ba(JConsoleApp.theApp.mVersionName+"/"+wdosrelease()+"/"+Util.i2s(Build.VERSION.SDK_INT));
    rc=-1;
  }

// ---------------------------------------------------------------------
  private void wdwt()
  {
    String p=cmd.getparms();
    if (noform()) return;
    String[] n=Util.qsless(p.split(" "),new String[] {""});   //,String::SkipEmptyParts);
    if (n.length!=1)
      error("wt requires 1 number: " + p);
    else {
      form.pane.weight=(float)Util.c_strtod(n[0]);
    }
  }

// ---------------------------------------------------------------------
  private void wdwh()
  {
    String p=cmd.getparms();
    if (noform()) return;
    String[] n=Util.qsless(p.split(" "),new String[] {""});   //,String::SkipEmptyParts);
    if (n.length!=2)
      error("wh requires 2 numbers: " + p);
    else {
      form.pane.sizew=Util.c_strtoi(n[0]);
      form.pane.sizeh=Util.c_strtoi(n[1]);
    }
  }

// ---------------------------------------------------------------------
  private void wdwwh()
  {
    String p=cmd.getparms();
    if (noform()) return;
    String[] n=Util.qsless(p.split(" "),new String[] {""});   //,String::SkipEmptyParts);
    if (n.length!=3)
      error("wh requires 3 numbers: " + p);
    else {
      form.pane.weight=(float)Util.c_strtod(n[0]);
      form.pane.sizew=Util.c_strtoi(n[1]);
      form.pane.sizeh=Util.c_strtoi(n[2]);
    }
  }

// ---------------------------------------------------------------------
  private void wdminwh()
  {
    String p=cmd.getparms();
    if (noform()) return;
    String[] n=Util.qsless(p.split(" "),new String[] {""});  //,String::SkipEmptyParts);
    if (n.length!=2)
      error("minwh requires 2 numbers: " + p);
    else {
      form.pane.minsizew=Util.c_strtoi(n[0]);
      form.pane.minsizeh=Util.c_strtoi(n[1]);
    }
  }

// ---------------------------------------------------------------------
  public void error(String s)
  {
    lasterror=ccmd+" : "+s;
    Log.d(JConsoleApp.LogTag,"error: "+lasterror);
    rc=1;
  }

// ---------------------------------------------------------------------
  public void glerror(int t, String s)
  {
    lasterror=Integer.toString(t)+" : "+s;
    Log.d(JConsoleApp.LogTag,"error: "+lasterror);
    rc=1;
  }

// ---------------------------------------------------------------------
// returns: id of current form child
  private String formchildid()
  {
    if (noform()) return "";
    if (null==form.child) return "";
    return form.child.id;
  }

// ---------------------------------------------------------------------
  boolean invalidopt(String n,String[] opt,String valid)
  {
    String[] unopt=Util.qsless(Util.qsless(opt,Util.sajoin(defChildStyle,Cmd.qsplit(valid))),new String[] {""});
    if (0==unopt.length || Util.qsnumeric(unopt)) return false;
    error("unrecognized style for " + n + ": " + Util.sajoinstr(unopt," "));
    return true;
  }

// ---------------------------------------------------------------------
  private boolean nochild()
  {
    if (null!=cc) return false;
    error("no child selected");
    return true;
  }

// ---------------------------------------------------------------------
  private boolean noform()
  {
    if (null!=form) return false;
    error("no parent selected");
    return true;
  }

// ---------------------------------------------------------------------
// returns: 0=id not found
//          1=child id (cc=child)
//          2=menu  id (cc=menubar)
  private int setchild(String id)
  {
    Child c;
    if (noform()) return 0;
    c=form.id2child(id);
    if (null!=c) {
      cc=c;
      return 1;
    }
    c=form.setmenuid(id);
    if (null!=c) {
      cc=c;
      return 2;
    }
    return 0;
  }

// ---------------------------------------------------------------------
// translate event.keyboard key to Private Use Area
  int translateqkey(int key)
  {
    return (key>=0x1000000) ? ((key & 0xff) | 0xf800) : key;
  }

}
